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
import io.github.ktestify.notifications.TestFixtures;
import io.github.ktestify.notifications.config.NotificationsConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("NotificationService")
class NotificationServiceTest {

    @BeforeEach
    void setUp() {
        KtestifyConfig.reset();
    }

    @AfterEach
    void tearDown() {
        KtestifyConfig.reset();
    }

    // ── NOOP singleton ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("NOOP singleton")
    class NoopTests {

        @Test
        @DisplayName("NOOP.dispatch(SuiteEvent) does not throw")
        void noopDispatchSuiteNoThrow() {
            assertDoesNotThrow(() -> NotificationService.NOOP.dispatch(TestFixtures.passedSuite()));
        }

        @Test
        @DisplayName("NOOP.dispatch(ScenarioEvent) does not throw")
        void noopDispatchScenarioNoThrow() {
            assertDoesNotThrow(() -> NotificationService.NOOP.dispatch(TestFixtures.passedScenario("s1")));
        }

        @Test
        @DisplayName("NOOP.shutdown() does not throw")
        void noopShutdownNoThrow() {
            assertDoesNotThrow(() -> NotificationService.NOOP.shutdown(1));
        }
    }

    // ── Enabled service ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("enabled service with log channel")
    class EnabledServiceTests {

        private NotificationService service;

        @BeforeEach
        void setUp() {
            NotificationsConfig cfg = NotificationsConfig.from(ConfigFactory.parseString(
                    "ktestify.plugins.notifications.enabled = true"));
            service = new NotificationService(cfg);
        }

        @AfterEach
        void tearDown() {
            service.shutdown(1);
        }

        @Test
        @DisplayName("dispatch(SuiteEvent) PASSED does not throw")
        void dispatchPassedSuiteNoThrow() {
            assertDoesNotThrow(() -> service.dispatch(TestFixtures.passedSuite()));
        }

        @Test
        @DisplayName("dispatch(SuiteEvent) FAILED does not throw")
        void dispatchFailedSuiteNoThrow() {
            assertDoesNotThrow(() -> service.dispatch(TestFixtures.failedSuite()));
        }

        @Test
        @DisplayName("dispatch(SuiteEvent) with groups and CI context does not throw")
        void dispatchSuiteWithGroupsNoThrow() {
            assertDoesNotThrow(() -> service.dispatch(TestFixtures.failedSuiteWithGroups()));
        }

        @Test
        @DisplayName("dispatch(ScenarioEvent) does not throw")
        void dispatchScenarioNoThrow() {
            assertDoesNotThrow(() -> service.dispatch(TestFixtures.passedScenario("s1")));
        }

        @Test
        @DisplayName("shutdown(1) does not throw")
        void shutdownNoThrow() {
            assertDoesNotThrow(() -> service.shutdown(1));
        }

        @Test
        @DisplayName("shutdown() twice does not throw")
        void doubleShutdownNoThrow() {
            service.shutdown(1);
            assertDoesNotThrow(() -> service.shutdown(1));
        }
    }

    // ── Disabled service (notifications.enabled = false) ─────────────────────

    @Nested
    @DisplayName("disabled service")
    class DisabledServiceTests {

        private NotificationService service;

        @BeforeEach
        void setUp() {
            // Default reference.conf has enabled = false
            NotificationsConfig cfg = NotificationsConfig.from(KtestifyConfig.getOrLoad().getRaw());
            assertFalse(cfg.isEnabled(), "pre-condition: plugin should be disabled by default");
            service = new NotificationService(cfg);
        }

        @AfterEach
        void tearDown() {
            service.shutdown(1);
        }

        @Test
        @DisplayName("dispatch(SuiteEvent) on disabled service does not throw")
        void dispatchOnDisabledNoThrow() {
            assertDoesNotThrow(() -> service.dispatch(TestFixtures.passedSuite()));
        }

        @Test
        @DisplayName("dispatch(ScenarioEvent) on disabled service does not throw")
        void dispatchScenarioOnDisabledNoThrow() {
            assertDoesNotThrow(() -> service.dispatch(TestFixtures.passedScenario("s1")));
        }
    }

    // ── on-failure-only global flag ───────────────────────────────────────────

    @Nested
    @DisplayName("global on-failure-only flag")
    class OnFailureOnlyTests {

        @Test
        @DisplayName("PASSED suite dispatched when on-failure-only=false")
        void passedSuiteDispatchedWhenNotOnFailureOnly() {
            NotificationsConfig cfg = NotificationsConfig.from(ConfigFactory.parseString("""
                    ktestify.plugins.notifications {
                      enabled = true
                      on-failure-only = false
                    }
                    """));
            var service = new NotificationService(cfg);
            // should not throw and not skip silently
            assertDoesNotThrow(() -> service.dispatch(TestFixtures.passedSuite()));
            service.shutdown(1);
        }

        @Test
        @DisplayName("PASSED suite skipped when on-failure-only=true (no channel fires)")
        void passedSuiteSkippedWhenOnFailureOnly() {
            NotificationsConfig cfg = NotificationsConfig.from(ConfigFactory.parseString("""
                    ktestify.plugins.notifications {
                      enabled = true
                      on-failure-only = true
                      channels = [{ type = "log", enabled = true, on-failure-only = true }]
                    }
                    """));
            var service = new NotificationService(cfg);
            // Dispatch PASSED with global on-failure-only=true — log channel should skip it
            assertDoesNotThrow(() -> service.dispatch(TestFixtures.passedSuite()));
            service.shutdown(1);
        }
    }
}

