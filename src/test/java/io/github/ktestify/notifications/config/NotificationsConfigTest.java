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
package io.github.ktestify.notifications.config;

import static org.junit.jupiter.api.Assertions.*;

import com.typesafe.config.ConfigFactory;
import io.github.ktestify.config.KtestifyConfig;
import org.junit.jupiter.api.*;

@DisplayName("NotificationsConfig")
class NotificationsConfigTest {

    @AfterEach
    void tearDown() {
        KtestifyConfig.reset();
    }

    @Test
    @DisplayName("defaults: plugin is disabled out of the box")
    void defaultsPluginDisabled() {
        NotificationsConfig cfg =
                NotificationsConfig.from(KtestifyConfig.getOrLoad().getRaw());
        assertFalse(cfg.isEnabled());
    }

    @Test
    @DisplayName("defaults: thresholds are 75 / 50")
    void defaultThresholds() {
        NotificationsConfig cfg =
                NotificationsConfig.from(KtestifyConfig.getOrLoad().getRaw());
        assertEquals(75, cfg.getThresholds().getGood());
        assertEquals(50, cfg.getThresholds().getWarning());
    }

    @Test
    @DisplayName("defaults: log channel enabled, zero other channels")
    void defaultChannels() {
        NotificationsConfig cfg =
                NotificationsConfig.from(KtestifyConfig.getOrLoad().getRaw());
        assertEquals(1, cfg.getEnabledChannels().size());
        assertEquals("log", cfg.getEnabledChannels().get(0).getType());
    }

    @Test
    @DisplayName("defaults: groups list is empty")
    void defaultGroupsEmpty() {
        NotificationsConfig cfg =
                NotificationsConfig.from(KtestifyConfig.getOrLoad().getRaw());
        assertTrue(cfg.getGroups().isEmpty());
    }

    @Test
    @DisplayName("enabled flag can be overridden")
    void enabledOverride() {
        KtestifyConfig config =
                KtestifyConfig.load(ConfigFactory.parseString("ktestify.plugins.notifications.enabled = true"));
        NotificationsConfig cfg = NotificationsConfig.from(config.getRaw());
        assertTrue(cfg.isEnabled());
    }

    @Test
    @DisplayName("groups are parsed correctly")
    void groupsParsed() {
        KtestifyConfig config = KtestifyConfig.load(ConfigFactory.parseString("""
                ktestify.plugins.notifications.groups = [
                  { tag = "orders", label = "Order Service", emoji = "📦" },
                  { tag = "payments" }
                ]
                """));
        NotificationsConfig cfg = NotificationsConfig.from(config.getRaw());

        assertEquals(2, cfg.getGroups().size());
        assertEquals("orders", cfg.getGroups().get(0).getTag());
        assertEquals("Order Service", cfg.getGroups().get(0).getLabel());
        assertEquals("📦", cfg.getGroups().get(0).getEmoji());
        // label defaults to tag when not specified
        assertEquals("payments", cfg.getGroups().get(1).getLabel());
        // emoji defaults to 🏷️
        assertEquals("🏷️", cfg.getGroups().get(1).getEmoji());
    }

    @Test
    @DisplayName("channel template object is parsed into suite/group/footer slots")
    void channelTemplateObjectParsed() {
        KtestifyConfig config = KtestifyConfig.load(ConfigFactory.parseString("""
                ktestify.plugins.notifications.channels = [
                  {
                    type    = "teams"
                    enabled = true
                    template {
                      suite  = "my-suite-template"
                      group  = "my-group-template"
                      footer = "my-footer-template"
                    }
                  }
                ]
                """));
        NotificationsConfig cfg = NotificationsConfig.from(config.getRaw());
        ChannelConfig ch = cfg.getEnabledChannels().get(0);

        assertEquals("my-suite-template", ch.getTemplateSuite());
        assertEquals("my-group-template", ch.getTemplateGroup());
        assertEquals("my-footer-template", ch.getTemplateFooter());
    }

    @Nested
    @DisplayName("ThresholdsConfig.computeStyle()")
    class StyleTests {

        private ThresholdsConfig thresholds;

        @BeforeEach
        void setUp() {
            KtestifyConfig.reset();
            thresholds = NotificationsConfig.from(KtestifyConfig.getOrLoad().getRaw())
                    .getThresholds();
        }

        @Test
        void rateAbove75IsGood() {
            assertEquals("good", thresholds.computeStyle(100));
        }

        @Test
        void rateAt75IsGood() {
            assertEquals("good", thresholds.computeStyle(75));
        }

        @Test
        void rateBetween50And74IsWarning() {
            assertEquals("warning", thresholds.computeStyle(60));
        }

        @Test
        void rateAt50IsWarning() {
            assertEquals("warning", thresholds.computeStyle(50));
        }

        @Test
        void rateBelow50IsAttention() {
            assertEquals("attention", thresholds.computeStyle(49));
        }

        @Test
        void rateZeroIsAttention() {
            assertEquals("attention", thresholds.computeStyle(0));
        }
    }
}
