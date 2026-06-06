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

import static org.junit.jupiter.api.Assertions.*;

import com.typesafe.config.ConfigFactory;
import io.github.ktestify.config.KtestifyConfig;
import io.github.ktestify.plugin.PluginContext;
import org.junit.jupiter.api.*;

/** Unit tests for {@link NotificationsPlugin}, lifecycle, metadata, and configuration validation. */
@DisplayName("NotificationsPlugin")
class NotificationsPluginTest {

    private NotificationsPlugin plugin;
    private PluginContext ctx;

    @BeforeEach
    void setUp() {
        KtestifyConfig.reset();
        plugin = new NotificationsPlugin();
        ctx = KtestifyConfig::getOrLoad;
    }

    @AfterEach
    void tearDown() {
        KtestifyConfig.reset();
    }

    @Nested
    @DisplayName("Metadata")
    class MetadataTests {

        @Test
        @DisplayName("getId() returns 'notifications'")
        void idIsNotifications() {
            assertEquals("notifications", plugin.getId());
        }

        @Test
        @DisplayName("getVersion() is non-blank")
        void versionIsNonBlank() {
            assertNotNull(plugin.getVersion());
            assertFalse(plugin.getVersion().isBlank());
        }

        @Test
        @DisplayName("getGluePackage() points to the hooks package")
        void gluePackageIsHooksPackage() {
            assertEquals("io.github.ktestify.notifications.hooks", plugin.getGluePackage());
        }

        @Test
        @DisplayName("getAuthorName() is non-blank")
        void authorNameNonBlank() {
            assertFalse(plugin.getAuthorName().isBlank());
        }
    }

    @Nested
    @DisplayName("initialize()")
    class InitializeTests {

        @Test
        @DisplayName("initialize() succeeds when plugin is disabled (default)")
        void initializeSucceedsWhenDisabled() {
            assertDoesNotThrow(() -> plugin.initialize(ctx));
        }

        @Test
        @DisplayName("initialize() succeeds when plugin is enabled with log channel only")
        void initializeSucceedsWhenEnabled() {
            KtestifyConfig cfg =
                    KtestifyConfig.load(ConfigFactory.parseString("ktestify.plugins.notifications.enabled = true"));

            assertDoesNotThrow(() -> plugin.initialize(() -> cfg));
        }

        @Test
        @DisplayName("initialize() succeeds when Teams channel is configured (no webhook-url set)")
        void initializeSucceedsWithTeamsNoUrl() {
            KtestifyConfig cfg = KtestifyConfig.load(ConfigFactory.parseString("""
                    ktestify.plugins.notifications {
                      enabled = true
                      channels = [{ type = "teams", enabled = true }]
                    }
                    """));
            assertDoesNotThrow(() -> plugin.initialize(() -> cfg));
        }
    }

    @Nested
    @DisplayName("shutdown()")
    class ShutdownTests {

        @Test
        @DisplayName("shutdown() before initialize() does not throw")
        void shutdownBeforeInit() {
            assertDoesNotThrow(plugin::shutdown);
        }

        @Test
        @DisplayName("shutdown() after initialize() does not throw")
        void shutdownAfterInit() {
            plugin.initialize(ctx);
            assertDoesNotThrow(plugin::shutdown);
        }
    }
}
