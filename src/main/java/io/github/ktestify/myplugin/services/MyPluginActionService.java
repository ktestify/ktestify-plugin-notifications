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

import io.github.ktestify.myplugin.config.MyPluginConfig;
import io.github.ktestify.myplugin.entities.KtestifyMyEntity;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for sending / uploading data via MyPlugin.
 *
 * <p>Called by the {@code When MyPlugin record is sent from file} Cucumber step.
 *
 * <p>TODO: Implement the actual send/upload logic for your transport.
 *
 * @since 1.0.0
 */
@Slf4j
public class MyPluginActionService {

    private final MyPluginConfig globalConfig;

    /** @param globalConfig global plugin configuration used as credential fallback */
    public MyPluginActionService(MyPluginConfig globalConfig) {
        this.globalConfig = globalConfig;
    }

    /**
     * Sends a local file's content to the target resource.
     *
     * <p>TODO: implement this method using your transport SDK.
     *
     * @param resource the target resource entity
     * @param recordId the destination record identifier (e.g. object key, message ID)
     * @param sourceFile the absolute local path of the file to send
     */
    public void send(KtestifyMyEntity resource, String recordId, String sourceFile) {
        log.info("Sending '{}' → record '{}' in resource '{}'…", sourceFile, recordId, resource.getResourceName());

        // TODO: read the file content and send it via your transport client.
        // Example pattern (adjust for your SDK):
        //
        // String connStr = resolveConnectionString(resource);
        // MyTransportClient client = MyTransportClient.withConnectionString(connStr);
        // try {
        //     byte[] content = Files.readAllBytes(Path.of(sourceFile));
        //     client.send(resource.getResourceName(), recordId, content);
        //     log.info("Send complete — record '{}' ({} bytes).", recordId, content.length);
        // } catch (Exception e) {
        //     throw new RuntimeException(
        //             "Failed to send '" + sourceFile + "' as record '" + recordId + "': " + e.getMessage(), e);
        // }

        throw new UnsupportedOperationException("TODO: implement MyPluginActionService.send()");
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves the effective connection string: resource-level override → global config.
     *
     * <p>TODO: adapt to your transport's auth resolution order.
     */
    private String resolveConnectionString(KtestifyMyEntity resource) {
        String connStr = resource.getConnectionString();
        if (connStr != null && !connStr.isBlank()) return connStr;
        if (globalConfig.hasConnectionString()) return globalConfig.getConnectionString();
        throw new io.github.ktestify.exceptions.PluginException(
                "MyPlugin: no connection string configured for resource '"
                        + resource.getResourceName() + "'. "
                        + "Set KTESTIFY_MYPLUGIN_CONNECTION_STRING or provide it in the step DataTable.");
    }
}
