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

import io.github.ktestify.myplugin.config.MyPluginConfig;
import io.github.ktestify.plugin.KtestifyPlugin;
import io.github.ktestify.plugin.PluginContext;
import lombok.extern.slf4j.Slf4j;

/**
 * ktestify plugin for MyPlugin transport.
 *
 * <p>Registered via {@code META-INF/services/io.github.ktestify.plugin.KtestifyPlugin} so it is discovered
 * automatically by {@link java.util.ServiceLoader}.
 *
 * <h2>Lifecycle</h2>
 *
 * <ol>
 *   <li>{@link #initialize(PluginContext)} — validates config at startup; logs a warning if no credentials are found.
 *   <li>Cucumber scenarios run — step definitions are discovered via {@link #getGluePackage()}.
 *   <li>{@link #shutdown()} — releases shared resources (if any).
 * </ol>
 *
 * @since 1.0.0
 * @see MyPluginConfig
 */
@Slf4j
public class MyPlugin implements KtestifyPlugin {

    // TODO: change this to your plugin's stable kebab-case identifier.
    // This must match the HOCON key: ktestify.plugins.<id>
    private static final String PLUGIN_ID = "my-plugin";

    // TODO: keep this in sync with your pom.xml <version>.
    private static final String VERSION = "1.0-SNAPSHOT";

    @Override
    public String getId() {
        return PLUGIN_ID;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getAuthorName() {
        // TODO: replace with your name.
        return "Your Name";
    }

    @Override
    public String getAuthorEmail() {
        // TODO: replace with your email.
        return "your.email@example.com";
    }

    /**
     * Returns the Cucumber glue package for this plugin's step definitions.
     *
     * <p>The ktestify runtime injects this as a {@code --glue} argument so steps are discovered automatically.
     *
     * @return the steps package
     */
    @Override
    public String getGluePackage() {
        // TODO: keep in sync with your actual steps package name.
        return "io.github.ktestify.myplugin.steps";
    }

    /**
     * Validates configuration at startup.
     *
     * <p>Logs a warning if no credentials are configured — the plugin still loads because credentials may be provided
     * per-scenario via the DataTable.
     *
     * @param context the plugin context providing access to the loaded {@link io.github.ktestify.config.KtestifyConfig}
     */
    @Override
    public void initialize(PluginContext context) {
        log.info("Initializing {} plugin v{}…", getId(), getVersion());

        // TODO: load your plugin config and validate required settings.
        // Example:
        MyPluginConfig cfg = MyPluginConfig.from(context.getConfig().getRaw());

        // TODO: implement credential validation appropriate for your transport.
        // Example: warn if connection string is blank (per-scenario override is still possible).
        if (!cfg.hasConnectionString()) {
            log.warn(
                    "[{}] No connection string configured. "
                            + "Set KTESTIFY_MYPLUGIN_CONNECTION_STRING or provide it per-step via DataTable.",
                    getId());
        }

        log.info("{} plugin initialized successfully.", getId());
    }

    /** No-op — this plugin has no long-lived shared resources. Override if needed. */
    @Override
    public void shutdown() {
        log.debug("{} plugin shut down.", getId());
    }
}
