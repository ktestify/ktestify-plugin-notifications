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
package io.github.ktestify.notifications.model;

import lombok.Builder;
import lombok.Value;

/**
 * Immutable value object holding Git repository metadata auto-detected at runtime.
 *
 * <p>Populated alongside {@link CiContext} by
 * {@link io.github.ktestify.notifications.service.CiContextResolver}.
 *
 * @since 1.0.0
 */
@Value
@Builder
public class GitContext {

    /** Current branch name (e.g. {@code "main"}, {@code "feature/new-consumer"}). May be {@code null}. */
    String branch;

    /** Short commit SHA (e.g. {@code "a1b2c3d"}). May be {@code null}. */
    String revision;

    /** Git tag if the build is on a tag (e.g. {@code "v1.4.0"}). May be {@code null}. */
    String tag;

    /** Remote URL of the repository. May be {@code null}. */
    String remote;
}

