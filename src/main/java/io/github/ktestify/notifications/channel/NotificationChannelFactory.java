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

import io.github.ktestify.notifications.channel.impl.LogNotificationChannel;
import io.github.ktestify.notifications.channel.impl.SlackNotificationChannel;
import io.github.ktestify.notifications.channel.impl.TeamsNotificationChannel;
import io.github.ktestify.notifications.channel.impl.WebhookNotificationChannel;
import io.github.ktestify.notifications.config.ChannelConfig;
import io.github.ktestify.notifications.config.NotificationsConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Config-driven factory that builds the list of active {@link NotificationChannel} instances.
 *
 * <h2>Built-in channels</h2>
 *
 * <table>
 *   <tr><th>type</th><th>Implementation</th></tr>
 *   <tr><td>{@code "log"}</td><td>{@link LogNotificationChannel}</td></tr>
 *   <tr><td>{@code "teams"}</td><td>{@link TeamsNotificationChannel}</td></tr>
 *   <tr><td>{@code "slack"}</td><td>{@link SlackNotificationChannel}</td></tr>
 *   <tr><td>{@code "webhook"}</td><td>{@link WebhookNotificationChannel}</td></tr>
 * </table>
 *
 * <h2>Custom channels via SPI</h2>
 *
 * <p>Third-party implementations registered via {@link ServiceLoader} (in
 * {@code META-INF/services/io.github.ktestify.notifications.channel.NotificationChannel}) are discovered at startup
 * and matched to channel config blocks by {@code type}.
 *
 * @since 1.0.0
 */
public final class NotificationChannelFactory {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationChannelFactory.class);

    private NotificationChannelFactory() {}

    /**
     * Builds a list of active {@link NotificationChannel} instances from the given configuration.
     *
     * <p>Only channels with {@code enabled = true} in their config block are instantiated.
     *
     * @param config the root notifications config
     * @return ordered list of active channels,  never {@code null}, may be empty
     */
    public static List<NotificationChannel> build(NotificationsConfig config) {
        // Discover SPI-registered custom channels
        List<NotificationChannel> spiChannels = discoverSpiChannels();

        List<NotificationChannel> active = new ArrayList<>();

        for (ChannelConfig channelCfg : config.getEnabledChannels()) {
            NotificationChannel channel = createChannel(channelCfg, config, spiChannels);
            if (channel != null) {
                active.add(channel);
                LOG.info("Registered channel: {} (type={})", channel.getClass().getSimpleName(), channelCfg.getType());
            }
        }
        return List.copyOf(active);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static NotificationChannel createChannel(
            ChannelConfig cfg, NotificationsConfig globalConfig, List<NotificationChannel> spiChannels) {
        return switch (cfg.getType().toLowerCase()) {
            case "log" -> new LogNotificationChannel(cfg);
            case "teams" -> new TeamsNotificationChannel(cfg, globalConfig);
            case "slack" -> new SlackNotificationChannel(cfg, globalConfig);
            case "webhook" -> new WebhookNotificationChannel(cfg, globalConfig);
            default -> {
                // Try SPI-discovered custom channel
                NotificationChannel custom = spiChannels.stream()
                        .filter(c -> c.getType().equalsIgnoreCase(cfg.getType()))
                        .findFirst()
                        .orElse(null);
                if (custom == null) {
                    LOG.warn("Unknown channel type '{}',  skipping.", cfg.getType());
                }
                yield custom;
            }
        };
    }

    private static List<NotificationChannel> discoverSpiChannels() {
        List<NotificationChannel> discovered = new ArrayList<>();
        try {
            ServiceLoader.load(NotificationChannel.class)
                    .forEach(channel -> {
                        discovered.add(channel);
                        LOG.debug("SPI channel discovered: {} (type={})",
                                channel.getClass().getName(), channel.getType());
                    });
        } catch (Exception e) {
            LOG.debug("SPI channel discovery failed: {}", e.getMessage());
        }
        return discovered;
    }
}

