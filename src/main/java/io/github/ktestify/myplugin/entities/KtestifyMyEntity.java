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
package io.github.ktestify.myplugin.entities;

import lombok.Builder;
import lombok.Value;

/**
 * Immutable entity representing a MyPlugin resource registered in a Cucumber scenario.
 *
 * <p>Created by the {@code Given MyPlugin resource} step and stored in
 * {@link io.github.ktestify.myplugin.steps.SharedMyPluginResources} keyed by name and/or alias.
 *
 * <h2>Example</h2>
 *
 * <pre>
 * Given MyPlugin resource
 *   | resourceName | resourceAlias | connectionString   |
 *   | my-resource  | my-alias      | ...                |
 * </pre>
 *
 * <p>TODO: Replace this entity with one that models your transport's resource (e.g. bucket, queue, endpoint). Add or
 * remove fields to match your transport's configuration model.
 *
 * @since 1.0.0
 */
@Value
@Builder
public class KtestifyMyEntity {

    /** The physical resource name (as it appears in the remote system). Must be non-null. */
    String resourceName;

    /**
     * Optional alias used to reference this resource from other steps. If blank, only {@code resourceName} can be used
     * as the lookup key.
     */
    String resourceAlias;

    /**
     * Optional per-resource connection string. When {@code null} or blank the global
     * {@code ktestify.plugins.my-plugin.connection-string} is used.
     *
     * <p>TODO: Replace with your transport's per-resource auth fields.
     */
    String connectionString;
}
