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

/**
 * Represents the status of a Cucumber scenario or test suite run.
 *
 * <p>Used in both {@link ScenarioEvent} (per-scenario) and {@link SuiteEvent} (aggregated suite result).
 *
 * @since 1.0.0
 */
public enum NotificationStatus {

    /** All assertions passed. */
    PASSED,

    /** One or more assertions failed or an unexpected exception was thrown. */
    FAILED,

    /** Scenario was skipped (step marked {@code @Pending}, or step undefined). */
    SKIPPED
}

