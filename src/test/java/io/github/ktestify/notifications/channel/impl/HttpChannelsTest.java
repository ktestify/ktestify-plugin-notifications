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
import io.github.ktestify.notifications.model.NotificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for Teams, Slack and generic Webhook channels focusing on behaviour that does NOT require a live HTTP
 * endpoint: type identity, support logic, and the no-URL graceful degradation path.
 */
@DisplayName("HTTP notification channels")
class HttpChannelsTest {

    // =========================================================================
    // TeamsNotificationChannel
    // =========================================================================

    @Nested
    @DisplayName("TeamsNotificationChannel")
    class TeamsTests {

        @Test
        @DisplayName("getType() returns 'teams'")
        void typeIsTeams() {
            var ch = new TeamsNotificationChannel(TestFixtures.teamsChannelConfig(""), null);
            assertEquals("teams", ch.getType());
        }

        @Test
        @DisplayName("sendSuite() with no webhook-url logs a warning and does not throw")
        void noWebhookUrlDoesNotThrow() {
            var ch = new TeamsNotificationChannel(TestFixtures.teamsChannelConfig(""), null);
            assertDoesNotThrow(() -> ch.sendSuite(TestFixtures.passedSuite()));
        }

        @Test
        @DisplayName("sendSuite() with groups and CI context does not throw (no URL configured)")
        void withGroupsAndCiNoThrow() {
            var ch = new TeamsNotificationChannel(TestFixtures.teamsChannelConfig(""), null);
            assertDoesNotThrow(() -> ch.sendSuite(TestFixtures.failedSuiteWithGroups()));
        }

        @Test
        @DisplayName("supportsSuite() returns true for PASSED when on-failure-only=false")
        void supportsPassedWhenNotOnFailureOnly() {
            var ch = new TeamsNotificationChannel(TestFixtures.teamsChannelConfig(""), null);
            assertTrue(ch.supportsSuite(TestFixtures.passedSuite(), false));
        }

        @Test
        @DisplayName("supportsSuite() returns false for PASSED when on-failure-only=true")
        void skipsPassedWhenOnFailureOnly() {
            var ch = new TeamsNotificationChannel(
                    TestFixtures.teamsChannelConfig(""), null);
            // channel's own on-failure-only = false (from fixture), but we test the method param
            // to cover the branch — create a channel with on-failure-only = true
            var cfgOnFailure = io.github.ktestify.notifications.config.ChannelConfig.from(
                    com.typesafe.config.ConfigFactory.parseString(
                            "type=\"teams\", enabled=true, on-failure-only=true, webhook-url=\"\""));
            var chOnFailure = new TeamsNotificationChannel(cfgOnFailure, null);
            assertFalse(chOnFailure.supportsSuite(TestFixtures.passedSuite(), true));
            assertTrue(chOnFailure.supportsSuite(TestFixtures.failedSuite(), true));
        }

        @Test
        @DisplayName("supportsScenario() always returns false")
        void supportsScenarioFalse() {
            var ch = new TeamsNotificationChannel(TestFixtures.teamsChannelConfig(""), null);
            assertFalse(ch.supportsScenario(TestFixtures.passedScenario("s1")));
        }
    }

    // =========================================================================
    // SlackNotificationChannel
    // =========================================================================

    @Nested
    @DisplayName("SlackNotificationChannel")
    class SlackTests {

        @Test
        @DisplayName("getType() returns 'slack'")
        void typeIsSlack() {
            var ch = new SlackNotificationChannel(TestFixtures.slackChannelConfig(""), null);
            assertEquals("slack", ch.getType());
        }

        @Test
        @DisplayName("sendSuite() with no webhook-url logs a warning and does not throw")
        void noWebhookUrlDoesNotThrow() {
            var ch = new SlackNotificationChannel(TestFixtures.slackChannelConfig(""), null);
            assertDoesNotThrow(() -> ch.sendSuite(TestFixtures.passedSuite()));
        }

        @Test
        @DisplayName("sendSuite() with failed suite and no URL does not throw")
        void failedSuiteNoUrlNoThrow() {
            var ch = new SlackNotificationChannel(TestFixtures.slackChannelConfig(""), null);
            assertDoesNotThrow(() -> ch.sendSuite(TestFixtures.failedSuite()));
        }

        @Test
        @DisplayName("supportsSuite() on-failure-only=true skips PASSED, fires on FAILED")
        void onFailureOnlyBehavior() {
            var cfg = io.github.ktestify.notifications.config.ChannelConfig.from(
                    com.typesafe.config.ConfigFactory.parseString(
                            "type=\"slack\", enabled=true, on-failure-only=true, webhook-url=\"\""));
            var ch = new SlackNotificationChannel(cfg, null);
            assertFalse(ch.supportsSuite(TestFixtures.passedSuite(), true));
            assertTrue(ch.supportsSuite(TestFixtures.failedSuite(), true));
        }

        @Test
        @DisplayName("supportsScenario() always returns false")
        void supportsScenarioFalse() {
            var ch = new SlackNotificationChannel(TestFixtures.slackChannelConfig(""), null);
            assertFalse(ch.supportsScenario(TestFixtures.failedScenario("s1")));
        }
    }

    // =========================================================================
    // WebhookNotificationChannel
    // =========================================================================

    @Nested
    @DisplayName("WebhookNotificationChannel")
    class WebhookTests {

        @Test
        @DisplayName("getType() returns 'webhook'")
        void typeIsWebhook() {
            var ch = new WebhookNotificationChannel(TestFixtures.webhookChannelConfig(""), null);
            assertEquals("webhook", ch.getType());
        }

        @Test
        @DisplayName("sendSuite() with no url logs a warning and does not throw")
        void noUrlDoesNotThrow() {
            var ch = new WebhookNotificationChannel(TestFixtures.webhookChannelConfig(""), null);
            assertDoesNotThrow(() -> ch.sendSuite(TestFixtures.passedSuite()));
        }

        @Test
        @DisplayName("sendSuite() with failed suite and no URL does not throw")
        void failedSuiteNoUrlNoThrow() {
            var ch = new WebhookNotificationChannel(TestFixtures.webhookChannelConfig(""), null);
            assertDoesNotThrow(() -> ch.sendSuite(TestFixtures.failedSuite()));
        }

        @Test
        @DisplayName("supportsSuite() on-failure-only=true skips PASSED")
        void onFailureOnlySkipsPassed() {
            var cfg = io.github.ktestify.notifications.config.ChannelConfig.from(
                    com.typesafe.config.ConfigFactory.parseString(
                            "type=\"webhook\", enabled=true, on-failure-only=true, url=\"\""));
            var ch = new WebhookNotificationChannel(cfg, null);
            assertFalse(ch.supportsSuite(TestFixtures.passedSuite(), true));
        }

        @Test
        @DisplayName("supportsSuite() on-failure-only=false always fires")
        void notOnFailureOnlyAlwaysFires() {
            var ch = new WebhookNotificationChannel(TestFixtures.webhookChannelConfig(""), null);
            assertTrue(ch.supportsSuite(TestFixtures.passedSuite(), false));
            assertTrue(ch.supportsSuite(TestFixtures.failedSuite(), false));
        }

        @Test
        @DisplayName("supportsScenario() always returns false")
        void supportsScenarioFalse() {
            var ch = new WebhookNotificationChannel(TestFixtures.webhookChannelConfig(""), null);
            assertFalse(ch.supportsScenario(TestFixtures.passedScenario("s1")));
        }
    }
}

