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

import lombok.Getter;

/**
 * Success-rate thresholds that drive the visual style in notification card templates.
 *
 * <p>Maps to the {@code ktestify.plugins.notifications.thresholds} HOCON block.
 *
 * <table>
 *   <tr><th>Rate condition</th><th>Style</th><th>Color</th></tr>
 *   <tr><td>&ge; {@code good} (default 75 %)</td><td>{@code "good"}</td><td>green</td></tr>
 *   <tr><td>&ge; {@code warning} (default 50 %)</td><td>{@code "warning"}</td><td>yellow</td></tr>
 *   <tr><td>&lt; {@code warning}</td><td>{@code "attention"}</td><td>red</td></tr>
 * </table>
 *
 * @since 1.0.0
 */
@Getter
public final class ThresholdsConfig {

    private final int good;
    private final int warning;

    ThresholdsConfig(com.typesafe.config.Config cfg) {
        this.good = cfg.getInt("good");
        this.warning = cfg.getInt("warning");
    }

    /**
     * Computes the visual style string for a given success rate.
     *
     * @param successRate integer percentage (0–100)
     * @return one of {@code "good"}, {@code "warning"}, {@code "attention"}
     */
    public String computeStyle(int successRate) {
        if (successRate >= good) return "good";
        if (successRate >= warning) return "warning";
        return "attention";
    }
}
