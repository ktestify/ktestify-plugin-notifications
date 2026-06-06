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
package io.github.ktestify.notifications.channel;

import static org.junit.jupiter.api.Assertions.*;

import com.typesafe.config.ConfigFactory;
import io.github.ktestify.config.KtestifyConfig;
import io.github.ktestify.notifications.channel.impl.LogNotificationChannel;
import io.github.ktestify.notifications.channel.impl.SlackNotificationChannel;
import io.github.ktestify.notifications.channel.impl.TeamsNotificationChannel;
import io.github.ktestify.notifications.channel.impl.WebhookNotificationChannel;
import io.github.ktestify.notifications.config.NotificationsConfig;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("NotificationChannelFactory")
class NotificationChannelFactoryTest {

    @BeforeEach
    void setUp() {
        KtestifyConfig.reset();
    }

    @AfterEach
    void tearDown() {
        KtestifyConfig.reset();
    }

    @Test
    @DisplayName("empty channels list returns empty result")
    void emptyChannels() {
        NotificationsConfig cfg = NotificationsConfig.from(ConfigFactory.parseString("""
                ktestify.plugins.notifications {
                  enabled = true
                  channels = []
                }
                """));
        List<NotificationChannel> channels = NotificationChannelFactory.build(cfg);
        assertTrue(channels.isEmpty());
    }

    @Test
    @DisplayName("disabled channels are excluded from result")
    void disabledChannelExcluded() {
        NotificationsConfig cfg = NotificationsConfig.from(ConfigFactory.parseString("""
                ktestify.plugins.notifications {
                  enabled = true
                  channels = [
                    { type = "teams", enabled = false, webhook-url = "" },
                    { type = "log",   enabled = true }
                  ]
                }
                """));
        List<NotificationChannel> channels = NotificationChannelFactory.build(cfg);
        assertEquals(1, channels.size());
        assertInstanceOf(LogNotificationChannel.class, channels.get(0));
    }

    @Nested
    @DisplayName("built-in channel types")
    class BuiltinChannelTypes {

        @Test
        @DisplayName("'log' type creates LogNotificationChannel")
        void logType() {
            NotificationsConfig cfg = NotificationsConfig.from(ConfigFactory.parseString("""
                    ktestify.plugins.notifications {
                      enabled = true
                      channels = [{ type = "log", enabled = true }]
                    }
                    """));
            List<NotificationChannel> channels = NotificationChannelFactory.build(cfg);
            assertEquals(1, channels.size());
            assertInstanceOf(LogNotificationChannel.class, channels.get(0));
        }

        @Test
        @DisplayName("'teams' type creates TeamsNotificationChannel")
        void teamsType() {
            NotificationsConfig cfg = NotificationsConfig.from(ConfigFactory.parseString("""
                    ktestify.plugins.notifications {
                      enabled = true
                      channels = [{ type = "teams", enabled = true, webhook-url = "" }]
                    }
                    """));
            List<NotificationChannel> channels = NotificationChannelFactory.build(cfg);
            assertEquals(1, channels.size());
            assertInstanceOf(TeamsNotificationChannel.class, channels.get(0));
        }

        @Test
        @DisplayName("'slack' type creates SlackNotificationChannel")
        void slackType() {
            NotificationsConfig cfg = NotificationsConfig.from(ConfigFactory.parseString("""
                    ktestify.plugins.notifications {
                      enabled = true
                      channels = [{ type = "slack", enabled = true, webhook-url = "" }]
                    }
                    """));
            List<NotificationChannel> channels = NotificationChannelFactory.build(cfg);
            assertEquals(1, channels.size());
            assertInstanceOf(SlackNotificationChannel.class, channels.get(0));
        }

        @Test
        @DisplayName("'webhook' type creates WebhookNotificationChannel")
        void webhookType() {
            NotificationsConfig cfg = NotificationsConfig.from(ConfigFactory.parseString("""
                    ktestify.plugins.notifications {
                      enabled = true
                      channels = [{ type = "webhook", enabled = true, url = "" }]
                    }
                    """));
            List<NotificationChannel> channels = NotificationChannelFactory.build(cfg);
            assertEquals(1, channels.size());
            assertInstanceOf(WebhookNotificationChannel.class, channels.get(0));
        }

        @Test
        @DisplayName("unknown type is skipped with a warning")
        void unknownTypeSkipped() {
            NotificationsConfig cfg = NotificationsConfig.from(ConfigFactory.parseString("""
                    ktestify.plugins.notifications {
                      enabled = true
                      channels = [
                        { type = "unknown-channel", enabled = true },
                        { type = "log", enabled = true }
                      ]
                    }
                    """));
            List<NotificationChannel> channels = NotificationChannelFactory.build(cfg);
            // Only the log channel should be created; the unknown type is skipped
            assertEquals(1, channels.size());
            assertInstanceOf(LogNotificationChannel.class, channels.get(0));
        }
    }

    @Test
    @DisplayName("multiple enabled channels are all created")
    void multipleChannels() {
        NotificationsConfig cfg = NotificationsConfig.from(ConfigFactory.parseString("""
                ktestify.plugins.notifications {
                  enabled = true
                  channels = [
                    { type = "log",   enabled = true },
                    { type = "teams", enabled = true, webhook-url = "" },
                    { type = "slack", enabled = true, webhook-url = "" }
                  ]
                }
                """));
        List<NotificationChannel> channels = NotificationChannelFactory.build(cfg);
        assertEquals(3, channels.size());
    }
}
