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
import lombok.Getter;

/**
 * Configuration for one tag-based scenario group.
 *
 * <p>Maps to entries in the {@code ktestify.plugins.notifications.groups} HOCON list.
 *
 * <p>Example HOCON:
 *
 * <pre>
 * groups = [
 *   { tag = "orders",   label = "Order Service",   emoji = "📦" },
 *   { tag = "payments", label = "Payment Service", emoji = "💳" }
 * ]
 * </pre>
 *
 * @since 1.0.0
 */
@Getter
public final class TagGroupConfig {

    /** Cucumber tag to match (without the {@code @} prefix). */
    private final String tag;

    /** Display label used in the notification card. */
    private final String label;

    /** Emoji prefix for visual identification. Defaults to {@code "🏷️"} if not set. */
    private final String emoji;

    private TagGroupConfig(Config cfg) {
        this.tag = cfg.getString("tag");
        this.label = cfg.hasPath("label") ? cfg.getString("label") : tag;
        this.emoji = cfg.hasPath("emoji") ? cfg.getString("emoji") : "🏷️";
    }

    /**
     * Parses a {@code TagGroupConfig} from a single HOCON config object in the groups list.
     *
     * @param cfg a single group config block
     * @return the parsed group config
     */
    public static TagGroupConfig from(Config cfg) {
        return new TagGroupConfig(cfg);
    }
}

