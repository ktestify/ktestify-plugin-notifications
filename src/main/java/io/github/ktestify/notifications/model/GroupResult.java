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

import lombok.Builder;
import lombok.Value;

/**
 * Aggregated result for one Cucumber tag group within a suite run.
 *
 * <p>Created by {@link io.github.ktestify.notifications.service.ScenarioAggregator} for each group configured in
 * {@code ktestify.plugins.notifications.groups}, plus an automatic {@code "Untagged"} group for scenarios that match no
 * configured tag.
 *
 * <p>The {@link #style} field maps the success rate to a visual style for card templates:
 *
 * <ul>
 *   <li>{@code "good"}, success rate ≥ {@code thresholds.good} (default 75 %)
 *   <li>{@code "warning"}, success rate ≥ {@code thresholds.warning} (default 50 %)
 *   <li>{@code "attention"}, success rate < {@code thresholds.warning}
 * </ul>
 *
 * @since 1.0.0
 */
@Value
@Builder
public class GroupResult {

    /** Display label for this group (from {@code TagGroupConfig.label}). */
    String groupLabel;

    /** Emoji associated with this group (from {@code TagGroupConfig.emoji}). Defaults to {@code "🏷️"}. */
    String emoji;

    /** Cucumber tag that was matched (e.g. {@code "orders"}). {@code "untagged"} for the fallback group. */
    String tag;

    /** Total number of scenarios in this group. */
    int totalCount;

    /** Number of passed scenarios. */
    int passedCount;

    /** Number of failed scenarios. */
    int failedCount;

    /** Number of skipped scenarios. */
    int skippedCount;

    /**
     * Success rate as an integer percentage (0–100). Computed as {@code (passedCount / totalCount) * 100}, rounded
     * down.
     */
    int successRate;

    /**
     * Visual style derived from {@link #successRate} and the configured thresholds. One of {@code "good"},
     * {@code "warning"}, {@code "attention"}.
     */
    String style;
}
