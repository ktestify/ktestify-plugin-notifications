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

import static org.junit.jupiter.api.Assertions.*;

import com.typesafe.config.ConfigFactory;
import io.github.ktestify.config.KtestifyConfig;
import io.github.ktestify.notifications.config.NotificationsConfig;
import io.github.ktestify.notifications.model.NotificationStatus;
import io.github.ktestify.notifications.model.ScenarioEvent;
import io.github.ktestify.notifications.model.SuiteEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.*;

@DisplayName("ScenarioAggregator")
class ScenarioAggregatorTest {

    @BeforeEach
    void setUp() {
        KtestifyConfig.reset();
        ScenarioAggregator.clear();
    }

    @AfterEach
    void tearDown() {
        KtestifyConfig.reset();
        ScenarioAggregator.clear();
    }

    private static ScenarioEvent scenario(String name, NotificationStatus status, String... tags) {
        return ScenarioEvent.builder()
                .scenarioName(name)
                .featureName("test.feature")
                .tags(List.of(tags))
                .status(status)
                .durationMs(100)
                .timestamp(Instant.now())
                .build();
    }

    @Test
    @DisplayName("record() and getEvents() return accumulated events")
    void recordAndGet() {
        ScenarioAggregator.record(scenario("s1", NotificationStatus.PASSED));
        ScenarioAggregator.record(scenario("s2", NotificationStatus.FAILED));

        List<ScenarioEvent> events = ScenarioAggregator.getEvents();
        assertEquals(2, events.size());
    }

    @Test
    @DisplayName("clear() removes all events")
    void clearRemovesAll() {
        ScenarioAggregator.record(scenario("s1", NotificationStatus.PASSED));
        ScenarioAggregator.clear();
        assertTrue(ScenarioAggregator.getEvents().isEmpty());
    }

    @Nested
    @DisplayName("buildSuiteEvent()")
    class BuildSuiteEventTests {

        private NotificationsConfig config;

        @BeforeEach
        void setUp() {
            KtestifyConfig.reset();
            config = NotificationsConfig.from(KtestifyConfig.getOrLoad().getRaw());
        }

        @Test
        @DisplayName("counts are correctly aggregated")
        void countsAggregated() {
            ScenarioAggregator.record(scenario("s1", NotificationStatus.PASSED));
            ScenarioAggregator.record(scenario("s2", NotificationStatus.PASSED));
            ScenarioAggregator.record(scenario("s3", NotificationStatus.FAILED));
            ScenarioAggregator.record(scenario("s4", NotificationStatus.SKIPPED));

            SuiteEvent event = ScenarioAggregator.buildSuiteEvent(config, null, null);

            assertEquals(4, event.getTotalCount());
            assertEquals(2, event.getPassedCount());
            assertEquals(1, event.getFailedCount());
            assertEquals(1, event.getSkippedCount());
        }

        @Test
        @DisplayName("success rate is computed correctly (50% for 1/2 passed)")
        void successRate() {
            ScenarioAggregator.record(scenario("s1", NotificationStatus.PASSED));
            ScenarioAggregator.record(scenario("s2", NotificationStatus.FAILED));

            SuiteEvent event = ScenarioAggregator.buildSuiteEvent(config, null, null);
            assertEquals(50, event.getSuccessRate());
        }

        @Test
        @DisplayName("overall status is FAILED when any scenario failed")
        void statusFailedWhenAnyFailed() {
            ScenarioAggregator.record(scenario("s1", NotificationStatus.PASSED));
            ScenarioAggregator.record(scenario("s2", NotificationStatus.FAILED));

            SuiteEvent event = ScenarioAggregator.buildSuiteEvent(config, null, null);
            assertEquals(NotificationStatus.FAILED, event.getStatus());
        }

        @Test
        @DisplayName("overall status is PASSED when all scenarios pass")
        void statusPassedWhenAllPassed() {
            ScenarioAggregator.record(scenario("s1", NotificationStatus.PASSED));
            ScenarioAggregator.record(scenario("s2", NotificationStatus.PASSED));

            SuiteEvent event = ScenarioAggregator.buildSuiteEvent(config, null, null);
            assertEquals(NotificationStatus.PASSED, event.getStatus());
        }

        @Test
        @DisplayName("untagged group is created for scenarios with no matching configured tag")
        void untaggedGroupCreated() {
            ScenarioAggregator.record(scenario("s1", NotificationStatus.PASSED, "@smoke"));

            SuiteEvent event = ScenarioAggregator.buildSuiteEvent(config, null, null);

            assertFalse(event.getGroupedResults().isEmpty());
            assertEquals("untagged", event.getGroupedResults().get(0).getTag());
        }

        @Test
        @DisplayName("scenarios are grouped by configured tags")
        void scenariosGroupedByTag() {
            KtestifyConfig.reset();
            KtestifyConfig config2 = KtestifyConfig.load(ConfigFactory.parseString("""
                    ktestify.plugins.notifications.groups = [
                      { tag = "orders",   label = "Orders",   emoji = "📦" },
                      { tag = "payments", label = "Payments", emoji = "💳" }
                    ]
                    """));
            NotificationsConfig cfg = NotificationsConfig.from(config2.getRaw());

            ScenarioAggregator.record(scenario("order1", NotificationStatus.PASSED, "@orders"));
            ScenarioAggregator.record(scenario("order2", NotificationStatus.FAILED, "@orders"));
            ScenarioAggregator.record(scenario("pay1", NotificationStatus.PASSED, "@payments"));

            SuiteEvent event = ScenarioAggregator.buildSuiteEvent(cfg, null, null);

            // 2 configured groups (orders, payments),  no untagged
            assertEquals(2, event.getGroupedResults().size());

            var ordersGroup = event.getGroupedResults().stream()
                    .filter(g -> g.getTag().equals("orders"))
                    .findFirst()
                    .orElseThrow();
            assertEquals(2, ordersGroup.getTotalCount());
            assertEquals(1, ordersGroup.getPassedCount());
            assertEquals(1, ordersGroup.getFailedCount());
            assertEquals(50, ordersGroup.getSuccessRate());
            assertEquals("📦", ordersGroup.getEmoji());
        }

        @Test
        @DisplayName("empty suite: 100% success rate, PASSED status")
        void emptyRun() {
            SuiteEvent event = ScenarioAggregator.buildSuiteEvent(config, null, null);

            assertEquals(0, event.getTotalCount());
            assertEquals(100, event.getSuccessRate());
            assertEquals(NotificationStatus.PASSED, event.getStatus());
        }
    }
}
