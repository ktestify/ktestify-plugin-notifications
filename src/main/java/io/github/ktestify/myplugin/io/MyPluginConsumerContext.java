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
package io.github.ktestify.myplugin.io;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Immutable context object that configures a {@link MyPluginRecordFetcher} for a single fetch operation.
 *
 * <p>Mirrors the design of {@code ConsumerContext} in {@code ktestify-core}: it is a pure value object built by the
 * service layer ({@link io.github.ktestify.myplugin.services.MyPluginValidationService}) and consumed by the transport
 * layer ({@link MyPluginRecordFetcher}).
 *
 * <p>TODO: Add or remove fields to match your transport's addressing / auth model.
 *
 * @since 1.0.0
 * @see MyPluginRecordFetcher
 */
@Value
@Builder
public class MyPluginConsumerContext {

    // ── Resource addressing ──────────────────────────────────────────────────

    /** The logical resource name (e.g. bucket name, queue name, topic name). Must be non-null. */
    String resourceName;

    /**
     * The specific record identifier within the resource (e.g. object key, message ID). TODO: rename / replace with
     * whatever identifies a single record in your transport.
     */
    String recordId;

    // ── Authentication (per-context overrides) ───────────────────────────────

    /**
     * Per-context connection string. When non-blank, overrides the global config. TODO: replace with your transport's
     * auth fields.
     */
    String connectionString;

    // ── Matching ─────────────────────────────────────────────────────────────

    /**
     * Match method — same constants as {@code RecordMatcherFactory.METHOD_MATCH_*} (e.g. {@code "methodMatchFile"},
     * {@code "methodMatchXML"}). May be {@code null} to skip content comparison and only assert record existence.
     */
    String matchMethod;

    /**
     * Ordered list of expected-content file paths used by the matcher. Single-record matchers use
     * {@code matchFilePaths.get(0)}; batch matchers iterate by index. Defaults to an empty list.
     */
    @Builder.Default
    List<String> matchFilePaths = Collections.emptyList();

    /**
     * Field names (or XML element names) to exclude during comparison. Passed directly to the matcher as
     * {@code MatchContext.excludedFields}. Defaults to an empty list.
     */
    @Builder.Default
    List<String> excludedFields = Collections.emptyList();

    // ── Timing ───────────────────────────────────────────────────────────────

    /**
     * Maximum time in milliseconds to poll for the record before throwing a
     * {@link io.github.ktestify.exceptions.FetchException}. When {@code null} the global config value is used.
     */
    Long readTimeoutMs;

    /** Interval in milliseconds between polls. When {@code null} the global config value is used. */
    Long pollIntervalMs;

    // ── Convenience accessor ─────────────────────────────────────────────────

    /**
     * Returns the first match file path, or {@code null} if the list is empty. Mirrors the pattern of
     * {@code MatchContext.getMatchFilePath()} in {@code ktestify-core}.
     *
     * @return the first element of {@link #matchFilePaths}, or {@code null}
     */
    public String getMatchFilePath() {
        return matchFilePaths != null && !matchFilePaths.isEmpty() ? matchFilePaths.get(0) : null;
    }
}
