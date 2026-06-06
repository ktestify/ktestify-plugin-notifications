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
package io.github.ktestify.notifications.template;

import static org.junit.jupiter.api.Assertions.*;

import com.typesafe.config.ConfigFactory;
import io.github.ktestify.config.KtestifyConfig;
import io.github.ktestify.notifications.TestFixtures;
import io.github.ktestify.notifications.config.ChannelConfig;
import io.github.ktestify.notifications.config.NotificationsConfig;
import io.github.ktestify.notifications.model.SuiteEvent;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("NotificationTemplateEngine")
class NotificationTemplateEngineTest {

    @Nested
    @DisplayName("substitute()")
    class SubstituteTests {

        @Test
        @DisplayName("replaces a single variable")
        void singleVariable() {
            String result = NotificationTemplateEngine.substitute(
                    "Hello {{name}}!", Map.of("name", "ktestify"));
            assertEquals("Hello ktestify!", result);
        }

        @Test
        @DisplayName("replaces multiple variables")
        void multipleVariables() {
            String result = NotificationTemplateEngine.substitute(
                    "{{a}} + {{b}} = {{c}}", Map.of("a", "1", "b", "2", "c", "3"));
            assertEquals("1 + 2 = 3", result);
        }

        @Test
        @DisplayName("unknown variables are replaced with empty string")
        void unknownVariableBecomesEmpty() {
            String result = NotificationTemplateEngine.substitute(
                    "{{known}} {{unknown}}", Map.of("known", "hello"));
            assertEquals("hello ", result);
        }

        @Test
        @DisplayName("null value in map is treated as empty string")
        void nullValueIsEmpty() {
            Map<String, String> vars = new java.util.HashMap<>();
            vars.put("val", null);
            String result = NotificationTemplateEngine.substitute("x={{val}}", vars);
            assertEquals("x=", result);
        }

        @Test
        @DisplayName("template with no placeholders is returned unchanged")
        void noPlaceholders() {
            String template = "{\"type\": \"message\"}";
            String result = NotificationTemplateEngine.substitute(template, Map.of("unused", "value"));
            assertEquals(template, result);
        }

        @Test
        @DisplayName("numeric variables are substituted correctly")
        void numericVariables() {
            String result = NotificationTemplateEngine.substitute(
                    "passed={{passedCount}}/{{totalCount}}", Map.of("passedCount", "8", "totalCount", "10"));
            assertEquals("passed=8/10", result);
        }
    }

    @Nested
    @DisplayName("BuiltinTemplates.get()")
    class BuiltinTemplateTests {

        @Test
        @DisplayName("teams/suite template is non-blank")
        void teamsSuiteNonBlank() {
            assertFalse(BuiltinTemplates.get("teams", "suite").isBlank());
        }

        @Test
        @DisplayName("teams/group template is non-blank")
        void teamsGroupNonBlank() {
            assertFalse(BuiltinTemplates.get("teams", "group").isBlank());
        }

        @Test
        @DisplayName("slack/suite template is non-blank")
        void slackSuiteNonBlank() {
            assertFalse(BuiltinTemplates.get("slack", "suite").isBlank());
        }

        @Test
        @DisplayName("webhook/suite template is non-blank")
        void webhookSuiteNonBlank() {
            assertFalse(BuiltinTemplates.get("webhook", "suite").isBlank());
        }

        @Test
        @DisplayName("unknown type returns empty string")
        void unknownTypeReturnsEmpty() {
            assertEquals("", BuiltinTemplates.get("unknown", "suite"));
        }

        @Test
        @DisplayName("teams/suite template contains expected variables")
        void teamsSuiteContainsExpectedVars() {
            String template = BuiltinTemplates.get("teams", "suite");
            assertTrue(template.contains("{{suiteName}}"));
            assertTrue(template.contains("{{successRate}}"));
            assertTrue(template.contains("{{groupSections}}"));
            assertTrue(template.contains("{{footer}}"));
        }
    }

    @Nested
    @DisplayName("NotificationTemplateResolver.resolve()")
    class ResolverTests {

        @Test
        @DisplayName("null resolves to builtin")
        void nullResolvesToBuiltin() {
            String builtin = BuiltinTemplates.get("teams", "suite");
            assertEquals(builtin, NotificationTemplateResolver.resolve(null, "teams", "suite"));
        }

        @Test
        @DisplayName("'builtin' string resolves to builtin")
        void builtinStringResolvesToBuiltin() {
            String builtin = BuiltinTemplates.get("slack", "suite");
            assertEquals(builtin, NotificationTemplateResolver.resolve("builtin", "slack", "suite"));
        }

        @Test
        @DisplayName("blank string resolves to builtin")
        void blankResolvesToBuiltin() {
            String builtin = BuiltinTemplates.get("teams", "group");
            assertEquals(builtin, NotificationTemplateResolver.resolve("  ", "teams", "group"));
        }

        @Test
        @DisplayName("inline JSON string (starts with {) is returned as-is")
        void inlineJsonReturnedAsIs() {
            String inline = "{\"type\": \"AdaptiveCard\", \"body\": []}";
            assertEquals(inline, NotificationTemplateResolver.resolve(inline, "teams", "suite"));
        }

        @Test
        @DisplayName("multiline string (contains newline) is returned as-is")
        void multilineReturnedAsIs() {
            String inline = "line1\nline2\nline3";
            assertEquals(inline, NotificationTemplateResolver.resolve(inline, "teams", "suite"));
        }

        @Test
        @DisplayName("non-existent classpath resource falls back to builtin")
        void missingClasspathFallsBackToBuiltin() {
            String builtin = BuiltinTemplates.get("teams", "suite");
            String result = NotificationTemplateResolver.resolve("classpath:non-existent.json", "teams", "suite");
            assertEquals(builtin, result);
        }

        @Test
        @DisplayName("non-existent file path falls back to builtin")
        void missingFileFallsBackToBuiltin() {
            String builtin = BuiltinTemplates.get("teams", "suite");
            String result = NotificationTemplateResolver.resolve("/tmp/does-not-exist-ktestify.json", "teams", "suite");
            assertEquals(builtin, result);
        }
    }

    // =========================================================================
    // renderSuite()
    // =========================================================================

    @Nested
    @DisplayName("renderSuite()")
    class RenderSuiteTests {

        private NotificationsConfig defaultConfig;

        @BeforeEach
        void setUp() {
            KtestifyConfig.reset();
            defaultConfig = NotificationsConfig.from(KtestifyConfig.getOrLoad().getRaw());
        }

        @AfterEach
        void tearDown() {
            KtestifyConfig.reset();
        }

        @Test
        @DisplayName("renders a non-blank result for PASSED suite with Teams channel")
        void teamsPassedSuiteNonBlank() {
            ChannelConfig cfg = ChannelConfig.from(
                    ConfigFactory.parseString("type=\"teams\", enabled=true, webhook-url=\"\""));
            String result = NotificationTemplateEngine.renderSuite(
                    TestFixtures.passedSuite(), cfg, defaultConfig);
            assertNotNull(result);
            assertFalse(result.isBlank());
        }

        @Test
        @DisplayName("renders a non-blank result for FAILED suite with Slack channel")
        void slackFailedSuiteNonBlank() {
            ChannelConfig cfg = ChannelConfig.from(
                    ConfigFactory.parseString("type=\"slack\", enabled=true, webhook-url=\"\""));
            String result = NotificationTemplateEngine.renderSuite(
                    TestFixtures.failedSuite(), cfg, defaultConfig);
            assertFalse(result.isBlank());
        }

        @Test
        @DisplayName("suite name is injected into rendered Teams output")
        void suiteNameInjected() {
            ChannelConfig cfg = ChannelConfig.from(
                    ConfigFactory.parseString("type=\"teams\", enabled=true, webhook-url=\"\""));
            String result = NotificationTemplateEngine.renderSuite(
                    TestFixtures.passedSuite(), cfg, defaultConfig);
            assertTrue(result.contains("Test Suite"), "Expected suite name in output");
        }

        @Test
        @DisplayName("success rate is injected into rendered output")
        void successRateInjected() {
            ChannelConfig cfg = ChannelConfig.from(
                    ConfigFactory.parseString("type=\"teams\", enabled=true, webhook-url=\"\""));
            String result = NotificationTemplateEngine.renderSuite(
                    TestFixtures.passedSuite(), cfg, defaultConfig);
            assertTrue(result.contains("100"), "Expected successRate=100 in output");
        }

        @Test
        @DisplayName("group sections are rendered and injected when groups are present")
        void groupSectionsInjected() {
            ChannelConfig cfg = ChannelConfig.from(
                    ConfigFactory.parseString("type=\"teams\", enabled=true, webhook-url=\"\""));
            String result = NotificationTemplateEngine.renderSuite(
                    TestFixtures.failedSuiteWithGroups(), cfg, defaultConfig);
            // Both group labels should appear in the output
            assertTrue(result.contains("Orders"), "Expected 'Orders' group in output");
            assertTrue(result.contains("Payments"), "Expected 'Payments' group in output");
        }

        @Test
        @DisplayName("CI context variables are injected when present")
        void ciContextInjected() {
            ChannelConfig cfg = ChannelConfig.from(
                    ConfigFactory.parseString("type=\"teams\", enabled=true, webhook-url=\"\""));
            String result = NotificationTemplateEngine.renderSuite(
                    TestFixtures.failedSuiteWithGroups(), cfg, defaultConfig);
            assertTrue(result.contains("GitHub Actions"), "Expected CI name in output");
        }

        @Test
        @DisplayName("git branch is injected when present")
        void gitBranchInjected() {
            ChannelConfig cfg = ChannelConfig.from(
                    ConfigFactory.parseString("type=\"teams\", enabled=true, webhook-url=\"\""));
            String result = NotificationTemplateEngine.renderSuite(
                    TestFixtures.failedSuiteWithGroups(), cfg, defaultConfig);
            assertTrue(result.contains("main"), "Expected git branch in output");
        }

        @Test
        @DisplayName("renders correctly with null CI and Git context (shows N/A)")
        void nullCiAndGitRendersNA() {
            ChannelConfig cfg = ChannelConfig.from(
                    ConfigFactory.parseString("type=\"teams\", enabled=true, webhook-url=\"\""));
            String result = NotificationTemplateEngine.renderSuite(
                    TestFixtures.passedSuite(), cfg, defaultConfig);
            // CI name defaults to N/A when no CI context
            assertTrue(result.contains("N/A"), "Expected N/A for missing CI context");
        }

        @Test
        @DisplayName("renders correctly with Webhook channel")
        void webhookChannelRenders() {
            ChannelConfig cfg = ChannelConfig.from(
                    ConfigFactory.parseString("type=\"webhook\", enabled=true, url=\"\""));
            String result = NotificationTemplateEngine.renderSuite(
                    TestFixtures.failedSuite(), cfg, defaultConfig);
            assertFalse(result.isBlank());
            assertTrue(result.contains("FAILED"));
        }

        @Test
        @DisplayName("inline HOCON template is used when provided in channel config")
        void inlineTemplateUsed() {
            ChannelConfig cfg = ChannelConfig.from(ConfigFactory.parseString("""
                    type = "webhook"
                    enabled = true
                    url = ""
                    template {
                      suite  = "STATUS={{status}} SUITE={{suiteName}}"
                      group  = ""
                      footer = ""
                    }
                    """));
            String result = NotificationTemplateEngine.renderSuite(
                    TestFixtures.passedSuite(), cfg, defaultConfig);
            assertEquals("STATUS=PASSED SUITE=Test Suite", result);
        }
    }

    // =========================================================================
    // Additional substitute() edge cases
    // =========================================================================

    @Nested
    @DisplayName("substitute() edge cases")
    class AdditionalSubstituteTests {

        @Test
        @DisplayName("empty template returns empty string")
        void emptyTemplateReturnsEmpty() {
            assertEquals("", NotificationTemplateEngine.substitute("", Map.of("key", "val")));
        }

        @Test
        @DisplayName("empty vars map leaves known placeholders as empty string")
        void emptyVarsMap() {
            String result = NotificationTemplateEngine.substitute("{{unknown}}", Map.of());
            assertEquals("", result);
        }

        @Test
        @DisplayName("placeholder appearing multiple times is replaced everywhere")
        void multipleOccurrences() {
            String result = NotificationTemplateEngine.substitute(
                    "{{x}}-{{x}}-{{x}}", Map.of("x", "A"));
            assertEquals("A-A-A", result);
        }
    }
}



