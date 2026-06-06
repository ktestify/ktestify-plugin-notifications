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
package io.github.ktestify.notifications.model;

import io.cucumber.java.Scenario;
import java.net.URI;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Immutable event object representing the result of a single Cucumber scenario.
 *
 * <p>Created by {@link io.github.ktestify.notifications.hooks.NotificationHooks} in the {@code @After} hook and
 * accumulated in {@link io.github.ktestify.notifications.service.ScenarioAggregator} for the suite summary.
 *
 * @since 1.0.0
 */
@Value
@Builder
public class ScenarioEvent {

    /** Display name of the scenario (from {@link Scenario#getName()}). */
    String scenarioName;

    /** Feature file name (e.g. {@code "raw-orders.feature"}), extracted from the scenario URI. */
    String featureName;

    /** All Cucumber tags on this scenario (e.g. {@code ["@smoke", "@orders"]}). */
    List<String> tags;

    /** Outcome of the scenario. */
    NotificationStatus status;

    /** Wall-clock duration in milliseconds from {@code @Before} to {@code @After}. */
    long durationMs;

    /** The instant this event was recorded (end of scenario). */
    Instant timestamp;

    // ── Factory ──────────────────────────────────────────────────────────────

    /**
     * Constructs a {@code ScenarioEvent} from the Cucumber {@link Scenario} object and the measured duration.
     *
     * @param scenario   the scenario provided by the {@code @After} hook
     * @param durationMs elapsed milliseconds from scenario start to end
     * @return a populated {@code ScenarioEvent}
     */
    public static ScenarioEvent from(Scenario scenario, long durationMs) {
        NotificationStatus status = resolveStatus(scenario);
        String featureName = extractFeatureName(scenario.getUri());

        return ScenarioEvent.builder()
                .scenarioName(scenario.getName())
                .featureName(featureName)
                .tags(List.copyOf(scenario.getSourceTagNames()))
                .status(status)
                .durationMs(durationMs)
                .timestamp(Instant.now())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static NotificationStatus resolveStatus(Scenario scenario) {
        // Use name() comparison to avoid importing internal Cucumber Status enum
        String statusName = scenario.getStatus().name();
        return switch (statusName) {
            case "FAILED", "AMBIGUOUS", "UNDEFINED" -> NotificationStatus.FAILED;
            case "SKIPPED", "PENDING" -> NotificationStatus.SKIPPED;
            default -> NotificationStatus.PASSED;
        };
    }

    private static String extractFeatureName(URI uri) {
        if (uri == null) return "unknown";
        try {
            return Paths.get(uri.getPath()).getFileName().toString();
        } catch (Exception e) {
            return uri.toString();
        }
    }
}

