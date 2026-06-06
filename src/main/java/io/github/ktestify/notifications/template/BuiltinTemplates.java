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

/**
 * Bundled default JSON templates for each channel type and template slot.
 *
 * <p>Returned by {@link NotificationTemplateResolver} when no user-defined template is configured.
 *
 * <h2>Template variable reference</h2>
 *
 * <p>Suite-level variables: {@code {{suiteName}}}, {@code {{environment}}}, {@code {{date}}},
 * {@code {{totalCount}}}, {@code {{passedCount}}}, {@code {{failedCount}}}, {@code {{skippedCount}}},
 * {@code {{successRate}}}, {@code {{overallStyle}}}, {@code {{reportUrl}}}, {@code {{pipelineUrl}}},
 * {@code {{gitBranch}}}, {@code {{gitRevision}}}, {@code {{gitTag}}}, {@code {{buildNumber}}},
 * {@code {{ciName}}}, {@code {{groupSections}}}, {@code {{footer}}}.
 *
 * <p>Group-level variables: {@code {{emoji}}}, {@code {{groupLabel}}}, {@code {{tag}}},
 * {@code {{successRate}}}, {@code {{style}}}, {@code {{passedCount}}}, {@code {{failedCount}}},
 * {@code {{totalCount}}}.
 *
 * <p>Note on JSON array injection: {@code {{groupSections}}} is replaced with a comma-joined list of
 * rendered group fragments (each ending with a trailing comma if non-empty, enabling valid JSON array
 * construction). {@code {{footer}}} is always the last array element (no trailing comma).
 *
 * @since 1.0.0
 */
public final class BuiltinTemplates {

    private BuiltinTemplates() {}

    // =========================================================================
    // Teams (Adaptive Card)
    // =========================================================================

    static final String TEAMS_SUITE = """
            {
              "type": "AdaptiveCard",
              "$schema": "http://adaptivecards.io/schemas/adaptive-card.json",
              "version": "1.4",
              "body": [
                {
                  "type": "Container",
                  "style": "{{overallStyle}}",
                  "items": [{
                    "type": "TextBlock",
                    "text": "🧪 **{{suiteName}}**  |  {{environment}}  |  {{date}}",
                    "weight": "Bolder",
                    "size": "Medium",
                    "wrap": true
                  }]
                },
                {
                  "type": "FactSet",
                  "facts": [
                    {"title": "CI",      "value": "{{ciName}}"},
                    {"title": "Branch",  "value": "{{gitBranch}}"},
                    {"title": "Commit",  "value": "{{gitRevision}}"},
                    {"title": "Build",   "value": "#{{buildNumber}}"},
                    {"title": "Total",   "value": "{{totalCount}}"},
                    {"title": "✅ Passed",  "value": "{{passedCount}}"},
                    {"title": "❌ Failed",  "value": "{{failedCount}}"},
                    {"title": "⏭️ Skipped", "value": "{{skippedCount}}"},
                    {"title": "Success", "value": "{{successRate}}%"}
                  ]
                },
                {{groupSections}}{{footer}}
              ],
              "msteams": {"width": "Full"}
            }
            """;

    static final String TEAMS_GROUP = """
            {
              "type": "Container",
              "style": "{{style}}",
              "items": [{
                "type": "TextBlock",
                "text": "{{emoji}} **{{groupLabel}}**,  {{successRate}}%  ({{passedCount}}/{{totalCount}})",
                "wrap": true
              }]
            }
            """;

    static final String TEAMS_FOOTER = """
            {
              "type": "ActionSet",
              "actions": [
                {"type": "Action.OpenUrl", "title": "📊 Test Report", "url": "{{reportUrl}}"},
                {"type": "Action.OpenUrl", "title": "🔗 Pipeline",    "url": "{{pipelineUrl}}"}
              ]
            }
            """;

    // =========================================================================
    // Slack (Block Kit)
    // =========================================================================

    static final String SLACK_SUITE = """
            {
              "blocks": [
                {
                  "type": "header",
                  "text": {"type": "plain_text", "text": "🧪 {{suiteName}},  {{environment}}"}
                },
                {
                  "type": "section",
                  "fields": [
                    {"type": "mrkdwn", "text": "*Date:*    {{date}}"},
                    {"type": "mrkdwn", "text": "*CI:*      {{ciName}}"},
                    {"type": "mrkdwn", "text": "*Branch:*  {{gitBranch}}"},
                    {"type": "mrkdwn", "text": "*Build:*   #{{buildNumber}}"},
                    {"type": "mrkdwn", "text": "*Passed:*  {{passedCount}}/{{totalCount}}"},
                    {"type": "mrkdwn", "text": "*Success:* {{successRate}}%"}
                  ]
                },
                {"type": "divider"},
                {{groupSections}}{{footer}}
              ]
            }
            """;

    static final String SLACK_GROUP = """
            {
              "type": "section",
              "text": {
                "type": "mrkdwn",
                "text": "{{emoji}} *{{groupLabel}}*,  {{successRate}}%  ({{passedCount}}/{{totalCount}})"
              }
            }
            """;

    static final String SLACK_FOOTER = """
            {
              "type": "actions",
              "elements": [
                {"type": "button", "text": {"type": "plain_text", "text": "📊 Test Report"}, "url": "{{reportUrl}}"},
                {"type": "button", "text": {"type": "plain_text", "text": "🔗 Pipeline"},    "url": "{{pipelineUrl}}"}
              ]
            }
            """;

    // =========================================================================
    // Generic Webhook (minimal JSON object)
    // =========================================================================

    static final String WEBHOOK_SUITE = """
            {
              "suiteName":   "{{suiteName}}",
              "environment": "{{environment}}",
              "status":      "{{status}}",
              "date":        "{{date}}",
              "totalCount":  {{totalCount}},
              "passedCount": {{passedCount}},
              "failedCount": {{failedCount}},
              "successRate": {{successRate}},
              "branch":      "{{gitBranch}}",
              "buildNumber": "{{buildNumber}}",
              "ciName":      "{{ciName}}",
              "reportUrl":   "{{reportUrl}}",
              "pipelineUrl": "{{pipelineUrl}}"
            }
            """;

    // Webhook group and footer are embedded in suite for this minimal format,  no separate slots.
    static final String WEBHOOK_GROUP = "";
    static final String WEBHOOK_FOOTER = "";

    // =========================================================================
    // Accessor
    // =========================================================================

    /**
     * Returns the bundled default template for the given channel type and template slot.
     *
     * @param channelType one of {@code "teams"}, {@code "slack"}, {@code "webhook"}, {@code "log"}
     * @param slot        one of {@code "suite"}, {@code "group"}, {@code "footer"}
     * @return the default template string, or empty string if no default exists
     */
    public static String get(String channelType, String slot) {
        return switch (channelType.toLowerCase()) {
            case "teams" -> switch (slot) {
                case "suite" -> TEAMS_SUITE;
                case "group" -> TEAMS_GROUP;
                case "footer" -> TEAMS_FOOTER;
                default -> "";
            };
            case "slack" -> switch (slot) {
                case "suite" -> SLACK_SUITE;
                case "group" -> SLACK_GROUP;
                case "footer" -> SLACK_FOOTER;
                default -> "";
            };
            case "webhook" -> switch (slot) {
                case "suite" -> WEBHOOK_SUITE;
                default -> "";
            };
            default -> "";
        };
    }
}

