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
package io.github.ktestify.myplugin.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;
import io.github.ktestify.myplugin.entities.KtestifyMyEntity;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Cucumber {@code @When} step definitions for MyPlugin actions (send/upload).
 *
 * <h2>Available steps</h2>
 *
 * <pre>{@code
 * When MyPlugin record is sent from file
 *   | resourceAlias | file              | recordId         |
 *   | my-alias      | payloads/data.json | output/result.json |
 * }</pre>
 *
 * <p>TODO: Rename step wording and DataTable columns to match your transport.
 *
 * @since 1.0.0
 */
@Slf4j
public class MyPluginActionSteps {

    private final SharedMyPluginResources shared;

    /** PicoContainer constructor injection. */
    public MyPluginActionSteps(SharedMyPluginResources shared) {
        this.shared = shared;
    }

    // -------------------------------------------------------------------------
    // Step definitions
    // -------------------------------------------------------------------------

    /**
     * Sends a local file to the target MyPlugin resource.
     *
     * <p>DataTable columns:
     *
     * <table>
     *   <tr><th>Column</th><th>Required</th><th>Description</th></tr>
     *   <tr><td>resourceAlias</td><td>yes</td><td>Alias (or name) of the resource declared in Background</td></tr>
     *   <tr><td>file</td><td>yes</td><td>Local source file path; relative paths resolved against assets dir</td></tr>
     *   <tr><td>recordId</td><td>yes</td><td>Destination record identifier (e.g. object key, message ID)</td></tr>
     * </table>
     *
     * <p>TODO: update step wording and DataTable columns to match your transport.
     *
     * @param dataTable one-row DataTable defining the send operation
     */
    @When("MyPlugin record is sent from file")
    public void whenMyPluginRecordIsSentFromFile(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().get(0);

        String resourceAlias = getRequired(row, "resourceAlias");
        String file = getRequired(row, "file");
        String recordId = getRequired(row, "recordId");

        KtestifyMyEntity resource = shared.resources.getOrThrow(resourceAlias);
        String resolvedFile = resolve(shared.assetsDirectory, file);

        log.info(
                "Sending file '{}' as record '{}' to resource '{}'…",
                resolvedFile,
                recordId,
                resource.getResourceName());

        shared.actionService.send(resource, recordId, resolvedFile);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String getRequired(Map<String, String> row, String col) {
        String v = row.get(col);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Required DataTable column '" + col + "' is missing.");
        }
        return v.trim();
    }

    private static String resolve(String assetsDir, String path) {
        if (assetsDir == null || assetsDir.isBlank() || path == null) return path;
        if (java.nio.file.Path.of(path).isAbsolute()) return path;
        return java.nio.file.Path.of(assetsDir, path).toString();
    }
}
