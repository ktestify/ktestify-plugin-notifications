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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.cucumber.java.Scenario;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ScenarioEvent")
class ScenarioEventTest {

    private Scenario mockScenario(String name, String statusName, String... tags) {
        Scenario s = mock(Scenario.class);
        when(s.getName()).thenReturn(name);

        // Status mock via an anonymous enum-like object with name()
        Object mockStatus = mock(Object.class);
        when(mockStatus.toString()).thenReturn(statusName);

        // Scenario.getStatus() returns an enum; we use the name() of the status
        var status = mock(io.cucumber.java.Status.class);
        when(status.name()).thenReturn(statusName);
        when(s.getStatus()).thenReturn(status);
        when(s.isFailed()).thenReturn("FAILED".equals(statusName));

        when(s.getSourceTagNames()).thenReturn(List.of(tags));
        when(s.getUri()).thenReturn(URI.create("file:src/test/resources/features/orders.feature"));
        return s;
    }

    @Nested
    @DisplayName("from(Scenario, durationMs)")
    class FromScenarioTests {

        @Test
        @DisplayName("PASSED scenario maps to PASSED status")
        void passedStatusMapped() {
            Scenario s = mockScenario("my scenario", "PASSED");
            ScenarioEvent event = ScenarioEvent.from(s, 300L);
            assertEquals(NotificationStatus.PASSED, event.getStatus());
        }

        @Test
        @DisplayName("FAILED scenario maps to FAILED status")
        void failedStatusMapped() {
            Scenario s = mockScenario("my scenario", "FAILED");
            ScenarioEvent event = ScenarioEvent.from(s, 300L);
            assertEquals(NotificationStatus.FAILED, event.getStatus());
        }

        @Test
        @DisplayName("SKIPPED scenario maps to SKIPPED status")
        void skippedStatusMapped() {
            Scenario s = mockScenario("my scenario", "SKIPPED");
            ScenarioEvent event = ScenarioEvent.from(s, 100L);
            assertEquals(NotificationStatus.SKIPPED, event.getStatus());
        }

        @Test
        @DisplayName("PENDING scenario maps to SKIPPED status")
        void pendingStatusMappedToSkipped() {
            Scenario s = mockScenario("pending scenario", "PENDING");
            ScenarioEvent event = ScenarioEvent.from(s, 50L);
            assertEquals(NotificationStatus.SKIPPED, event.getStatus());
        }

        @Test
        @DisplayName("AMBIGUOUS scenario maps to FAILED status")
        void ambiguousStatusMappedToFailed() {
            Scenario s = mockScenario("ambiguous scenario", "AMBIGUOUS");
            ScenarioEvent event = ScenarioEvent.from(s, 50L);
            assertEquals(NotificationStatus.FAILED, event.getStatus());
        }

        @Test
        @DisplayName("UNDEFINED scenario maps to FAILED status")
        void undefinedStatusMappedToFailed() {
            Scenario s = mockScenario("undefined scenario", "UNDEFINED");
            ScenarioEvent event = ScenarioEvent.from(s, 50L);
            assertEquals(NotificationStatus.FAILED, event.getStatus());
        }

        @Test
        @DisplayName("scenario name is captured")
        void scenarioNameCaptured() {
            Scenario s = mockScenario("Place an order", "PASSED");
            ScenarioEvent event = ScenarioEvent.from(s, 100L);
            assertEquals("Place an order", event.getScenarioName());
        }

        @Test
        @DisplayName("duration is captured")
        void durationCaptured() {
            Scenario s = mockScenario("my scenario", "PASSED");
            ScenarioEvent event = ScenarioEvent.from(s, 1234L);
            assertEquals(1234L, event.getDurationMs());
        }

        @Test
        @DisplayName("tags are captured")
        void tagsCaptured() {
            Scenario s = mockScenario("my scenario", "PASSED", "@smoke", "@orders");
            ScenarioEvent event = ScenarioEvent.from(s, 100L);
            assertEquals(List.of("@smoke", "@orders"), event.getTags());
        }

        @Test
        @DisplayName("feature name is extracted from URI")
        void featureNameExtractedFromUri() {
            Scenario s = mockScenario("my scenario", "PASSED");
            when(s.getUri()).thenReturn(URI.create("file:///src/test/resources/features/orders.feature"));
            ScenarioEvent event = ScenarioEvent.from(s, 100L);
            assertEquals("orders.feature", event.getFeatureName());
        }

        @Test
        @DisplayName("null URI results in 'unknown' feature name")
        void nullUriResultsInUnknown() {
            Scenario s = mockScenario("my scenario", "PASSED");
            when(s.getUri()).thenReturn(null);
            ScenarioEvent event = ScenarioEvent.from(s, 100L);
            assertEquals("unknown", event.getFeatureName());
        }

        @Test
        @DisplayName("timestamp is set to current time")
        void timestampIsSet() {
            Scenario s = mockScenario("my scenario", "PASSED");
            ScenarioEvent event = ScenarioEvent.from(s, 100L);
            assertNotNull(event.getTimestamp());
        }
    }
}


