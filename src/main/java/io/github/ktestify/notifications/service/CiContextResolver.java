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
package io.github.ktestify.notifications.service;

import io.github.ktestify.notifications.model.CiContext;
import io.github.ktestify.notifications.model.GitContext;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects the current CI/CD environment and Git context at runtime.
 *
 * <p>Uses the {@code cucumber-cienvironment} library (available transitively via {@code cucumber-java}) to auto-detect
 * platform-specific environment variables. Supports GitLab CI, GitHub Actions, CircleCI, Jenkins, Azure Pipelines, and
 * others.
 *
 * <p>Detection results are cached as static fields since the CI environment does not change during a JVM run.
 *
 * <p>All methods are null-safe and wrapped in try-catch; missing classes or environment variables never cause a test
 * failure.
 *
 * @since 1.0.0
 */
public final class CiContextResolver {

    private static final Logger LOG = LoggerFactory.getLogger(CiContextResolver.class);

    private static volatile boolean resolved = false;
    private static volatile CiContext cachedCi = null;
    private static volatile GitContext cachedGit = null;
    private static final Object LOCK = new Object();

    private CiContextResolver() {}

    /**
     * Returns the detected CI platform context, or {@link Optional#empty()} if not running in a recognised CI
     * environment or if the {@code cucumber-cienvironment} library is unavailable.
     *
     * @return optional CI context
     */
    public static Optional<CiContext> resolve() {
        ensureResolved();
        return Optional.ofNullable(cachedCi);
    }

    /**
     * Returns the detected Git repository context, or {@link Optional#empty()} if not available.
     *
     * @return optional Git context
     */
    public static Optional<GitContext> resolveGit() {
        ensureResolved();
        return Optional.ofNullable(cachedGit);
    }

    /** Clears cached state. Useful for testing. */
    public static void reset() {
        synchronized (LOCK) {
            resolved = false;
            cachedCi = null;
            cachedGit = null;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static void ensureResolved() {
        if (resolved) return;
        synchronized (LOCK) {
            if (resolved) return;
            detect();
            resolved = true;
        }
    }

    @SuppressWarnings("unchecked")
    private static void detect() {
        try {
            // Uses cucumber-cienvironment (transitive via cucumber-java).
            // Wrapped in try-catch so a missing class never breaks test execution.
            var ciOpt = io.cucumber.cienvironment.DetectCiEnvironment.detectCiEnvironment(System.getenv());

            if (ciOpt.isEmpty()) {
                LOG.debug("No CI environment detected.");
                return;
            }

            var ci = ciOpt.get();

            cachedCi = CiContext.builder()
                    .ciName(ci.getName())
                    .pipelineUrl(ci.getUrl())
                    .buildNumber(ci.getBuildNumber().orElse(null))
                    .build();

            LOG.debug("Detected CI: {},  {}", cachedCi.getCiName(), cachedCi.getPipelineUrl());

            ci.getGit().ifPresent(git -> {
                cachedGit = GitContext.builder()
                        .branch(git.getBranch().orElse(null))
                        .revision(git.getRevision())
                        .tag(git.getTag().orElse(null))
                        .remote(git.getRemote())
                        .build();
                LOG.debug("Detected Git branch: {}", cachedGit.getBranch());
            });

        } catch (NoClassDefFoundError | Exception e) {
            LOG.debug("CI environment detection unavailable: {}", e.getMessage());
        }
    }
}
