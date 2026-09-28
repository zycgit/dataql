/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web.body;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.model.WebRequest;
import net.hasor.dataway.service.DatawayException;
import tools.jackson.core.JacksonException;

public class JsonBodyReader implements BodyReader {
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> read(WebRequest request, Charset charset) throws IOException {
        PushbackInputStream input = new PushbackInputStream(request.getBody());
        int first = input.read();
        if (first == -1) {
            return new LinkedHashMap<>();
        }

        input.unread(first);
        Object value;

        try {
            if (StandardCharsets.UTF_8.equals(charset)) {
                value = JsonUtils.readValue(input, Object.class);
            } else {
                value = JsonUtils.readValue(new InputStreamReader(input, charset), Object.class);
            }
        } catch (JacksonException e) {
            throw new DatawayException(400, "Invalid request: " + e.getMessage(), e);
        }

        if (!(value instanceof Map<?, ?> map)) {
            throw new DatawayException(400, "JSON body must be an object");
        } else {
            return (Map<String, Object>) map;
        }
    }
}