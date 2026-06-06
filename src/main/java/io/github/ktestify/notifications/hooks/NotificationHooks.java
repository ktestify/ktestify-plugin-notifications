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
package io.github.ktestify.notifications.hooks;

import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.github.ktestify.config.KtestifyConfig;
import io.github.ktestify.notifications.config.NotificationsConfig;
import io.github.ktestify.notifications.model.CiContext;
import io.github.ktestify.notifications.model.GitContext;
import io.github.ktestify.notifications.model.ScenarioEvent;
import io.github.ktestify.notifications.model.SuiteEvent;
import io.github.ktestify.notifications.service.CiContextResolver;
import io.github.ktestify.notifications.service.NotificationService;
import io.github.ktestify.notifications.service.ScenarioAggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cucumber lifecycle hooks that integrate the notification system into the test run.
 *
 * <h2>Hook overview</h2>
 *
 * <table>
 *   <tr><th>Hook</th><th>Order</th><th>Action</th></tr>
 *   <tr><td>{@code @Before}</td><td>{@link Integer#MIN_VALUE}</td><td>Records scenario start time.</td></tr>
 *   <tr><td>{@code @After}</td><td>{@link Integer#MAX_VALUE}</td><td>Builds a {@link ScenarioEvent}, stores it in
 *       {@link ScenarioAggregator}, and optionally dispatches a per-scenario notification.</td></tr>
 *   <tr><td>{@code @AfterAll}</td><td>—</td><td>Builds the {@link SuiteEvent}, dispatches the suite-level
 *       notification to all channels, and shuts down the thread pool gracefully.</td></tr>
 * </table>
 *
 * <h2>Thread safety</h2>
 *
 * <p>{@link #NOTIFICATION_SERVICE} is initialised once via double-checked locking. {@link ScenarioAggregator} uses a
 * {@code Collections.synchronizedList} so concurrent {@code @After} calls are safe.
 *
 * <h2>Failure safety</h2>
 *
 * <p>All notification dispatch calls are wrapped in try-catch. A notification failure is logged at {@code WARN} and
 * never propagates to the test result.
 *
 * @since 1.0.0
 */
public class NotificationHooks {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationHooks.class);

    // ── Static singleton,  initialised once per JVM run ───────────────────────
    private static final Object INIT_LOCK = new Object();
    private static volatile NotificationService NOTIFICATION_SERVICE;
    private static volatile NotificationsConfig CACHED_CONFIG;

    // ── Per-scenario state (instance field,  one instance per scenario) ────────
    private long scenarioStartMs;

    // ── Lifecycle hooks ───────────────────────────────────────────────────────

    /**
     * Records the scenario start time. Runs before all other {@code @Before} hooks.
     *
     * @param scenario the current scenario
     */
    @Before(order = Integer.MIN_VALUE)
    public void beforeScenario(Scenario scenario) {
        this.scenarioStartMs = System.currentTimeMillis();
    }

    /**
     * Builds and records a {@link ScenarioEvent}, then optionally dispatches a per-scenario notification. Runs after
     * all other {@code @After} hooks.
     *
     * @param scenario the completed scenario
     */
    @After(order = Integer.MAX_VALUE)
    public void afterScenario(Scenario scenario) {
        try {
            long durationMs = System.currentTimeMillis() - scenarioStartMs;
            ScenarioEvent event = ScenarioEvent.from(scenario, durationMs);

            ScenarioAggregator.record(event);

            NotificationService service = getService();
            service.dispatch(event); // no-op for most channels unless supportsScenario() is overridden
        } catch (Exception e) {
            LOG.warn("Error recording scenario event for '{}': {}", scenario.getName(), e.getMessage());
        }
    }

    /**
     * Builds the suite summary, dispatches notifications to all configured channels, and shuts down the notification
     * thread pool. Runs once after all scenarios have completed.
     */
    @AfterAll
    public static void afterAllScenarios() {
        try {
            NotificationsConfig cfg = getConfig();
            if (!cfg.isEnabled()) {
                LOG.debug("Plugin disabled,  skipping suite notification.");
                return;
            }

            CiContext ci = CiContextResolver.resolve().orElse(null);
            GitContext git = CiContextResolver.resolveGit().orElse(null);

            SuiteEvent suiteEvent = ScenarioAggregator.buildSuiteEvent(cfg, ci, git);
            LOG.debug("Suite event built: {} scenarios, successRate={}%",
                    suiteEvent.getTotalCount(), suiteEvent.getSuccessRate());

            getService().dispatch(suiteEvent);
        } catch (Exception e) {
            LOG.warn("Error building or dispatching suite event: {}", e.getMessage());
        } finally {
            // Always shut down the thread pool,  allows in-flight notifications to complete
            try {
                getService().shutdown(30);
            } catch (Exception e) {
                LOG.warn("Error shutting down notification service: {}", e.getMessage());
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static NotificationService getService() {
        if (NOTIFICATION_SERVICE == null) {
            synchronized (INIT_LOCK) {
                if (NOTIFICATION_SERVICE == null) {
                    NotificationsConfig cfg = getConfig();
                    NOTIFICATION_SERVICE = cfg.isEnabled()
                            ? new NotificationService(cfg)
                            : NotificationService.NOOP;
                    LOG.debug("NotificationService initialised (enabled={}).", cfg.isEnabled());
                }
            }
        }
        return NOTIFICATION_SERVICE;
    }

    private static NotificationsConfig getConfig() {
        if (CACHED_CONFIG == null) {
            synchronized (INIT_LOCK) {
                if (CACHED_CONFIG == null) {
                    CACHED_CONFIG = NotificationsConfig.from(KtestifyConfig.getOrLoad().getRaw());
                }
            }
        }
        return CACHED_CONFIG;
    }

    /** Resets static state. For testing only. */
    static void reset() {
        synchronized (INIT_LOCK) {
            NOTIFICATION_SERVICE = null;
            CACHED_CONFIG = null;
        }
        ScenarioAggregator.clear();
        CiContextResolver.reset();
    }
}

