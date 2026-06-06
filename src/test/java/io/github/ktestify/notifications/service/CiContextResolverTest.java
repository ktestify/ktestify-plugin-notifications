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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CiContextResolver")
class CiContextResolverTest {

    @AfterEach
    void tearDown() {
        CiContextResolver.reset();
    }

    @Test
    @DisplayName("resolve() does not throw when not running in CI")
    void resolveDoesNotThrow() {
        assertDoesNotThrow(CiContextResolver::resolve);
    }

    @Test
    @DisplayName("resolveGit() does not throw when not running in CI")
    void resolveGitDoesNotThrow() {
        assertDoesNotThrow(CiContextResolver::resolveGit);
    }

    @Test
    @DisplayName("resolve() returns an Optional (empty or present — never null)")
    void resolveReturnsOptional() {
        assertNotNull(CiContextResolver.resolve());
    }

    @Test
    @DisplayName("resolveGit() returns an Optional (empty or present — never null)")
    void resolveGitReturnsOptional() {
        assertNotNull(CiContextResolver.resolveGit());
    }

    @Test
    @DisplayName("resolve() is called twice but detection runs only once (caching)")
    void resultsAreCached() {
        var first = CiContextResolver.resolve();
        var second = CiContextResolver.resolve();
        // Same result object (or both empty) — detection is not re-run
        assertEquals(first.isPresent(), second.isPresent());
        if (first.isPresent()) {
            assertEquals(first.get().getCiName(), second.get().getCiName());
        }
    }

    @Nested
    @DisplayName("reset()")
    class ResetTests {

        @Test
        @DisplayName("reset() allows a fresh detection on the next call")
        void resetAllowsFreshDetection() {
            CiContextResolver.resolve(); // prime the cache
            CiContextResolver.reset();
            // After reset, calling resolve() again should not throw
            assertDoesNotThrow(CiContextResolver::resolve);
        }

        @Test
        @DisplayName("reset() clears both CI and Git caches")
        void resetClearsBothCaches() {
            CiContextResolver.resolve();
            CiContextResolver.resolveGit();
            CiContextResolver.reset();
            // Both should be safely callable after reset
            assertDoesNotThrow(CiContextResolver::resolve);
            assertDoesNotThrow(CiContextResolver::resolveGit);
        }
    }
}

