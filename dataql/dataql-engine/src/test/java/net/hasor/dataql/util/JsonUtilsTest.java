/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.util;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import tools.jackson.core.JacksonException;
import static org.junit.Assert.*;

public class JsonUtilsTest {
    @Test
    public void writingOmitsNullMapEntriesAndPreservesNullArrayElements() {
        Map<?, ?> data = JsonUtils.readValue("""
                {"id":1,"parent_id":null,"children":[{"id":2,"parent_id":1,"comment":null},null],"empty":[]}
                """, Map.class);
        var expected = JsonUtils.readTree("""
                {"id":1,"children":[{"id":2,"parent_id":1},null],"empty":[]}
                """);

        assertEquals(expected, JsonUtils.readTree(JsonUtils.writeValueAsString(data)));
        assertEquals(expected, JsonUtils.readTree(JsonUtils.writeValueAsPrettyString(data)));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        JsonUtils.writeValue(output, data);
        assertEquals(expected, JsonUtils.readTree(output.toString(StandardCharsets.UTF_8)));

        assertTrue(data.containsKey("parent_id"));
        assertNull(data.get("parent_id"));
        List<?> children = (List<?>) data.get("children");
        Map<?, ?> child = (Map<?, ?>) children.get(0);
        assertTrue(child.containsKey("comment"));
        assertNull(child.get("comment"));
        assertNull(children.get(1));
    }

    @Test
    public void writingPreservesRootNull() {
        assertEquals("null", JsonUtils.writeValueAsString(null));
        assertEquals("null", JsonUtils.writeValueAsPrettyString(null));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        JsonUtils.writeValue(output, null);
        assertEquals("null", output.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void parsingDoesNotCloseTheCallerStreamOnSuccessOrFailure() throws Exception {
        Path file = Files.createTempFile("dataql-json-read", ".json");
        try {
            for (String content : List.of("{\"value\":1}", "{broken")) {
                Files.writeString(file, content);
                try (InputStream input = Files.newInputStream(file)) {
                    if (content.equals("{broken")) {
                        assertThrows(JacksonException.class, () -> JsonUtils.readValue(input, Map.class));
                    } else {
                        assertEquals(Map.of("value", 1), JsonUtils.readValue(input, Map.class));
                    }
                    assertTrue(input.available() >= 0);
                }
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void writingLeavesTheCallerStreamOpen() throws Exception {
        Path file = Files.createTempFile("dataql-json-write", ".json");
        try {
            try (OutputStream output = Files.newOutputStream(file)) {
                JsonUtils.writeValue(output, Map.of("value", "雪"));
                output.write('\n');
            }
            assertEquals("{\"value\":\"雪\"}\n", Files.readString(file));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
