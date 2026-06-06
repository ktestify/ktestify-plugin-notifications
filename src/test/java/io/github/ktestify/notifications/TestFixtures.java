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
package io.github.ktestify.notifications;

import com.typesafe.config.ConfigFactory;
import io.github.ktestify.notifications.config.ChannelConfig;
import io.github.ktestify.notifications.config.NotificationsConfig;
import io.github.ktestify.notifications.model.CiContext;
import io.github.ktestify.notifications.model.GitContext;
import io.github.ktestify.notifications.model.GroupResult;
import io.github.ktestify.notifications.model.NotificationStatus;
import io.github.ktestify.notifications.model.ScenarioEvent;
import io.github.ktestify.notifications.model.SuiteEvent;
import java.time.Instant;
import java.util.List;

/**
 * Shared test fixtures used across multiple test classes.
 *
 * @since 1.0.0
 */
public final class TestFixtures {

    private TestFixtures() {}

    // ── SuiteEvent builders ───────────────────────────────────────────────────

    public static SuiteEvent passedSuite() {
        return SuiteEvent.builder()
                .suiteName("Test Suite")
                .environment("test")
                .reportUrl("https://example.com/report")
                .status(NotificationStatus.PASSED)
                .totalCount(10)
                .passedCount(10)
                .failedCount(0)
                .skippedCount(0)
                .durationMs(5_000)
                .successRate(100)
                .style("good")
                .groupedResults(List.of())
                .ciContext(null)
                .gitContext(null)
                .timestamp(Instant.parse("2026-06-06T12:00:00Z"))
                .build();
    }

    public static SuiteEvent failedSuite() {
        return SuiteEvent.builder()
                .suiteName("Test Suite")
                .environment("staging")
                .reportUrl("https://example.com/report")
                .status(NotificationStatus.FAILED)
                .totalCount(10)
                .passedCount(7)
                .failedCount(3)
                .skippedCount(0)
                .durationMs(8_000)
                .successRate(70)
                .style("warning")
                .groupedResults(List.of())
                .ciContext(null)
                .gitContext(null)
                .timestamp(Instant.parse("2026-06-06T12:00:00Z"))
                .build();
    }

    public static SuiteEvent failedSuiteWithGroups() {
        GroupResult ordersGroup = GroupResult.builder()
                .groupLabel("Orders")
                .emoji("📦")
                .tag("orders")
                .totalCount(5)
                .passedCount(5)
                .failedCount(0)
                .skippedCount(0)
                .successRate(100)
                .style("good")
                .build();
        GroupResult paymentsGroup = GroupResult.builder()
                .groupLabel("Payments")
                .emoji("💳")
                .tag("payments")
                .totalCount(5)
                .passedCount(2)
                .failedCount(3)
                .skippedCount(0)
                .successRate(40)
                .style("attention")
                .build();
        return SuiteEvent.builder()
                .suiteName("Test Suite")
                .environment("staging")
                .reportUrl("https://example.com/report")
                .status(NotificationStatus.FAILED)
                .totalCount(10)
                .passedCount(7)
                .failedCount(3)
                .skippedCount(0)
                .durationMs(8_000)
                .successRate(70)
                .style("warning")
                .groupedResults(List.of(ordersGroup, paymentsGroup))
                .ciContext(ciContext())
                .gitContext(gitContext())
                .timestamp(Instant.parse("2026-06-06T12:00:00Z"))
                .build();
    }

    public static CiContext ciContext() {
        return CiContext.builder()
                .ciName("GitHub Actions")
                .pipelineUrl("https://github.com/org/repo/actions/runs/123")
                .buildNumber("123")
                .build();
    }

    public static GitContext gitContext() {
        return GitContext.builder()
                .branch("main")
                .revision("a1b2c3d")
                .tag(null)
                .remote("https://github.com/org/repo.git")
                .build();
    }

    // ── ScenarioEvent builders ────────────────────────────────────────────────

    public static ScenarioEvent passedScenario(String name, String... tags) {
        return ScenarioEvent.builder()
                .scenarioName(name)
                .featureName("test.feature")
                .tags(List.of(tags))
                .status(NotificationStatus.PASSED)
                .durationMs(200)
                .timestamp(Instant.now())
                .build();
    }

    public static ScenarioEvent failedScenario(String name, String... tags) {
        return ScenarioEvent.builder()
                .scenarioName(name)
                .featureName("test.feature")
                .tags(List.of(tags))
                .status(NotificationStatus.FAILED)
                .durationMs(500)
                .timestamp(Instant.now())
                .build();
    }

    // ── ChannelConfig builders ────────────────────────────────────────────────

    public static ChannelConfig logChannelConfig() {
        return ChannelConfig.from(ConfigFactory.parseString("type = \"log\", enabled = true, on-failure-only = false"));
    }

    public static ChannelConfig logChannelOnFailureOnly() {
        return ChannelConfig.from(ConfigFactory.parseString("type = \"log\", enabled = true, on-failure-only = true"));
    }

    public static ChannelConfig teamsChannelConfig(String webhookUrl) {
        return ChannelConfig.from(
                ConfigFactory.parseString("type = \"teams\", enabled = true, webhook-url = \"" + webhookUrl + "\""));
    }

    public static ChannelConfig slackChannelConfig(String webhookUrl) {
        return ChannelConfig.from(
                ConfigFactory.parseString("type = \"slack\", enabled = true, webhook-url = \"" + webhookUrl + "\""));
    }

    public static ChannelConfig webhookChannelConfig(String url) {
        return ChannelConfig.from(ConfigFactory.parseString(
                String.format("type = \"webhook\", enabled = true, url = \"%s\", method = \"POST\"", url)));
    }

    // ── NotificationsConfig builders ─────────────────────────────────────────

    public static NotificationsConfig enabledWithLogChannel() {
        return NotificationsConfig.from(ConfigFactory.parseString("ktestify.plugins.notifications.enabled = true"));
    }
}
