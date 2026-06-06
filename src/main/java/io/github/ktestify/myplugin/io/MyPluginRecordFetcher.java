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

import io.github.ktestify.exceptions.FetchException;
import io.github.ktestify.io.core.RecordFetcher;
import io.github.ktestify.models.ConsumedRecord;
import io.github.ktestify.myplugin.config.MyPluginConfig;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transport-layer implementation of {@link RecordFetcher} for MyPlugin.
 *
 * <p>Polls for a specific record until it exists (or the read timeout expires). Returns the record's content as a
 * {@link ConsumedRecord}{@code <String>} — the common currency shared with all ktestify matchers.
 *
 * <h2>ConsumedRecord field mapping</h2>
 *
 * <table>
 *   <tr><th>ConsumedRecord field</th><th>MyPlugin source</th></tr>
 *   <tr><td>source</td><td>resource name</td></tr>
 *   <tr><td>partition</td><td>0 (no partitioning concept)</td></tr>
 *   <tr><td>offset</td><td>-1 (no offset concept)</td></tr>
 *   <tr><td>key</td><td>record identifier</td></tr>
 *   <tr><td>value</td><td>record content as UTF-8 String</td></tr>
 *   <tr><td>timestamp</td><td>record creation/modification time</td></tr>
 *   <tr><td>headers</td><td>record metadata (String key-value pairs)</td></tr>
 * </table>
 *
 * <p>TODO: Implement the actual transport logic below. The skeleton uses a polling loop identical to the Azure Blob
 * plugin — adapt it to your transport's client SDK.
 *
 * @since 1.0.0
 * @see MyPluginConsumerContext
 * @see MyPluginConsumer
 */
public class MyPluginRecordFetcher implements RecordFetcher<String> {

    private static final Logger LOG = LoggerFactory.getLogger(MyPluginRecordFetcher.class);

    private final MyPluginConsumerContext context;
    private final MyPluginConfig globalConfig;

    // TODO: add a field for your transport client (e.g. S3Client, JmsSession, etc.)
    // private final MyTransportClient client;

    /**
     * Creates a fetcher backed by the given context and global plugin config.
     *
     * <p>The transport client is built eagerly so credential errors surface at construction time.
     *
     * @param context the per-fetch context
     * @param globalConfig the global plugin config (fallback credentials / timeouts)
     */
    public MyPluginRecordFetcher(MyPluginConsumerContext context, MyPluginConfig globalConfig) {
        this.context = context;
        this.globalConfig = globalConfig;
        // TODO: build your transport client here.
        // this.client = buildClient();
    }

    // -------------------------------------------------------------------------
    // RecordFetcher contract
    // -------------------------------------------------------------------------

    /**
     * Blocks until the configured record exists (or the read timeout expires), then fetches and returns its content.
     *
     * @return a single-element list containing the record as a {@link ConsumedRecord}{@code <String>}
     * @throws FetchException if the record is not found within the timeout, fetching fails, or the thread is
     *     interrupted
     */
    @Override
    public List<ConsumedRecord<String>> fetch() throws FetchException {
        String recordId = context.getRecordId();
        String resourceName = context.getResourceName();
        long deadlineMs = System.currentTimeMillis() + resolveReadTimeoutMs();
        long pollMs = resolvePollIntervalMs();

        LOG.info(
                "Waiting for record '{}' in resource '{}' (timeout={}ms, poll={}ms)…",
                recordId,
                resourceName,
                resolveReadTimeoutMs(),
                pollMs);

        while (System.currentTimeMillis() < deadlineMs) {

            // TODO: check whether the record exists using your transport client.
            // boolean exists = client.exists(resourceName, recordId);
            boolean exists = false; // ← replace this stub

            if (exists) {
                LOG.info("Record '{}' found — fetching content.", recordId);
                return List.of(downloadRecord(resourceName, recordId));
            }

            LOG.debug("Record '{}' not yet present — retrying in {}ms…", recordId, pollMs);
            sleep(pollMs, recordId);
        }

        throw new FetchException(String.format(
                "Timed out after %dms waiting for record '%s' in resource '%s'.",
                resolveReadTimeoutMs(), recordId, resourceName));
    }

    /**
     * Releases transport resources.
     *
     * <p>TODO: close your transport client if it is not managed externally (e.g. close a JMS session). If your SDK
     * manages connection pooling internally (like the Azure SDK), this can be a no-op.
     */
    @Override
    public void close() {
        // TODO: close client if needed.
        LOG.debug("MyPluginRecordFetcher closed.");
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Fetches the record and wraps it in a {@link ConsumedRecord}.
     *
     * <p>TODO: implement this using your transport client.
     */
    private ConsumedRecord<String> downloadRecord(String resourceName, String recordId) {
        try {
            // TODO: download the record content via your transport client.
            // byte[] bytes = client.download(resourceName, recordId);
            // String content = new String(bytes, StandardCharsets.UTF_8);
            // Instant timestamp = client.getLastModified(resourceName, recordId);
            // Map<String, String> headers = client.getMetadata(resourceName, recordId);

            String content = "TODO: replace with actual content fetch"; // ← stub
            Instant timestamp = Instant.now(); // ← stub
            Map<String, String> headers = Collections.emptyMap(); // ← stub

            LOG.debug("Fetched record '{}'.", recordId);
            return new ConsumedRecord<>(resourceName, 0, -1L, recordId, content, timestamp, headers);
        } catch (Exception e) {
            throw new FetchException("Failed to fetch record '" + recordId + "': " + e.getMessage(), e);
        }
    }

    /**
     * Builds the transport client from the resolved credentials.
     *
     * <p>TODO: implement credential resolution (context override → global config) and build your SDK client.
     */
    // private MyTransportClient buildClient() {
    //     String connStr = context.getConnectionString();
    //     if (connStr != null && !connStr.isBlank()) {
    //         return MyTransportClient.withConnectionString(connStr);
    //     }
    //     if (globalConfig.hasConnectionString()) {
    //         return MyTransportClient.withConnectionString(globalConfig.getConnectionString());
    //     }
    //     throw new io.github.ktestify.exceptions.PluginException(
    //             "MyPlugin: no credentials configured. Set KTESTIFY_MYPLUGIN_CONNECTION_STRING.");
    // }

    private long resolveReadTimeoutMs() {
        return context.getReadTimeoutMs() != null ? context.getReadTimeoutMs() : globalConfig.getReadTimeoutMs();
    }

    private long resolvePollIntervalMs() {
        return context.getPollIntervalMs() != null ? context.getPollIntervalMs() : globalConfig.getPollIntervalMs();
    }

    private static void sleep(long ms, String recordId) throws FetchException {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FetchException("Interrupted while waiting for record '" + recordId + "'.");
        }
    }
}
