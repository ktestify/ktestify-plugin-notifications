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
 * Immutable value object holding CI pipeline metadata auto-detected at runtime.
 *
 * <p>Populated by {@link io.github.ktestify.notifications.service.CiContextResolver} using the
 * {@code cucumber-cienvironment} library, which recognises GitLab CI, GitHub Actions, CircleCI, Jenkins, and others.
 *
 * @since 1.0.0
 * @see io.github.ktestify.notifications.service.CiContextResolver
 */
@Value
@Builder
public class CiContext {

    /** Human-readable name of the detected CI platform (e.g. {@code "GitHub Actions"}, {@code "GitLab CI"}). */
    String ciName;

    /** Direct URL to the current pipeline/build run. May be {@code null} if not available. */
    String pipelineUrl;

    /** Build or pipeline number (e.g. {@code "1234"}). May be {@code null}. */
    String buildNumber;
}

