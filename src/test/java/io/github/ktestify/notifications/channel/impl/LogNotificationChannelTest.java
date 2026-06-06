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
package io.github.ktestify.notifications.channel.impl;

import static org.junit.jupiter.api.Assertions.*;

import io.github.ktestify.notifications.TestFixtures;
import io.github.ktestify.notifications.config.ChannelConfig;
import io.github.ktestify.notifications.model.NotificationStatus;
import io.github.ktestify.notifications.model.SuiteEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("LogNotificationChannel")
class LogNotificationChannelTest {

    private LogNotificationChannel channel(boolean onFailureOnly) {
        ChannelConfig cfg = onFailureOnly ? TestFixtures.logChannelOnFailureOnly() : TestFixtures.logChannelConfig();
        return new LogNotificationChannel(cfg);
    }

    @Test
    @DisplayName("getType() returns 'log'")
    void typeIsLog() {
        assertEquals("log", channel(false).getType());
    }

    @Nested
    @DisplayName("sendSuite()")
    class SendSuiteTests {

        @Test
        @DisplayName("PASSED suite event does not throw")
        void passedEventNoThrow() {
            assertDoesNotThrow(() -> channel(false).sendSuite(TestFixtures.passedSuite()));
        }

        @Test
        @DisplayName("FAILED suite event does not throw")
        void failedEventNoThrow() {
            assertDoesNotThrow(() -> channel(false).sendSuite(TestFixtures.failedSuite()));
        }

        @Test
        @DisplayName("suite with groups and CI context does not throw")
        void suiteWithGroupsAndCiNoThrow() {
            assertDoesNotThrow(() -> channel(false).sendSuite(TestFixtures.failedSuiteWithGroups()));
        }

        @Test
        @DisplayName("suite with empty reportUrl does not throw")
        void suiteWithEmptyReportUrlNoThrow() {
            SuiteEvent event = SuiteEvent.builder()
                    .suiteName("Suite")
                    .environment("")
                    .reportUrl("")
                    .status(NotificationStatus.PASSED)
                    .totalCount(1)
                    .passedCount(1)
                    .failedCount(0)
                    .skippedCount(0)
                    .durationMs(100)
                    .successRate(100)
                    .style("good")
                    .groupedResults(java.util.List.of())
                    .ciContext(null)
                    .gitContext(null)
                    .timestamp(java.time.Instant.now())
                    .build();
            assertDoesNotThrow(() -> channel(false).sendSuite(event));
        }
    }

    @Nested
    @DisplayName("supportsSuite()")
    class SupportsSuiteTests {

        @Test
        @DisplayName("on-failure-only=false supports PASSED suite")
        void alwaysTrueWhenNotOnFailureOnly() {
            assertTrue(channel(false).supportsSuite(TestFixtures.passedSuite(), false));
        }

        @Test
        @DisplayName("on-failure-only=false supports FAILED suite")
        void alwaysTrueForFailedWhenNotOnFailureOnly() {
            assertTrue(channel(false).supportsSuite(TestFixtures.failedSuite(), false));
        }

        @Test
        @DisplayName("on-failure-only=true skips PASSED suite")
        void skipsPassedWhenOnFailureOnly() {
            assertFalse(channel(true).supportsSuite(TestFixtures.passedSuite(), true));
        }

        @Test
        @DisplayName("on-failure-only=true fires on FAILED suite")
        void firesOnFailedWhenOnFailureOnly() {
            assertTrue(channel(true).supportsSuite(TestFixtures.failedSuite(), true));
        }
    }

    @Nested
    @DisplayName("supportsScenario()")
    class SupportsScenarioTests {

        @Test
        @DisplayName("always returns false (suite-only channel)")
        void alwaysFalse() {
            assertFalse(channel(false).supportsScenario(TestFixtures.passedScenario("s1")));
            assertFalse(channel(false).supportsScenario(TestFixtures.failedScenario("s2")));
        }
    }

    @Nested
    @DisplayName("sendScenario()")
    class SendScenarioTests {

        @Test
        @DisplayName("sendScenario() default no-op does not throw")
        void scenarioNoOpNoThrow() {
            assertDoesNotThrow(() -> channel(false).sendScenario(TestFixtures.passedScenario("s1")));
        }
    }
}
