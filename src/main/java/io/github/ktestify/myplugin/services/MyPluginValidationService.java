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
package io.github.ktestify.myplugin.services;

import static io.github.ktestify.match.RecordMatcherFactory.METHOD_MATCH_FILE;

import io.github.ktestify.exceptions.ConsumerException;
import io.github.ktestify.myplugin.config.MyPluginConfig;
import io.github.ktestify.myplugin.entities.KtestifyMyEntity;
import io.github.ktestify.myplugin.io.MyPluginConsumer;
import io.github.ktestify.myplugin.io.MyPluginConsumerContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates MyPlugin validation for Cucumber step definitions.
 *
 * <p>Mirrors the design of {@code ConsumerValidationService} in {@code ktestify-cucumber}:
 *
 * <ul>
 *   <li>Builds a typed {@link MyPluginConsumerContext} from the DataTable row.
 *   <li>Submits a {@link MyPluginConsumer} to a cached thread pool.
 *   <li>Applies a two-layer timeout (inner: {@link io.github.ktestify.myplugin.io.MyPluginRecordFetcher}; outer:
 *       executor guard with {@code BUFFER_TIME_MS} extra).
 * </ul>
 *
 * <h2>DataTable column conventions</h2>
 *
 * <table>
 *   <tr><th>Column</th><th>Type</th><th>Description</th></tr>
 *   <tr><td>{@code recordId}</td><td>String</td><td>Record identifier within the resource</td></tr>
 *   <tr><td>{@code file}</td><td>String</td><td>Local expected-content file path (resolved against assets dir)</td></tr>
 *   <tr><td>{@code readTimeout}</td><td>int (seconds)</td><td>How long to poll for the record</td></tr>
 *   <tr><td>{@code excludedKeys}</td><td>comma-separated</td><td>JSON keys / XML elements to exclude from comparison</td></tr>
 * </table>
 *
 * @since 1.0.0
 */
@Slf4j
public class MyPluginValidationService {

    /** Extra buffer added on top of the read timeout for the outer executor guard (ms). */
    private static final long BUFFER_TIME_MS = 5_000L;

    private final MyPluginConfig globalConfig;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /** @param globalConfig global plugin config (fallback credentials and timeout defaults) */
    public MyPluginValidationService(MyPluginConfig globalConfig) {
        this.globalConfig = globalConfig;
    }

    // =========================================================================
    // Validation methods — add more as needed (XML, XPath, fields, batch…)
    // =========================================================================

    /**
     * Validates record content against a local expected file.
     *
     * @param row DataTable row ({@code recordId}, {@code file}, {@code readTimeout}, {@code excludedKeys})
     * @param resource the resolved resource entity
     * @param assetsDir optional base directory for relative file paths (may be {@code null})
     */
    public void validateFromFile(Map<String, String> row, KtestifyMyEntity resource, String assetsDir) {
        String recordId = getRequired(row, "recordId");
        String file = resolve(assetsDir, getRequired(row, "file"));
        List<String> excluded = splitComma(getString(row, "excludedKeys"));
        Long readTimeoutMs = getReadTimeoutMs(row);

        MyPluginConsumerContext ctx = MyPluginConsumerContext.builder()
                .resourceName(resource.getResourceName())
                .connectionString(resource.getConnectionString())
                .recordId(recordId)
                .matchMethod(METHOD_MATCH_FILE)
                .matchFilePaths(List.of(file))
                .excludedFields(excluded)
                .readTimeoutMs(readTimeoutMs)
                .build();

        execute(ctx, resource, readTimeoutMs);
    }

    /**
     * Asserts that a record does <em>not</em> appear within the given timeout (negative assertion).
     *
     * <p>A timeout (record not found) is treated as the expected outcome — the step passes.
     *
     * @param row DataTable row ({@code recordId}, {@code readTimeout})
     * @param resource the resolved resource entity
     */
    public void validateRecordAbsent(Map<String, String> row, KtestifyMyEntity resource) {
        String recordId = getRequired(row, "recordId");
        Long readTimeoutMs = getReadTimeoutMs(row);

        // No match method → NoOpRecordMatcher — if fetch succeeds (record appeared), fail
        MyPluginConsumerContext ctx = MyPluginConsumerContext.builder()
                .resourceName(resource.getResourceName())
                .connectionString(resource.getConnectionString())
                .recordId(recordId)
                .readTimeoutMs(readTimeoutMs)
                .build();

        boolean found;
        try {
            found = runWithTimeout(new MyPluginConsumer(ctx, globalConfig), readTimeoutMs);
        } catch (ConsumerException e) {
            // Timeout = record not found = expected
            log.info("Record '{}' not found in resource '{}' as expected.", recordId, resource.getResourceName());
            return;
        }
        if (found) {
            throw new AssertionError("Expected record '" + recordId + "' to be absent in resource '"
                    + resource.getResourceName() + "', but it was found.");
        }
    }

    // TODO: add more validation methods here as your plugin grows:
    // - validateFromXmlFile(...)    → METHOD_MATCH_XML
    // - validateFromXPathFile(...)  → METHOD_MATCH_XPATH
    // - validateFields(...)         → METHOD_FIELDS_TO_MATCH
    // - validateBatch(...)          → batch mode

    // =========================================================================
    // Private execution helpers
    // =========================================================================

    private void execute(MyPluginConsumerContext ctx, KtestifyMyEntity resource, Long readTimeoutMs) {
        boolean passed = runWithTimeout(new MyPluginConsumer(ctx, globalConfig), readTimeoutMs);
        if (!passed) {
            throw new AssertionError("MyPlugin validation failed for record '" + ctx.getRecordId() + "' in resource '"
                    + resource.getResourceName() + "'.");
        }
    }

    private boolean runWithTimeout(java.util.concurrent.Callable<Boolean> consumer, Long readTimeoutMs) {
        long effectiveMs = (readTimeoutMs != null ? readTimeoutMs : globalConfig.getReadTimeoutMs()) + BUFFER_TIME_MS;
        Future<Boolean> future = executor.submit(consumer);
        try {
            return Boolean.TRUE.equals(future.get(effectiveMs, TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new ConsumerException("Outer timeout exceeded after " + effectiveMs + "ms.");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ConsumerException ce) throw ce;
            throw new ConsumerException("MyPlugin consumer execution failed: " + cause.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConsumerException("MyPlugin consumer thread interrupted.");
        }
    }

    // =========================================================================
    // DataTable helpers
    // =========================================================================

    private static String getString(Map<String, String> row, String col) {
        String v = row.get(col);
        return (v != null && !v.isBlank()) ? v : null;
    }

    private static String getRequired(Map<String, String> row, String col) {
        String v = getString(row, col);
        if (v == null) throw new IllegalArgumentException("Required DataTable column '" + col + "' is missing.");
        return v;
    }

    /** Reads {@code readTimeout} (seconds) and converts to milliseconds. Returns {@code null} if absent. */
    private static Long getReadTimeoutMs(Map<String, String> row) {
        String v = getString(row, "readTimeout");
        return v != null ? Long.parseLong(v.trim()) * 1000L : null;
    }

    private static List<String> splitComma(String value) {
        if (value == null || value.isBlank()) return Collections.emptyList();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private static String resolve(String assetsDir, String path) {
        if (assetsDir == null || assetsDir.isBlank() || path == null) return path;
        if (java.nio.file.Path.of(path).isAbsolute()) return path;
        return java.nio.file.Path.of(assetsDir, path).toString();
    }
}
