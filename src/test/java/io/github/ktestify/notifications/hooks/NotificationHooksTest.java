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
package io.github.ktestify.notifications.hooks;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.cucumber.java.Scenario;
import io.github.ktestify.config.KtestifyConfig;
import io.github.ktestify.notifications.service.ScenarioAggregator;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("NotificationHooks")
class NotificationHooksTest {

    @BeforeEach
    void setUp() {
        KtestifyConfig.reset();
        ScenarioAggregator.clear();
        NotificationHooks.reset();
    }

    @AfterEach
    void tearDown() {
        KtestifyConfig.reset();
        ScenarioAggregator.clear();
        NotificationHooks.reset();
    }

    private Scenario mockPassedScenario(String name) {
        Scenario s = mock(Scenario.class);
        when(s.getName()).thenReturn(name);
        var status = mock(io.cucumber.java.Status.class);
        when(status.name()).thenReturn("PASSED");
        when(s.getStatus()).thenReturn(status);
        when(s.isFailed()).thenReturn(false);
        when(s.getSourceTagNames()).thenReturn(List.of("@smoke"));
        when(s.getUri()).thenReturn(URI.create("file:features/test.feature"));
        return s;
    }

    private Scenario mockFailedScenario(String name) {
        Scenario s = mock(Scenario.class);
        when(s.getName()).thenReturn(name);
        var status = mock(io.cucumber.java.Status.class);
        when(status.name()).thenReturn("FAILED");
        when(s.getStatus()).thenReturn(status);
        when(s.isFailed()).thenReturn(true);
        when(s.getSourceTagNames()).thenReturn(List.of("@smoke"));
        when(s.getUri()).thenReturn(URI.create("file:features/test.feature"));
        return s;
    }

    @Nested
    @DisplayName("beforeScenario()")
    class BeforeScenarioTests {

        @Test
        @DisplayName("does not throw")
        void doesNotThrow() {
            NotificationHooks hooks = new NotificationHooks();
            assertDoesNotThrow(() -> hooks.beforeScenario(mockPassedScenario("setup")));
        }
    }

    @Nested
    @DisplayName("afterScenario()")
    class AfterScenarioTests {

        @Test
        @DisplayName("records a PASSED scenario in the aggregator")
        void recordsPassedScenario() {
            NotificationHooks hooks = new NotificationHooks();
            hooks.beforeScenario(mockPassedScenario("my scenario"));
            hooks.afterScenario(mockPassedScenario("my scenario"));

            assertEquals(1, ScenarioAggregator.getEvents().size());
        }

        @Test
        @DisplayName("records a FAILED scenario in the aggregator")
        void recordsFailedScenario() {
            NotificationHooks hooks = new NotificationHooks();
            hooks.beforeScenario(mockFailedScenario("failing scenario"));
            hooks.afterScenario(mockFailedScenario("failing scenario"));

            assertEquals(1, ScenarioAggregator.getEvents().size());
            assertEquals(
                    io.github.ktestify.notifications.model.NotificationStatus.FAILED,
                    ScenarioAggregator.getEvents().get(0).getStatus());
        }

        @Test
        @DisplayName("multiple scenarios are all recorded")
        void multipleScenariosRecorded() {
            NotificationHooks hooks1 = new NotificationHooks();
            NotificationHooks hooks2 = new NotificationHooks();

            hooks1.beforeScenario(mockPassedScenario("s1"));
            hooks1.afterScenario(mockPassedScenario("s1"));

            hooks2.beforeScenario(mockFailedScenario("s2"));
            hooks2.afterScenario(mockFailedScenario("s2"));

            assertEquals(2, ScenarioAggregator.getEvents().size());
        }

        @Test
        @DisplayName("does not throw even if notification service dispatch fails internally")
        void doesNotPropagateDispatchErrors() {
            NotificationHooks hooks = new NotificationHooks();
            hooks.beforeScenario(mockPassedScenario("s1"));
            assertDoesNotThrow(() -> hooks.afterScenario(mockPassedScenario("s1")));
        }
    }

    @Nested
    @DisplayName("afterAllScenarios()")
    class AfterAllTests {

        @Test
        @DisplayName("does not throw with empty scenario list")
        void noThrowWithEmptyList() {
            assertDoesNotThrow(NotificationHooks::afterAllScenarios);
        }

        @Test
        @DisplayName("does not throw with accumulated scenarios")
        void noThrowWithScenarios() {
            NotificationHooks hooks = new NotificationHooks();
            hooks.beforeScenario(mockPassedScenario("s1"));
            hooks.afterScenario(mockPassedScenario("s1"));
            hooks.beforeScenario(mockFailedScenario("s2"));
            hooks.afterScenario(mockFailedScenario("s2"));

            assertDoesNotThrow(NotificationHooks::afterAllScenarios);
        }

        @Test
        @DisplayName("calling afterAllScenarios() twice does not throw")
        void calledTwiceNoThrow() {
            assertDoesNotThrow(NotificationHooks::afterAllScenarios);
            assertDoesNotThrow(NotificationHooks::afterAllScenarios);
        }
    }
}
