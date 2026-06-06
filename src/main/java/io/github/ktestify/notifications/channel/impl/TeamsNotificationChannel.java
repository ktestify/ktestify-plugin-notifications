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
package io.github.ktestify.notifications.channel.impl;

import io.github.ktestify.notifications.channel.NotificationChannel;
import io.github.ktestify.notifications.config.ChannelConfig;
import io.github.ktestify.notifications.config.NotificationsConfig;
import io.github.ktestify.notifications.model.NotificationStatus;
import io.github.ktestify.notifications.model.ScenarioEvent;
import io.github.ktestify.notifications.model.SuiteEvent;
import io.github.ktestify.notifications.template.NotificationTemplateEngine;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends suite notifications to a Microsoft Teams channel via an Incoming Webhook.
 *
 * <p>The rendered Adaptive Card is wrapped in the Teams message envelope format and POSTed to the configured
 * {@code webhook-url}.
 *
 * <p>Configure in HOCON:
 *
 * <pre>
 * {
 *   type        = "teams"
 *   enabled     = true
 *   webhook-url = ${?KTESTIFY_TEAMS_WEBHOOK_URL}
 *   template    = "builtin"   # or a HOCON triple-quoted object / classpath ref / file path
 * }
 * </pre>
 *
 * @since 1.0.0
 */
public class TeamsNotificationChannel implements NotificationChannel {

    private static final Logger LOG = LoggerFactory.getLogger(TeamsNotificationChannel.class);

    private final ChannelConfig config;
    private final NotificationsConfig globalConfig;
    private final HttpClient httpClient;

    public TeamsNotificationChannel(ChannelConfig config, NotificationsConfig globalConfig) {
        this.config = config;
        this.globalConfig = globalConfig;
        this.httpClient =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public String getType() {
        return "teams";
    }

    @Override
    public void sendSuite(SuiteEvent event) {
        String webhookUrl = config.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            LOG.warn("[teams] webhook-url is not configured,  skipping notification.");
            return;
        }
        try {
            String cardContent = NotificationTemplateEngine.renderSuite(event, config, globalConfig);
            // Wrap in Teams message envelope
            String payload = """
                    {"type":"message","attachments":[{"contentType":"application/vnd.microsoft.card.adaptive","content":%s}]}
                    """.formatted(cardContent.trim());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.info("[teams] Notification sent (HTTP {}).", response.statusCode());

            if (response.statusCode() >= 400) {
                LOG.warn("[teams] Webhook responded with error {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            LOG.warn("[teams] Failed to send notification: {}", e.getMessage());
        }
    }

    @Override
    public boolean supportsSuite(SuiteEvent event, boolean onFailureOnly) {
        boolean channelFailureOnly = config.isOnFailureOnly();
        return !channelFailureOnly || event.getStatus() == NotificationStatus.FAILED;
    }

    @Override
    public boolean supportsScenario(ScenarioEvent event) {
        return false;
    }
}
