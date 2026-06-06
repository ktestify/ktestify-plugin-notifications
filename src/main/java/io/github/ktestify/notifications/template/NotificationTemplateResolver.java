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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves a notification template string from one of four sources (highest to lowest priority):
 *
 * <ol>
 *   <li><b>Inline HOCON string</b>,  a multiline string from a HOCON {@code """..."""} block (contains newlines or
 *       starts with {@code {}).
 *   <li><b>Classpath resource</b>,  prefixed with {@code "classpath:"} (e.g. {@code "classpath:my-card.json"}).
 *   <li><b>Filesystem path</b>,  an absolute or relative path to a template file.
 *   <li><b>Bundled default</b>,  returned by {@link BuiltinTemplates#get(String, String)} when {@code null},
 *       blank, or {@code "builtin"} is configured.
 * </ol>
 *
 * @since 1.0.0
 */
public final class NotificationTemplateResolver {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationTemplateResolver.class);

    private NotificationTemplateResolver() {}

    /**
     * Resolves the template content for a given channel and slot.
     *
     * @param configured  the raw value from {@link io.github.ktestify.notifications.config.ChannelConfig}
     *                    (may be {@code null}, {@code "builtin"}, a classpath ref, a file path, or inline content)
     * @param channelType the channel type identifier (e.g. {@code "teams"}, {@code "slack"})
     * @param slot        the template slot ({@code "suite"}, {@code "group"}, {@code "footer"})
     * @return the resolved template string,  never {@code null}
     */
    public static String resolve(String configured, String channelType, String slot) {
        // 1. Null / blank / "builtin" → bundled default
        if (configured == null || configured.isBlank() || "builtin".equalsIgnoreCase(configured.trim())) {
            return BuiltinTemplates.get(channelType, slot);
        }

        // 2. Classpath resource
        if (configured.startsWith("classpath:")) {
            String resource = configured.substring("classpath:".length()).trim();
            return loadClasspath(resource, channelType, slot);
        }

        // 3. Inline content — multiline string (from HOCON """), JSON literal, or template with {{variables}}
        if (configured.contains("\n") || configured.trim().startsWith("{")
                || configured.trim().startsWith("[") || configured.contains("{{")) {
            return configured;
        }

        // 4. Filesystem path
        return loadFile(configured, channelType, slot);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static String loadClasspath(String resource, String channelType, String slot) {
        try (InputStream is = NotificationTemplateResolver.class.getClassLoader().getResourceAsStream(resource)) {
            if (is == null) {
                LOG.warn("Classpath resource '{}' not found,  falling back to builtin template.", resource);
                return BuiltinTemplates.get(channelType, slot);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Failed to read classpath resource '{}': {},  falling back to builtin.", resource, e.getMessage());
            return BuiltinTemplates.get(channelType, slot);
        }
    }

    private static String loadFile(String path, String channelType, String slot) {
        try {
            return Files.readString(Paths.get(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Failed to read template file '{}': {},  falling back to builtin.", path, e.getMessage());
            return BuiltinTemplates.get(channelType, slot);
        }
    }
}


