/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web.body;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import net.hasor.cobble.StringUtils;
import net.hasor.dataway.model.WebRequest;
import net.hasor.dataway.service.DatawayException;

/** Chooses a reader before acquiring the body stream. */
public final class BodyReaders {
    private static final BodyReader JSON      = new JsonBodyReader();
    private static final BodyReader FORM      = new FormBodyReader();
    private static final BodyReader MULTIPART = new MultipartBodyReader();

    private BodyReaders() {
    }

    public static Map<String, Object> read(WebRequest request) throws IOException {
        String header = request.getHeaders().getOrDefault("content-type", "");
        String type = header.split(";", 2)[0].trim();

        BodyReader reader;
        if (StringUtils.equalsIgnoreCase(type, "application/json")) {
            reader = JSON;
        } else if (StringUtils.equalsIgnoreCase(type, "application/x-www-form-urlencoded")) {
            reader = FORM;
        } else if (StringUtils.equalsIgnoreCase(type, "multipart/form-data")) {
            reader = MULTIPART;
        } else if (type.isEmpty()) {
            if (request.getBody().read() == -1) {
                return new LinkedHashMap<>();
            }
            throw new DatawayException(415, "Content-Type is required for a non-empty body");
        } else {
            throw new DatawayException(415, "Unsupported Content-Type: " + type);
        }

        return reader.read(request, BodyReaders.charset(header, StandardCharsets.UTF_8));
    }

    /** Splits header parameters outside quoted strings, including quoted multipart boundaries. */
    public static Charset charset(String header, Charset fallback) {
        if (header == null) {
            return fallback;
        }

        boolean quoted = false;
        boolean escaped = false;
        int start = 0;
        for (int i = 0; i <= header.length(); i++) {
            char ch = i == header.length() ? ';' : header.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (quoted && ch == '\\') {
                escaped = true;
            } else if (ch == '"') {
                quoted = !quoted;
            } else if (ch == ';' && !quoted) {
                String parameter = header.substring(start, i).trim();
                start = i + 1;
                int equals = parameter.indexOf('=');
                if (equals > 0 && StringUtils.equalsIgnoreCase(parameter.substring(0, equals).trim(), "charset")) {
                    String name = parameter.substring(equals + 1).trim();
                    if (name.startsWith("\"") && name.endsWith("\"") && name.length() >= 2) {
                        name = name.substring(1, name.length() - 1);
                    }
                    try {
                        return Charset.forName(name);
                    } catch (IllegalArgumentException e) {
                        throw new DatawayException(415, "Unsupported body charset: " + name, e);
                    }
                }
            }
        }

        return fallback;
    }
}