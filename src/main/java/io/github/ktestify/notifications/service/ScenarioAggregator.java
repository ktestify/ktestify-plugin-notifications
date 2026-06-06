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

import io.github.ktestify.notifications.config.NotificationsConfig;
import io.github.ktestify.notifications.config.TagGroupConfig;
import io.github.ktestify.notifications.config.ThresholdsConfig;
import io.github.ktestify.notifications.model.CiContext;
import io.github.ktestify.notifications.model.GitContext;
import io.github.ktestify.notifications.model.GroupResult;
import io.github.ktestify.notifications.model.NotificationStatus;
import io.github.ktestify.notifications.model.ScenarioEvent;
import io.github.ktestify.notifications.model.SuiteEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thread-safe static accumulator for {@link ScenarioEvent} instances throughout a test suite run.
 *
 * <p>Called from {@link io.github.ktestify.notifications.hooks.NotificationHooks} {@code @After} hook (concurrent,
 * once per scenario) and read from the {@code @AfterAll} hook (single-threaded, after all scenarios complete) to
 * build the final {@link SuiteEvent}.
 *
 * <h2>Tag-based grouping logic</h2>
 *
 * <p>Each scenario is matched to the <em>first</em> configured tag group whose {@code tag} appears in the scenario's
 * tag collection (with or without the leading {@code @}). Unmatched scenarios are placed in an automatic
 * {@code "Untagged"} group.
 *
 * @since 1.0.0
 */
public final class ScenarioAggregator {

    private static final List<ScenarioEvent> EVENTS = Collections.synchronizedList(new ArrayList<>());

    private ScenarioAggregator() {}

    /**
     * Records a scenario event. Thread-safe; may be called concurrently from multiple {@code @After} hooks.
     *
     * @param event the scenario result to record
     */
    public static void record(ScenarioEvent event) {
        EVENTS.add(event);
    }

    /**
     * Returns an unmodifiable snapshot of all recorded scenario events.
     *
     * @return unmodifiable list of events recorded so far
     */
    public static List<ScenarioEvent> getEvents() {
        return Collections.unmodifiableList(new ArrayList<>(EVENTS));
    }

    /**
     * Builds the {@link SuiteEvent} from all accumulated scenario events.
     *
     * @param config the notifications configuration (suite metadata, thresholds, group definitions)
     * @param ci     CI platform context (may be {@code null})
     * @param git    Git repository context (may be {@code null})
     * @return the aggregated suite event
     */
    public static SuiteEvent buildSuiteEvent(NotificationsConfig config, CiContext ci, GitContext git) {
        List<ScenarioEvent> all = getEvents();

        int total = all.size();
        int passed = count(all, NotificationStatus.PASSED);
        int failed = count(all, NotificationStatus.FAILED);
        int skipped = count(all, NotificationStatus.SKIPPED);
        long totalDurationMs = all.stream().mapToLong(ScenarioEvent::getDurationMs).sum();
        int successRate = total > 0 ? (int) (passed * 100.0 / total) : 100;

        ThresholdsConfig thresholds = config.getThresholds();
        String overallStyle = thresholds.computeStyle(successRate);
        NotificationStatus overallStatus = failed > 0 ? NotificationStatus.FAILED : NotificationStatus.PASSED;

        List<GroupResult> groupedResults = buildGroupResults(all, config);

        return SuiteEvent.builder()
                .suiteName(config.getSuite().getName())
                .environment(config.getSuite().getEnvironment())
                .reportUrl(config.getSuite().getReportUrl())
                .status(overallStatus)
                .totalCount(total)
                .passedCount(passed)
                .failedCount(failed)
                .skippedCount(skipped)
                .durationMs(totalDurationMs)
                .successRate(successRate)
                .style(overallStyle)
                .groupedResults(groupedResults)
                .ciContext(ci)
                .gitContext(git)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Clears all accumulated events. Useful for unit testing.
     */
    public static void clear() {
        EVENTS.clear();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static int count(List<ScenarioEvent> events, NotificationStatus status) {
        return (int) events.stream().filter(e -> e.getStatus() == status).count();
    }

    private static List<GroupResult> buildGroupResults(List<ScenarioEvent> events, NotificationsConfig config) {
        List<TagGroupConfig> groupDefs = config.getGroups();
        ThresholdsConfig thresholds = config.getThresholds();

        // Group scenarios by matched tag (insertion-ordered for deterministic output)
        Map<String, List<ScenarioEvent>> grouped = new LinkedHashMap<>();

        for (ScenarioEvent event : events) {
            String matchedTag = findMatchingTag(event, groupDefs);
            grouped.computeIfAbsent(matchedTag, k -> new ArrayList<>()).add(event);
        }

        List<GroupResult> results = new ArrayList<>();
        for (Map.Entry<String, List<ScenarioEvent>> entry : grouped.entrySet()) {
            String tag = entry.getKey();
            List<ScenarioEvent> groupEvents = entry.getValue();

            TagGroupConfig groupDef = findGroupDef(tag, groupDefs);
            String label = (groupDef != null) ? groupDef.getLabel() : capitalise(tag);
            String emoji = (groupDef != null) ? groupDef.getEmoji() : "🏷️";

            int total = groupEvents.size();
            int passed = count(groupEvents, NotificationStatus.PASSED);
            int failed = count(groupEvents, NotificationStatus.FAILED);
            int skipped = count(groupEvents, NotificationStatus.SKIPPED);
            int rate = total > 0 ? (int) (passed * 100.0 / total) : 100;

            results.add(GroupResult.builder()
                    .groupLabel(label)
                    .emoji(emoji)
                    .tag(tag)
                    .totalCount(total)
                    .passedCount(passed)
                    .failedCount(failed)
                    .skippedCount(skipped)
                    .successRate(rate)
                    .style(thresholds.computeStyle(rate))
                    .build());
        }
        return Collections.unmodifiableList(results);
    }

    /**
     * Returns the first configured group tag that matches any of the scenario's tags, or {@code "untagged"} if none
     * match.
     */
    private static String findMatchingTag(ScenarioEvent event, List<TagGroupConfig> groupDefs) {
        for (TagGroupConfig def : groupDefs) {
            String tagWithAt = "@" + def.getTag();
            String tagWithout = def.getTag();
            for (String scenarioTag : event.getTags()) {
                if (scenarioTag.equalsIgnoreCase(tagWithAt) || scenarioTag.equalsIgnoreCase(tagWithout)) {
                    return def.getTag();
                }
            }
        }
        return "untagged";
    }

    private static TagGroupConfig findGroupDef(String tag, List<TagGroupConfig> defs) {
        return defs.stream()
                .filter(d -> d.getTag().equalsIgnoreCase(tag))
                .findFirst()
                .orElse(null);
    }

    private static String capitalise(String s) {
        if (s == null || s.isBlank()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

