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

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Immutable event object representing the aggregated result of a complete test suite run.
 *
 * <p>Built by {@link io.github.ktestify.notifications.service.ScenarioAggregator#buildSuiteEvent} from all accumulated
 * {@link ScenarioEvent} instances after the last scenario completes.
 *
 * @since 1.0.0
 */
@Value
@Builder
public class SuiteEvent {

    // ── Suite identity ────────────────────────────────────────────────────────

    /** Suite display name (from {@code ktestify.plugins.notifications.suite.name}). */
    String suiteName;

    /** Environment label (e.g. {@code "staging"}). Empty string if not configured. */
    String environment;

    /** URL to the published test report. Empty string if not configured. */
    String reportUrl;

    // ── Outcome ───────────────────────────────────────────────────────────────

    /**
     * Overall suite status. {@link NotificationStatus#FAILED} if any scenario failed;
     * {@link NotificationStatus#PASSED} otherwise.
     */
    NotificationStatus status;

    /** Total number of scenarios that ran. */
    int totalCount;

    /** Number of scenarios that passed. */
    int passedCount;

    /** Number of scenarios that failed. */
    int failedCount;

    /** Number of scenarios that were skipped or pending. */
    int skippedCount;

    /** Total wall-clock duration across all scenarios (sum of per-scenario durations), in milliseconds. */
    long durationMs;

    /**
     * Overall success rate as an integer percentage (0–100). Computed as
     * {@code (passedCount / totalCount) * 100}. 100 when no scenarios ran.
     */
    int successRate;

    /**
     * Visual style derived from {@link #successRate} and configured thresholds. One of {@code "good"},
     * {@code "warning"}, {@code "attention"}.
     */
    String style;

    // ── Per-group breakdown ───────────────────────────────────────────────────

    /**
     * Per-tag-group results. Populated from {@code ktestify.plugins.notifications.groups} config. Always includes an
     * {@code "Untagged"} group if any scenarios matched no configured tag.
     */
    List<GroupResult> groupedResults;

    // ── CI / Git context ──────────────────────────────────────────────────────

    /** CI platform context (name, pipeline URL, build number). {@code null} if not running in a recognised CI. */
    CiContext ciContext;

    /** Git repository context (branch, commit, tag). {@code null} if not available. */
    GitContext gitContext;

    /** The instant the suite event was built (approximately when the last scenario finished). */
    Instant timestamp;
}

