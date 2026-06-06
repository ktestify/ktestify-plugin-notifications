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
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.github.ktestify.myplugin.entities.KtestifyMyEntity;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Cucumber {@code @Then} and {@code @And} step definitions for MyPlugin validations.
 *
 * <h2>Available steps</h2>
 *
 * <h3>Content validation against a file</h3>
 *
 * <pre>{@code
 * Then expected MyPlugin record from file
 *   | resourceAlias | recordId           | file          | readTimeout | excludedKeys |
 *   | my-alias      | output/result.json | expected.json | 30          | timestamp,id |
 * }</pre>
 *
 * <h3>Negative assertion — record must NOT appear</h3>
 *
 * <pre>{@code
 * And MyPlugin record should not appear
 *   | resourceAlias | recordId           | readTimeout |
 *   | my-alias      | output/result.json | 10          |
 * }</pre>
 *
 * <h3>Positive assertion — record must appear</h3>
 *
 * <pre>{@code
 * And MyPlugin record should appear
 *   | resourceAlias | recordId           | readTimeout |
 *   | my-alias      | output/result.json | 30          |
 * }</pre>
 *
 * <p>TODO: Rename step wording and DataTable columns to match your transport.
 *
 * @since 1.0.0
 */
@Slf4j
public class MyPluginValidationSteps {

    private final SharedMyPluginResources shared;

    /** PicoContainer constructor injection. */
    public MyPluginValidationSteps(SharedMyPluginResources shared) {
        this.shared = shared;
    }

    // -------------------------------------------------------------------------
    // Step definitions
    // -------------------------------------------------------------------------

    /**
     * Validates record content against a local expected file.
     *
     * @param dataTable DataTable row with columns: {@code resourceAlias}, {@code recordId}, {@code file},
     *     {@code readTimeout} (optional), {@code excludedKeys} (optional)
     */
    @Then("expected MyPlugin record from file")
    public void thenExpectedMyPluginRecordFromFile(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().get(0);
        KtestifyMyEntity resource = resolveResource(row);
        log.info("Validating record '{}' in resource '{}'…", row.get("recordId"), resource.getResourceName());
        shared.validationService.validateFromFile(row, resource, shared.assetsDirectory);
    }

    /**
     * Asserts that a record does <em>not</em> appear within the given timeout (negative assertion).
     *
     * <p>A timeout (record not found) is the expected outcome — the step passes.
     *
     * @param dataTable DataTable row with columns: {@code resourceAlias}, {@code recordId}, {@code readTimeout}
     *     (optional)
     */
    @And("MyPlugin record should not appear")
    public void andMyPluginRecordShouldNotAppear(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().get(0);
        KtestifyMyEntity resource = resolveResource(row);
        log.info(
                "Asserting record '{}' does not appear in resource '{}'…",
                row.get("recordId"),
                resource.getResourceName());
        shared.validationService.validateRecordAbsent(row, resource);
    }

    /**
     * Asserts that a record exists within the given timeout (positive existence check, no content comparison).
     *
     * @param dataTable DataTable row with columns: {@code resourceAlias}, {@code recordId}, {@code readTimeout}
     *     (optional)
     */
    @And("MyPlugin record should appear")
    public void andMyPluginRecordShouldAppear(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().get(0);
        KtestifyMyEntity resource = resolveResource(row);
        log.info("Asserting record '{}' appears in resource '{}'…", row.get("recordId"), resource.getResourceName());
        // No matchMethod → NoOpRecordMatcher (passes if record is found within timeout)
        shared.validationService.validateFromFile(
                // Inject a dummy file value — NoOpRecordMatcher ignores it
                new java.util.HashMap<>(row) {
                    {
                        putIfAbsent("file", "__existence_check__");
                    }
                },
                resource,
                shared.assetsDirectory);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private KtestifyMyEntity resolveResource(Map<String, String> row) {
        String alias = row.get("resourceAlias");
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("DataTable column 'resourceAlias' is required.");
        }
        return shared.resources.getOrThrow(alias);
    }
}
