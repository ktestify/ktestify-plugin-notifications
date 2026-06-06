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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.List;
import lombok.Getter;

/**
 * Root configuration for the ktestify notifications plugin.
 *
 * <p>Reads the {@code ktestify.plugins.notifications} HOCON subtree. All values can be overridden via environment
 * variables or an {@code application.conf} file.
 *
 * <p>Usage:
 *
 * <pre>
 * NotificationsConfig cfg = NotificationsConfig.from(KtestifyConfig.getOrLoad().getRaw());
 * if (cfg.isEnabled()) { ... }
 * </pre>
 *
 * @since 1.0.0
 */
@Getter
public final class NotificationsConfig {

    static final String CONFIG_PATH = "ktestify.plugins.notifications";

    private final boolean enabled;
    private final boolean onFailureOnly;
    private final SuiteConfig suite;
    private final ThresholdsConfig thresholds;
    private final List<TagGroupConfig> groups;
    private final List<ChannelConfig> channels;

    private NotificationsConfig(Config cfg) {
        this.enabled = cfg.getBoolean("enabled");
        this.onFailureOnly = cfg.getBoolean("on-failure-only");
        this.suite = new SuiteConfig(cfg.getConfig("suite"));
        this.thresholds = new ThresholdsConfig(cfg.getConfig("thresholds"));
        this.groups =
                cfg.getConfigList("groups").stream().map(TagGroupConfig::from).toList();
        this.channels =
                cfg.getConfigList("channels").stream().map(ChannelConfig::from).toList();
    }

    /**
     * Parses the plugin config from the full application {@link Config} object.
     *
     * @param root the root application config (from {@code KtestifyConfig.getRaw()})
     * @return a populated {@code NotificationsConfig}
     */
    public static NotificationsConfig from(Config root) {
        Config merged = root.withFallback(ConfigFactory.load()).resolve();
        return new NotificationsConfig(merged.getConfig(CONFIG_PATH));
    }

    /**
     * Returns only the enabled channels.
     *
     * @return list of {@link ChannelConfig} where {@code enabled == true}
     */
    public List<ChannelConfig> getEnabledChannels() {
        return channels.stream().filter(ChannelConfig::isEnabled).toList();
    }
}
