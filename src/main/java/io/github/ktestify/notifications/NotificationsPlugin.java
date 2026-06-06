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

import io.github.ktestify.notifications.config.NotificationsConfig;
import io.github.ktestify.plugin.KtestifyPlugin;
import io.github.ktestify.plugin.PluginContext;
import lombok.extern.slf4j.Slf4j;

/**
 * ktestify plugin that adds cross-platform test suite notifications.
 *
 * <p>Registered via {@code META-INF/services/io.github.ktestify.plugin.KtestifyPlugin} so it is discovered
 * automatically by {@link java.util.ServiceLoader}.
 *
 * <h2>Supported channels</h2>
 *
 * <ul>
 *   <li><b>Teams</b>, Adaptive Card sent to a Teams Incoming Webhook
 *   <li><b>Slack</b>, Block Kit payload sent to a Slack Incoming Webhook
 *   <li><b>Webhook</b>, Generic HTTP POST with a user-defined template
 *   <li><b>Log</b>, SLF4J logger (always available, zero external dependencies)
 * </ul>
 *
 * <h2>Key features</h2>
 *
 * <ul>
 *   <li>Tag-based scenario grouping with per-group success rates
 *   <li>CI environment auto-detection (GitLab CI, GitHub Actions, CircleCI, Jenkins…)
 *   <li>Configurable success-rate thresholds that drive visual card styling
 *   <li>Fully user-overridable templates via HOCON {@code """..."""} blocks, classpath resources, or file paths
 *   <li>Extensible via Java SPI, register custom channels in
 *       {@code META-INF/services/io.github.ktestify.notifications.channel.NotificationChannel}
 * </ul>
 *
 * <h2>Minimal HOCON configuration</h2>
 *
 * <pre>
 * ktestify.plugins.notifications {
 *   enabled     = true
 *   suite.name  = "My Test Suite"
 *   channels = [
 *     { type = "teams", enabled = true, webhook-url = ${?KTESTIFY_TEAMS_WEBHOOK_URL} }
 *   ]
 * }
 * </pre>
 *
 * @since 1.0.0
 * @see io.github.ktestify.notifications.hooks.NotificationHooks
 * @see io.github.ktestify.notifications.config.NotificationsConfig
 */
@Slf4j
public class NotificationsPlugin implements KtestifyPlugin {

    private static final String PLUGIN_ID = "notifications";
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
        return "Nil MALHOMME";
    }

    @Override
    public String getAuthorEmail() {
        return "malhomme.nil+oss@icloud.com";
    }

    /**
     * Returns the Cucumber glue package containing {@link io.github.ktestify.notifications.hooks.NotificationHooks}.
     *
     * <p>ktestify-cucumber discovers and registers this package automatically, so the hooks fire on every scenario
     * without any additional configuration.
     *
     * @return the hooks package
     */
    @Override
    public String getGluePackage() {
        return "io.github.ktestify.notifications.hooks";
    }

    /**
     * Validates the notifications configuration at startup and logs the active channel summary.
     *
     * <p>Logs a summary of active channels. When {@code enabled = false} (the default), only an info message is emitted
     * and the plugin performs no further work.
     *
     * @param context the plugin context
     */
    @Override
    public void initialize(PluginContext context) {
        log.info("Initializing {} plugin v{}", getId(), getVersion());

        NotificationsConfig cfg = NotificationsConfig.from(context.getConfig().getRaw());

        if (!cfg.isEnabled()) {
            log.info("[{}] Notifications disabled. Set KTESTIFY_NOTIFICATIONS_ENABLED=true to activate.", getId());
            return;
        }

        long enabledChannels = cfg.getEnabledChannels().size();
        long groupCount = cfg.getGroups().size();

        log.info(
                "[{}] Plugin initialized,  {} channel(s) active, {} tag group(s) configured, on-failure-only={}.",
                getId(),
                enabledChannels,
                groupCount,
                cfg.isOnFailureOnly());

        cfg.getEnabledChannels()
                .forEach(ch -> log.info(
                        "[{}]   • channel: type={}, on-failure-only={}", getId(), ch.getType(), ch.isOnFailureOnly()));
    }

    /**
     * No-op, lifecycle teardown is handled by
     * {@link io.github.ktestify.notifications.hooks.NotificationHooks#afterAllScenarios()}.
     */
    @Override
    public void shutdown() {
        log.debug("[{}] Plugin shut down.", getId());
    }
}
