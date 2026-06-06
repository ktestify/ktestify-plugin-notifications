/*
 * Copyright 2026 Nil MALHOMME (malhomme.nil+oss@icloud.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.ktestify.notifications.service;

import io.github.ktestify.notifications.channel.NotificationChannel;
import io.github.ktestify.notifications.channel.NotificationChannelFactory;
import io.github.ktestify.notifications.config.NotificationsConfig;
import io.github.ktestify.notifications.model.ScenarioEvent;
import io.github.ktestify.notifications.model.SuiteEvent;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orchestrates asynchronous fan-out of notification events to all configured channels.
 *
 * <h2>Async dispatch</h2>
 *
 * <p>Each {@link #dispatch(SuiteEvent)} and {@link #dispatch(ScenarioEvent)} call submits a task per channel to a
 * daemon thread pool. Callers (Cucumber hooks) return immediately; HTTP requests run in the background.
 *
 * <h2>Failure safety</h2>
 *
 * <p>Failed dispatches (network errors, channel exceptions) are logged at {@code WARN} and silently swallowed. A
 * failing notification channel <em>never</em> fails a test.
 *
 * <h2>Shutdown</h2>
 *
 * <p>Call {@link #shutdown(int)} after all notifications have been dispatched (in {@code @AfterAll}) to allow in-flight
 * tasks to complete before the JVM exits.
 *
 * @since 1.0.0
 */
public final class NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

    /** No-op singleton returned when notifications are disabled. */
    public static final NotificationService NOOP = new NotificationService();

    private final List<NotificationChannel> channels;
    private final ExecutorService executor;
    private final boolean enabled;
    private final boolean globalOnFailureOnly;

    /** Constructor for the NOOP singleton. */
    private NotificationService() {
        this.channels = List.of();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ktestify-notifications");
            t.setDaemon(true);
            return t;
        });
        this.enabled = false;
        this.globalOnFailureOnly = false;
    }

    /**
     * Creates an active notification service from the given configuration.
     *
     * @param config the root notifications configuration
     */
    public NotificationService(NotificationsConfig config) {
        this.enabled = config.isEnabled();
        this.globalOnFailureOnly = config.isOnFailureOnly();
        this.channels = NotificationChannelFactory.build(config);
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "ktestify-notifications");
            t.setDaemon(true);
            return t;
        });
        LOG.debug("NotificationService ready with {} active channel(s).", channels.size());
    }

    // ── Dispatch ──────────────────────────────────────────────────────────────

    /**
     * Dispatches a suite event asynchronously to all channels that support it.
     *
     * @param event the aggregated suite result
     */
    public void dispatch(SuiteEvent event) {
        if (!enabled || channels.isEmpty()) return;

        for (NotificationChannel channel : channels) {
            if (channel.supportsSuite(event, globalOnFailureOnly)) {
                CompletableFuture.runAsync(() -> channel.sendSuite(event), executor)
                        .exceptionally(t -> {
                            LOG.warn("Channel {} failed to send suite event: {}", channel.getType(), t.getMessage());
                            return null;
                        });
            }
        }
    }

    /**
     * Dispatches a scenario event asynchronously to all channels that support it.
     *
     * <p>Most channels return {@code false} from {@link NotificationChannel#supportsScenario} by default, making this a
     * no-op in standard configurations.
     *
     * @param event the individual scenario result
     */
    public void dispatch(ScenarioEvent event) {
        if (!enabled || channels.isEmpty()) return;

        for (NotificationChannel channel : channels) {
            if (channel.supportsScenario(event)) {
                CompletableFuture.runAsync(() -> channel.sendScenario(event), executor)
                        .exceptionally(t -> {
                            LOG.warn("Channel {} failed to send scenario event: {}", channel.getType(), t.getMessage());
                            return null;
                        });
            }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Gracefully shuts down the internal thread pool, waiting up to {@code timeoutSeconds} for in-flight tasks to
     * complete.
     *
     * <p>Call this in {@code @AfterAll} after all suite notifications have been dispatched.
     *
     * @param timeoutSeconds maximum seconds to wait for pending notifications to complete
     */
    public void shutdown(int timeoutSeconds) {
        if (executor.isShutdown()) return;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                LOG.warn("Some notifications did not complete within {}s.", timeoutSeconds);
                executor.shutdownNow();
            } else {
                LOG.debug("All notification tasks completed.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
