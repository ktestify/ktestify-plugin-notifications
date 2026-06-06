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
import io.github.ktestify.notifications.model.NotificationStatus;
import io.github.ktestify.notifications.model.ScenarioEvent;
import io.github.ktestify.notifications.model.SuiteEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Zero-dependency notification channel that writes suite results to the SLF4J logger.
 *
 * <p>Always available; useful during local development and as a debug channel.
 *
 * @since 1.0.0
 */
public class LogNotificationChannel implements NotificationChannel {

    private static final Logger LOG = LoggerFactory.getLogger(LogNotificationChannel.class);

    private final ChannelConfig config;

    public LogNotificationChannel(ChannelConfig config) {
        this.config = config;
    }

    @Override
    public String getType() {
        return "log";
    }

    @Override
    public void sendSuite(SuiteEvent event) {
        String symbol = event.getStatus() == NotificationStatus.FAILED ? "❌" : "✅";
        LOG.info(
                "{} Suite '{}' [{}] finished,  {}/{} passed ({}%)  branch={} build=#{}",
                symbol,
                event.getSuiteName(),
                event.getEnvironment().isBlank() ? "—" : event.getEnvironment(),
                event.getPassedCount(),
                event.getTotalCount(),
                event.getSuccessRate(),
                event.getGitContext() != null ? event.getGitContext().getBranch() : "N/A",
                event.getCiContext() != null ? event.getCiContext().getBuildNumber() : "N/A");

        event.getGroupedResults()
                .forEach(g -> LOG.info(
                        "   {} {},  {}%  ({}/{} passed)",
                        g.getEmoji(), g.getGroupLabel(), g.getSuccessRate(), g.getPassedCount(), g.getTotalCount()));

        if (!event.getReportUrl().isBlank()) {
            LOG.info("   📊 Report: {}", event.getReportUrl());
        }
        if (event.getCiContext() != null
                && event.getCiContext().getPipelineUrl() != null
                && !event.getCiContext().getPipelineUrl().isBlank()) {
            LOG.info("   🔗 Pipeline: {}", event.getCiContext().getPipelineUrl());
        }
    }

    @Override
    public void sendScenario(ScenarioEvent event) {
        // Optional real-time scenario logging,  only at DEBUG to avoid noise
        LOG.debug(
                "[scenario] {} '{}' ({}) in {}ms",
                event.getStatus().name(),
                event.getScenarioName(),
                event.getFeatureName(),
                event.getDurationMs());
    }

    @Override
    public boolean supportsScenario(ScenarioEvent event) {
        return false; // suite-summary only for this channel
    }

    @Override
    public boolean supportsSuite(SuiteEvent event, boolean onFailureOnly) {
        return !config.isOnFailureOnly() || event.getStatus() == NotificationStatus.FAILED;
    }
}
