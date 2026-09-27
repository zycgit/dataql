/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.util;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import tools.jackson.core.JacksonException;
import static org.junit.Assert.*;

public class JsonUtilsTest {
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
