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
package io.github.ktestify.myplugin.steps;

import io.github.ktestify.config.KtestifyConfig;
import io.github.ktestify.manager.ObjectManager;
import io.github.ktestify.myplugin.config.MyPluginConfig;
import io.github.ktestify.myplugin.entities.KtestifyMyEntity;
import io.github.ktestify.myplugin.services.MyPluginActionService;
import io.github.ktestify.myplugin.services.MyPluginValidationService;

/**
 * PicoContainer-managed shared state for the MyPlugin step definitions.
 *
 * <p>A single instance is created per Cucumber scenario by PicoContainer and injected into every step class that
 * declares it as a constructor parameter. This ensures the resource registry and services share the same lifecycle as
 * the scenario.
 *
 * <h2>Assets directory</h2>
 *
 * <p>{@link #assetsDirectory} is pre-populated from {@code ktestify.framework.directories.assets} in the loaded config.
 * The {@code Given MyPlugin assets directory} step overrides this for a specific scenario.
 *
 * @since 1.0.0
 */
public class SharedMyPluginResources {

    /**
     * Registry for MyPlugin resources, keyed by resource name and/or alias. Populated by
     * {@link MyPluginBackgroundSteps}.
     */
    public final ObjectManager<KtestifyMyEntity> resources = new ObjectManager<>();

    /** Global plugin configuration loaded once per scenario instance. */
    public final MyPluginConfig config;

    /** Action service (send/upload) — shared within the scenario. */
    public final MyPluginActionService actionService;

    /** Validation service — shared within the scenario (single thread pool). */
    public final MyPluginValidationService validationService;

    /**
     * The assets base directory for the current scenario. Pre-populated from
     * {@code ktestify.framework.directories.assets}; may be {@code null} if not configured.
     */
    public String assetsDirectory;

    /** Initialised by PicoContainer at the start of each scenario. */
    public SharedMyPluginResources() {
        KtestifyConfig cfg = KtestifyConfig.getOrLoad();
        this.config = MyPluginConfig.from(cfg.getRaw());
        this.actionService = new MyPluginActionService(config);
        this.validationService = new MyPluginValidationService(config);

        // Pre-populate assets directory from global config
        cfg.getFramework()
                .getAssetsDirectory()
                .filter(path -> !path.isBlank())
                .ifPresent(path -> this.assetsDirectory = path);
    }
}
