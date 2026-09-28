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
import java.io.Reader;
import java.util.Map;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

/** Shared JSON operations. Callers retain ownership of their input and output streams. */
public final class JsonUtils {
    private static final JsonMapper   JSON            = JsonMapper.builder() //
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)//
            .addMixIn(Map.class, JsonMapFilter.class)             //
            .filterProvider(new SimpleFilterProvider().addFilter(JsonMapFilter.NAME, new JsonMapFilter()))//
            .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)       //
            .disable(StreamWriteFeature.AUTO_CLOSE_TARGET)      //
            .build();
    private static final ObjectWriter JSON_WITH_NULLS = JSON.writer() //
            .with(new SimpleFilterProvider().addFilter(JsonMapFilter.NAME, SimpleBeanPropertyFilter.serializeAll()));

    private JsonUtils() {
    }

    public static <T> T readValue(String content, Class<T> type) {
        return JSON.readValue(content, type);
    }

    public static <T> T readValue(InputStream input, Class<T> type) {
        return JSON.readValue(input, type);
    }

    public static <T> T readValue(Reader input, Class<T> type) {
        return JSON.readValue(input, type);
    }

    public static JsonNode readTree(String content) {
        return JSON.readTree(content);
    }

    /** Converts JSON-compatible values without serializing them to text. */
    public static <T> T convertValue(Object value, Class<T> type) {
        return JSON.convertValue(value, type);
    }

    public static String writeValueAsString(Object value) {
        return JSON.writeValueAsString(value);
    }

    /** Preserves explicit null map entries when serializing configuration documents. */
    public static String writeValueAsStringWithNulls(Object value) {
        return JSON_WITH_NULLS.writeValueAsString(value);
    }

    public static String writeValueAsPrettyString(Object value) {
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    }

    public static void writeValue(OutputStream output, Object value) {
        JSON.writeValue(output, value);
    }
}
