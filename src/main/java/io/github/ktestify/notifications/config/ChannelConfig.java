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
import com.typesafe.config.ConfigValueType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * Configuration for one outbound notification channel.
 *
 * <p>Maps to a single entry in the {@code ktestify.plugins.notifications.channels} HOCON list.
 *
 * <h2>Template resolution</h2>
 *
 * <p>The {@code template} config key can be:
 *
 * <ul>
 *   <li>A string {@code "builtin"}, use the bundled default template for this channel type.
 *   <li>A string {@code "classpath:..."}, load from a classpath resource.
 *   <li>Any other non-blank string, treat as a filesystem path.
 *   <li>A HOCON object with {@code suite}, {@code group}, {@code footer} keys, inline triple-quoted strings.
 * </ul>
 *
 * @since 1.0.0
 */
@Getter
public final class ChannelConfig {

    /** Channel type identifier. One of {@code "log"}, {@code "teams"}, {@code "slack"}, {@code "webhook"}. */
    private final String type;

    /** Whether this channel is active. Defaults to {@code false}. */
    private final boolean enabled;

    /**
     * Override on-failure-only for this channel. When {@code null}, the global
     * {@code ktestify.plugins.notifications.on-failure-only} value applies.
     */
    private final boolean onFailureOnly;

    /** Webhook URL for Teams or Slack channels. Empty if not applicable. */
    private final String webhookUrl;

    /** Endpoint URL for the {@code "webhook"} type. Empty if not applicable. */
    private final String url;

    /** HTTP method for the {@code "webhook"} type (default: {@code "POST"}). */
    private final String method;

    /** Extra HTTP headers for the {@code "webhook"} type. Empty map if not configured. */
    private final Map<String, String> headers;

    /**
     * Suite-level template source. One of: {@code null}/{@code "builtin"}, a {@code "classpath:"} reference, a
     * filesystem path, or a multiline inline JSON string (from a HOCON triple-quoted block).
     */
    private final String templateSuite;

    /** Group-section template source (same semantics as {@link #templateSuite}). */
    private final String templateGroup;

    /** Footer template source (same semantics as {@link #templateSuite}). */
    private final String templateFooter;

    private ChannelConfig(Config cfg) {
        this.type = cfg.getString("type");
        this.enabled = cfg.hasPath("enabled") && cfg.getBoolean("enabled");
        this.onFailureOnly = cfg.hasPath("on-failure-only") && cfg.getBoolean("on-failure-only");
        this.webhookUrl = cfg.hasPath("webhook-url") ? cfg.getString("webhook-url") : "";
        this.url = cfg.hasPath("url") ? cfg.getString("url") : "";
        this.method = cfg.hasPath("method") ? cfg.getString("method") : "POST";
        this.headers = parseHeaders(cfg);

        // Template resolution: object with keys vs simple string
        if (cfg.hasPath("template")) {
            if (cfg.getValue("template").valueType() == ConfigValueType.OBJECT) {
                Config tpl = cfg.getConfig("template");
                this.templateSuite = tpl.hasPath("suite") ? tpl.getString("suite") : null;
                this.templateGroup = tpl.hasPath("group") ? tpl.getString("group") : null;
                this.templateFooter = tpl.hasPath("footer") ? tpl.getString("footer") : null;
            } else {
                // Simple string applied to all three slots
                String tpl = cfg.getString("template");
                this.templateSuite = tpl;
                this.templateGroup = tpl;
                this.templateFooter = tpl;
            }
        } else {
            this.templateSuite = null;
            this.templateGroup = null;
            this.templateFooter = null;
        }
    }

    /**
     * Parses a {@code ChannelConfig} from a single HOCON config object in the channels list.
     *
     * @param cfg a single channel config block
     * @return the parsed channel config
     */
    public static ChannelConfig from(Config cfg) {
        return new ChannelConfig(cfg);
    }

    private static Map<String, String> parseHeaders(Config cfg) {
        if (!cfg.hasPath("headers")) return Collections.emptyMap();
        Config hdrCfg = cfg.getConfig("headers");
        Map<String, String> map = new HashMap<>();
        hdrCfg.entrySet().forEach(e -> map.put(e.getKey(), hdrCfg.getString(e.getKey())));
        return Collections.unmodifiableMap(map);
    }
}
