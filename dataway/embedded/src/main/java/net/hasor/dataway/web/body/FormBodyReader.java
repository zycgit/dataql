/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web.body;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.hasor.dataway.model.WebRequest;
import net.hasor.dataway.service.DatawayException;

public class FormBodyReader implements BodyReader {
    @Override
    public Map<String, Object> read(WebRequest request, Charset charset) throws IOException {
        Map<String, Object> form = request.getFormBody();
        if (form != null) {
            return form;
        }

        String text = new String(request.getBody().readAllBytes(), charset);
        return FormBodyReader.parse(text, charset);
    }

    private static Map<String, Object> parse(String text, Charset charset) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (text == null || text.isEmpty()) {
            return values;
        }

        try {
            for (String pair : text.split("&")) {
                if (pair.isEmpty()) {
                    continue;
                }
                String[] parts = pair.split("=", 2);
                String key = URLDecoder.decode(parts[0], charset);
                String value = parts.length > 1 ? URLDecoder.decode(parts[1], charset) : "";
                FormBodyReader.add(values, key, value);
            }
            return values;
        } catch (IllegalArgumentException e) {
            throw new DatawayException(400, "Invalid form encoding", e);
        }
    }

    /** Preserves repeated fields and files under the same name. */
    public static void add(Map<String, Object> values, String name, Object value) {
        Object previous = values.get(name);
        if (previous == null) {
            values.put(name, value);
        } else if (previous instanceof List<?> list) {
            @SuppressWarnings("unchecked") List<Object> items = (List<Object>) list;
            items.add(value);
        } else {
            List<Object> items = new ArrayList<>();
            items.add(previous);
            items.add(value);
            values.put(name, items);
        }
    }

    /** Removes query occurrences from the host parameter map, preserving repeated form values. */
    public static Map<String, Object> fromParameters(Map<String, List<String>> parameters, String query) {
        Map<String, Object> queryValues = FormBodyReader.parse(query, StandardCharsets.UTF_8);
        Map<String, Object> form = new LinkedHashMap<>();
        parameters.forEach((name, entries) -> {
            List<String> values = new ArrayList<>(entries);
            Object queryValue = queryValues.get(name);
            if (queryValue instanceof List<?> list) {
                list.forEach(values::remove);
            } else if (queryValue != null) {
                values.remove(queryValue);
            }
            for (String value : values) {
                FormBodyReader.add(form, name, value);
            }
        });
        return form;
    }
}