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
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends suite notifications to any HTTP endpoint via a configurable webhook.
 *
 * <p>Supports custom headers, HTTP method, and a fully user-defined template. This channel can target any webhook
 * consumer: PagerDuty, Datadog, Discord, a custom CI dashboard, etc.
 *
 * <p>Configure in HOCON:
 *
 * <pre>
 * {
 *   type    = "webhook"
 *   enabled = true
 *   url     = ${?KTESTIFY_WEBHOOK_URL}
 *   method  = "POST"
 *   headers {
 *     Content-Type  = "application/json"
 *     Authorization = "Bearer "${?KTESTIFY_WEBHOOK_TOKEN}
 *   }
 *   on-failure-only = false
 *   template        = "builtin"
 * }
 * </pre>
 *
 * @since 1.0.0
 */
public class WebhookNotificationChannel implements NotificationChannel {

    private static final Logger LOG = LoggerFactory.getLogger(WebhookNotificationChannel.class);

    private final ChannelConfig config;
    private final NotificationsConfig globalConfig;
    private final HttpClient httpClient;

    public WebhookNotificationChannel(ChannelConfig config, NotificationsConfig globalConfig) {
        this.config = config;
        this.globalConfig = globalConfig;
        this.httpClient =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public String getType() {
        return "webhook";
    }

    @Override
    public void sendSuite(SuiteEvent event) {
        String url = config.getUrl();
        if (url == null || url.isBlank()) {
            LOG.warn("[webhook] url is not configured,  skipping notification.");
            return;
        }
        try {
            String payload = NotificationTemplateEngine.renderSuite(event, config, globalConfig);

            HttpRequest.Builder requestBuilder =
                    HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(30));

            // Apply custom headers
            Map<String, String> headers = config.getHeaders();
            if (headers.isEmpty()) {
                requestBuilder.header("Content-Type", "application/json; charset=UTF-8");
            } else {
                headers.forEach(requestBuilder::header);
            }

            // Apply HTTP method
            String method = config.getMethod().toUpperCase();
            HttpRequest.BodyPublisher body =
                    HttpRequest.BodyPublishers.ofString(payload.trim(), StandardCharsets.UTF_8);
            requestBuilder.method(method, body);

            HttpResponse<String> response =
                    httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            LOG.info("[webhook] Notification sent to {} (HTTP {}).", url, response.statusCode());

            if (response.statusCode() >= 400) {
                LOG.warn("[webhook] Endpoint responded with error {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            LOG.warn("[webhook] Failed to send notification to {}: {}", url, e.getMessage());
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
