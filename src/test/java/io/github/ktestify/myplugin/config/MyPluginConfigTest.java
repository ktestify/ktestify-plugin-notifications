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
package io.github.ktestify.myplugin.config;

import static org.junit.jupiter.api.Assertions.*;

import com.typesafe.config.ConfigFactory;
import io.github.ktestify.config.KtestifyConfig;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link MyPluginConfig}.
 *
 * <p>TODO: add tests for your plugin's specific config fields and helper methods.
 */
@DisplayName("MyPluginConfig")
class MyPluginConfigTest {

    @AfterEach
    void tearDown() {
        KtestifyConfig.reset();
    }

    @Test
    @DisplayName("defaults are loaded from reference.conf")
    void defaultsFromReferenceConf() {
        KtestifyConfig cfg = KtestifyConfig.getOrLoad();
        MyPluginConfig pluginCfg = MyPluginConfig.from(cfg.getRaw());

        // Default read-timeout = 30s → 30 000 ms
        assertEquals(30_000L, pluginCfg.getReadTimeoutMs());
        // Default poll-interval = 500ms
        assertEquals(500L, pluginCfg.getPollIntervalMs());
        // Default connection-string is blank
        assertFalse(pluginCfg.hasConnectionString());
    }

    @Test
    @DisplayName("connection string override is recognised")
    void connectionStringOverride() {
        KtestifyConfig cfg = KtestifyConfig.load(ConfigFactory.parseString(
                // TODO: update config key to match your plugin.
                "ktestify.plugins.my-plugin.connection-string = \"Server=localhost;\""));
        MyPluginConfig pluginCfg = MyPluginConfig.from(cfg.getRaw());

        assertTrue(pluginCfg.hasConnectionString());
        assertEquals("Server=localhost;", pluginCfg.getConnectionString());
    }

    @Test
    @DisplayName("read-timeout override is applied")
    void readTimeoutOverride() {
        KtestifyConfig cfg =
                KtestifyConfig.load(ConfigFactory.parseString("ktestify.plugins.my-plugin.read-timeout = 60s"));
        MyPluginConfig pluginCfg = MyPluginConfig.from(cfg.getRaw());

        assertEquals(60_000L, pluginCfg.getReadTimeoutMs());
    }
}
