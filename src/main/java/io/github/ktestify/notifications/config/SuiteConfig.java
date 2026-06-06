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
 * Configuration for the suite-level metadata displayed in notifications.
 *
 * <p>Maps to the {@code ktestify.plugins.notifications.suite} HOCON block.
 *
 * @since 1.0.0
 */
@Getter
public final class SuiteConfig {

    private final String name;
    private final String environment;
    private final String reportUrl;

    SuiteConfig(com.typesafe.config.Config cfg) {
        this.name = cfg.getString("name");
        this.environment = cfg.getString("environment");
        this.reportUrl = cfg.getString("report-url");
    }
}

