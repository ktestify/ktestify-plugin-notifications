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
package io.github.ktestify.notifications.channel;

import io.github.ktestify.notifications.model.NotificationStatus;
import io.github.ktestify.notifications.model.ScenarioEvent;
import io.github.ktestify.notifications.model.SuiteEvent;

/**
 * SPI contract for a notification output channel.
 *
 * <p>Implementations send a formatted message to a specific destination (Slack, Teams, a generic webhook, etc.) when
 * called by {@link io.github.ktestify.notifications.service.NotificationService}.
 *
 * <h2>Contract rules</h2>
 *
 * <ul>
 *   <li>{@link #sendSuite(SuiteEvent)} must <em>never throw</em>. Catch all exceptions internally, log at {@code WARN},
 *       and return silently. A failed notification must never fail a test.
 *   <li>{@link #sendScenario(ScenarioEvent)} is optional (default no-op). Override for real-time per-scenario alerts.
 *   <li>{@link #supportsSuite(SuiteEvent)} and {@link #supportsScenario(ScenarioEvent)} are called by
 *       {@link io.github.ktestify.notifications.service.NotificationService} before dispatching. Return {@code false}
 *       to skip (e.g. when {@code on-failure-only = true} and the suite passed).
 * </ul>
 *
 * <h2>Custom channels via SPI</h2>
 *
 * <p>Third-party channels can be registered by implementing this interface and declaring the implementation class in
 * {@code META-INF/services/io.github.ktestify.notifications.channel.NotificationChannel}. The
 * {@link NotificationChannelFactory} will pick them up automatically via {@link java.util.ServiceLoader}.
 *
 * @since 1.0.0
 * @see NotificationChannelFactory
 */
public interface NotificationChannel {

    /**
     * Returns the unique type identifier for this channel (e.g. {@code "slack"}, {@code "teams"}, {@code "webhook"}).
     *
     * <p>Must match the {@code type} key in the corresponding {@code ChannelConfig} HOCON block.
     *
     * @return channel type,  never {@code null} or blank
     */
    String getType();

    /**
     * Sends a suite-level summary notification.
     *
     * <p>Implementations <em>must not throw</em>. All exceptions must be caught internally.
     *
     * @param event the aggregated suite result
     */
    void sendSuite(SuiteEvent event);

    /**
     * Sends a per-scenario notification. Default implementation is a no-op.
     *
     * <p>Override to enable real-time alerting on individual scenario failures.
     *
     * @param event the scenario result
     */
    default void sendScenario(ScenarioEvent event) {
        // no-op by default
    }

    /**
     * Returns {@code true} if this channel should handle the given suite event.
     *
     * <p>The default implementation respects the channel's {@code on-failure-only} flag.
     *
     * @param event      the suite event
     * @param onFailureOnly the channel's configured on-failure-only flag
     * @return {@code true} to dispatch, {@code false} to skip
     */
    default boolean supportsSuite(SuiteEvent event, boolean onFailureOnly) {
        if (onFailureOnly) {
            return event.getStatus() == NotificationStatus.FAILED;
        }
        return true;
    }

    /**
     * Returns {@code true} if this channel should handle the given scenario event.
     *
     * <p>Default: {@code false},  suite-only channels skip scenario events.
     *
     * @param event the scenario event
     * @return {@code true} to dispatch, {@code false} to skip
     */
    default boolean supportsScenario(ScenarioEvent event) {
        return false;
    }
}

