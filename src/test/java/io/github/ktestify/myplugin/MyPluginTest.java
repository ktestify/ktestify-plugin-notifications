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
package io.github.ktestify.myplugin;

import static org.junit.jupiter.api.Assertions.*;

import com.typesafe.config.ConfigFactory;
import io.github.ktestify.config.KtestifyConfig;
import io.github.ktestify.plugin.PluginContext;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link MyPlugin} — lifecycle, metadata, and configuration validation.
 *
 * <p>These tests do not require Docker; they only exercise config loading and plugin contract methods.
 *
 * <p>TODO: add tests for your plugin-specific initialization logic (e.g. credential validation).
 */
@DisplayName("MyPlugin")
class MyPluginTest {

    private MyPlugin plugin;
    private PluginContext ctx;

    @BeforeEach
    void setUp() {
        KtestifyConfig.reset();
        plugin = new MyPlugin();
        ctx = KtestifyConfig::getOrLoad;
    }

    @AfterEach
    void tearDown() {
        KtestifyConfig.reset();
    }

    // =========================================================================
    // Identity / metadata
    // =========================================================================

    @Nested
    @DisplayName("Metadata")
    class MetadataTests {

        @Test
        @DisplayName("getId() returns 'my-plugin'")
        void idIsMyPlugin() {
            // TODO: update expected value to your actual plugin ID.
            assertEquals("my-plugin", plugin.getId());
        }

        @Test
        @DisplayName("getVersion() is non-blank")
        void versionIsNonBlank() {
            assertNotNull(plugin.getVersion());
            assertFalse(plugin.getVersion().isBlank());
        }

        @Test
        @DisplayName("getGluePackage() returns the steps package")
        void gluePackageIsStepsPackage() {
            // TODO: update expected value to your actual steps package.
            assertEquals("io.github.ktestify.myplugin.steps", plugin.getGluePackage());
        }
    }

    // =========================================================================
    // initialize()
    // =========================================================================

    @Nested
    @DisplayName("initialize()")
    class InitializeTests {

        @Test
        @DisplayName("initialize() succeeds when config section is present (no credentials = warn only)")
        void initializeSucceedsWithNoCreds() {
            // reference.conf provides the ktestify.plugins.my-plugin section — no exception expected
            assertDoesNotThrow(() -> plugin.initialize(ctx));
        }

        @Test
        @DisplayName("initialize() succeeds when connection string is set")
        void initializeSucceedsWithConnectionString() {
            // TODO: adapt to your plugin's config key.
            KtestifyConfig cfg = KtestifyConfig.load(
                    ConfigFactory.parseString("ktestify.plugins.my-plugin.connection-string = \"my-conn-string\""));

            assertDoesNotThrow(() -> plugin.initialize(() -> cfg));
        }
    }

    // =========================================================================
    // shutdown()
    // =========================================================================

    @Nested
    @DisplayName("shutdown()")
    class ShutdownTests {

        @Test
        @DisplayName("shutdown() before initialize() does not throw")
        void shutdownBeforeInitDoesNotThrow() {
            assertDoesNotThrow(plugin::shutdown);
        }

        @Test
        @DisplayName("shutdown() after initialize() does not throw")
        void shutdownAfterInitDoesNotThrow() {
            plugin.initialize(ctx);
            assertDoesNotThrow(plugin::shutdown);
        }
    }
}
