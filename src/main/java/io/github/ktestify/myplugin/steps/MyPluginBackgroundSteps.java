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
import io.cucumber.java.en.Given;
import io.github.ktestify.myplugin.entities.KtestifyMyEntity;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Cucumber {@code @Given} step definitions for MyPlugin setup.
 *
 * <p>These steps register resources into {@link SharedMyPluginResources} so they can be referenced by alias from action
 * and validation steps.
 *
 * <h2>Example usage</h2>
 *
 * <pre>{@code
 * Background:
 *   Given MyPlugin resource
 *     | resourceName | resourceAlias | connectionString |
 *     | my-resource  | my-alias      | ...              |
 * }</pre>
 *
 * <p>TODO: Rename step wording to match your transport (e.g. "S3 bucket", "IBM MQ queue", "SFTP server").
 *
 * @since 1.0.0
 */
@Slf4j
public class MyPluginBackgroundSteps {

    private final SharedMyPluginResources shared;

    /** PicoContainer constructor injection. */
    public MyPluginBackgroundSteps(SharedMyPluginResources shared) {
        this.shared = shared;
    }

    // -------------------------------------------------------------------------
    // Step definitions
    // -------------------------------------------------------------------------

    /**
     * Registers one MyPlugin resource.
     *
     * <p>DataTable columns:
     *
     * <table>
     *   <tr><th>Column</th><th>Required</th><th>Description</th></tr>
     *   <tr><td>resourceName</td><td>yes</td><td>Physical resource name in the remote system</td></tr>
     *   <tr><td>resourceAlias</td><td>no</td><td>Alias used in subsequent steps</td></tr>
     *   <tr><td>connectionString</td><td>no</td><td>Per-resource connection string (overrides global config)</td></tr>
     * </table>
     *
     * <p>TODO: update step wording and DataTable columns to match your transport.
     *
     * @param dataTable one-row DataTable defining the resource
     */
    @Given("MyPlugin resource")
    public void givenMyPluginResource(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().get(0);

        String resourceName = row.get("resourceName");
        String resourceAlias = row.get("resourceAlias");
        String connectionString = row.get("connectionString");

        if (resourceName == null || resourceName.isBlank()) {
            throw new IllegalArgumentException("DataTable column 'resourceName' is required for MyPlugin.");
        }

        KtestifyMyEntity resource = KtestifyMyEntity.builder()
                .resourceName(resourceName)
                .resourceAlias(resourceAlias)
                .connectionString(connectionString)
                .build();

        shared.resources.register(resourceName, resourceAlias, resource);
        log.info("Registered MyPlugin resource '{}' (alias: '{}').", resourceName, resourceAlias);
    }

    /**
     * Registers multiple MyPlugin resources in a single step.
     *
     * <p>TODO: update step wording to match your transport.
     *
     * @param dataTable multi-row DataTable defining each resource
     */
    @Given("MyPlugin resources")
    public void givenMyPluginResources(DataTable dataTable) {
        for (Map<String, String> row : dataTable.asMaps()) {
            String resourceName = row.get("resourceName");
            String resourceAlias = row.get("resourceAlias");
            String connectionString = row.get("connectionString");

            if (resourceName == null || resourceName.isBlank()) {
                throw new IllegalArgumentException(
                        "DataTable column 'resourceName' is required for each MyPlugin row.");
            }

            KtestifyMyEntity resource = KtestifyMyEntity.builder()
                    .resourceName(resourceName)
                    .resourceAlias(resourceAlias)
                    .connectionString(connectionString)
                    .build();

            shared.resources.register(resourceName, resourceAlias, resource);
            log.info("Registered MyPlugin resource '{}' (alias: '{}').", resourceName, resourceAlias);
        }
    }

    /**
     * Overrides the assets directory for the current scenario.
     *
     * <p>DataTable columns:
     *
     * <table>
     *   <tr><th>Column</th><th>Required</th><th>Description</th></tr>
     *   <tr><td>absolutePath</td><td>yes</td><td>Absolute path to the assets directory</td></tr>
     * </table>
     *
     * @param dataTable one-row DataTable with column {@code absolutePath}
     */
    @Given("MyPlugin assets directory")
    public void givenMyPluginAssetsDirectory(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().get(0);
        String path = row.get("absolutePath");
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException(
                    "DataTable column 'absolutePath' is required for 'Given MyPlugin assets directory'.");
        }
        shared.assetsDirectory = path;
        log.info("MyPlugin assets directory set to '{}'.", path);
    }
}
