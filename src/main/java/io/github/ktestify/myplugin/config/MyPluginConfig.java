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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;

/**
 * Typed configuration for the MyPlugin plugin.
 *
 * <p>Reads the {@code ktestify.plugins.my-plugin} HOCON subtree. All values can be overridden via environment variables
 * (see {@code reference.conf} in this module).
 *
 * <h2>Environment variables</h2>
 *
 * <table>
 *   <tr><th>Key</th><th>Env var</th><th>Default</th></tr>
 *   <tr><td>connection-string</td><td>KTESTIFY_MYPLUGIN_CONNECTION_STRING</td><td>(empty)</td></tr>
 *   <tr><td>read-timeout</td><td>KTESTIFY_MYPLUGIN_READ_TIMEOUT</td><td>30s</td></tr>
 *   <tr><td>poll-interval</td><td>—</td><td>500ms</td></tr>
 * </table>
 *
 * <p>TODO: Add your own fields and env var entries matching your transport's authentication/connection model.
 *
 * @since 1.0.0
 */
@Getter
public final class MyPluginConfig {

    // TODO: update to match your plugin ID: ktestify.plugins.<id>
    private static final String CONFIG_PATH = "ktestify.plugins.my-plugin";

    // TODO: replace with your transport-specific credential fields.
    /** Primary connection string. Takes priority over all other auth methods. */
    private final String connectionString;

    /** Maximum time in milliseconds to wait for a record to appear. Defaults to 30 000 ms. */
    private final long readTimeoutMs;

    /** Interval in milliseconds between polls when waiting for a record. Defaults to 500 ms. */
    private final long pollIntervalMs;

    private MyPluginConfig(Config cfg) {
        // TODO: bind your own config keys here.
        this.connectionString = cfg.getString("connection-string");
        this.readTimeoutMs = cfg.getDuration("read-timeout").toMillis();
        this.pollIntervalMs = cfg.getDuration("poll-interval").toMillis();
    }

    /**
     * Parses the plugin config from the full application {@link Config} object.
     *
     * <pre>
     * MyPluginConfig cfg = MyPluginConfig.from(ktestifyConfig.getRaw());
     * </pre>
     *
     * @param root the root application config
     * @return a populated {@code MyPluginConfig}
     */
    public static MyPluginConfig from(Config root) {
        Config merged = root.withFallback(ConfigFactory.load()).resolve();
        return new MyPluginConfig(merged.getConfig(CONFIG_PATH));
    }

    /**
     * Returns {@code true} if a connection string has been configured.
     *
     * @return {@code true} when {@link #connectionString} is non-blank
     */
    public boolean hasConnectionString() {
        return connectionString != null && !connectionString.isBlank();
    }

    // TODO: add more helper methods for your auth/connection modes (e.g. hasApiKey(), hasBearerToken(), etc.)
}
