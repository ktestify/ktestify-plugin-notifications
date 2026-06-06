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
package io.github.ktestify.notifications.template;

import io.github.ktestify.notifications.config.ChannelConfig;
import io.github.ktestify.notifications.config.NotificationsConfig;
import io.github.ktestify.notifications.model.GroupResult;
import io.github.ktestify.notifications.model.SuiteEvent;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Renders a {@link SuiteEvent} into a channel-specific HTTP payload string.
 *
 * <h2>Variable substitution</h2>
 *
 * <p>Templates use {@code {{variableName}}} syntax. All variables are substituted with their string values; unknown
 * variables are replaced with an empty string. Missing CI/Git context fields resolve to {@code "N/A"}.
 *
 * <h2>JSON array injection ({@code {{groupSections}}} and {@code {{footer}}})</h2>
 *
 * <p>{@code {{groupSections}}} is replaced with a comma-joined list of rendered group JSON fragments, each separated by
 * a comma. If non-empty, a trailing comma is appended so the subsequent {@code {{footer}}} element is valid JSON. If
 * there are no groups, {@code {{groupSections}}} is an empty string and {@code {{footer}}} becomes the sole extra
 * element.
 *
 * @since 1.0.0
 */
public final class NotificationTemplateEngine {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private NotificationTemplateEngine() {}

    /**
     * Renders the full HTTP body for a suite event using the given channel configuration.
     *
     * @param event the suite event to render
     * @param config the channel configuration (determines template sources and type)
     * @param globalConfig the root notifications config (for group definitions and thresholds)
     * @return the rendered HTTP payload string
     */
    public static String renderSuite(SuiteEvent event, ChannelConfig config, NotificationsConfig globalConfig) {
        // Render group sections
        String groupSections = renderGroupSections(event.getGroupedResults(), config);

        // Render footer
        String footer = renderFooter(event, config);

        // Build variable map
        Map<String, String> vars = buildSuiteVars(event);
        vars.put("groupSections", groupSections);
        vars.put("footer", footer);

        // Resolve suite template and substitute
        String template = NotificationTemplateResolver.resolve(config.getTemplateSuite(), config.getType(), "suite");
        return substitute(template, vars);
    }

    // ── Private rendering helpers ─────────────────────────────────────────────

    private static String renderGroupSections(List<GroupResult> groups, ChannelConfig config) {
        if (groups == null || groups.isEmpty()) return "";

        String template = NotificationTemplateResolver.resolve(config.getTemplateGroup(), config.getType(), "group");
        if (template.isBlank()) return "";

        String joined = groups.stream()
                .map(g -> substitute(template, buildGroupVars(g)))
                .collect(Collectors.joining(",\n"));

        // Trailing comma so the next element (footer) is a valid JSON array sibling
        return joined + ",\n";
    }

    private static String renderFooter(SuiteEvent event, ChannelConfig config) {
        String template = NotificationTemplateResolver.resolve(config.getTemplateFooter(), config.getType(), "footer");
        if (template.isBlank()) return "";
        return substitute(template, buildSuiteVars(event));
    }

    // ── Variable map builders ─────────────────────────────────────────────────

    private static Map<String, String> buildSuiteVars(SuiteEvent event) {
        Map<String, String> vars = new LinkedHashMap<>();

        // Suite identity
        vars.put("suiteName", orEmpty(event.getSuiteName()));
        vars.put("environment", orEmpty(event.getEnvironment()));
        vars.put("reportUrl", orEmpty(event.getReportUrl()));
        vars.put("date", DATE_FMT.format(event.getTimestamp()));
        vars.put("status", event.getStatus().name());
        vars.put("overallStyle", orEmpty(event.getStyle()));

        // Counts
        vars.put("totalCount", String.valueOf(event.getTotalCount()));
        vars.put("passedCount", String.valueOf(event.getPassedCount()));
        vars.put("failedCount", String.valueOf(event.getFailedCount()));
        vars.put("skippedCount", String.valueOf(event.getSkippedCount()));
        vars.put("successRate", String.valueOf(event.getSuccessRate()));

        // CI context
        if (event.getCiContext() != null) {
            vars.put("ciName", orNA(event.getCiContext().getCiName()));
            vars.put("pipelineUrl", orEmpty(event.getCiContext().getPipelineUrl()));
            vars.put("buildNumber", orNA(event.getCiContext().getBuildNumber()));
        } else {
            vars.put("ciName", "N/A");
            vars.put("pipelineUrl", "");
            vars.put("buildNumber", "N/A");
        }

        // Git context
        if (event.getGitContext() != null) {
            vars.put("gitBranch", orNA(event.getGitContext().getBranch()));
            vars.put("gitRevision", orNA(event.getGitContext().getRevision()));
            vars.put("gitTag", orNA(event.getGitContext().getTag()));
        } else {
            vars.put("gitBranch", "N/A");
            vars.put("gitRevision", "N/A");
            vars.put("gitTag", "N/A");
        }

        return vars;
    }

    private static Map<String, String> buildGroupVars(GroupResult g) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("groupLabel", orEmpty(g.getGroupLabel()));
        vars.put("emoji", orEmpty(g.getEmoji()));
        vars.put("tag", orEmpty(g.getTag()));
        vars.put("successRate", String.valueOf(g.getSuccessRate()));
        vars.put("style", orEmpty(g.getStyle()));
        vars.put("passedCount", String.valueOf(g.getPassedCount()));
        vars.put("failedCount", String.valueOf(g.getFailedCount()));
        vars.put("skippedCount", String.valueOf(g.getSkippedCount()));
        vars.put("totalCount", String.valueOf(g.getTotalCount()));
        return vars;
    }

    // ── Core substitutor ──────────────────────────────────────────────────────

    /**
     * Replaces all {@code {{key}}} tokens in {@code template} with the corresponding value from {@code vars}. Unknown
     * keys are replaced with an empty string.
     *
     * @param template the template string
     * @param vars variable name → value
     * @return the substituted string
     */
    static String substitute(String template, Map<String, String> vars) {
        String result = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
        }
        // Remove any remaining unresolved placeholders
        result = result.replaceAll("\\{\\{[^}]+}}", "");
        return result;
    }

    // ── Null-safe helpers ─────────────────────────────────────────────────────

    private static String orEmpty(String value) {
        return value != null ? value : "";
    }

    private static String orNA(String value) {
        return (value != null && !value.isBlank()) ? value : "N/A";
    }
}
