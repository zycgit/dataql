/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.model.WebRequest;

/** In-memory HTTP input for storage integration checks. */
public class TestWebRequest extends WebRequest {
    private final byte[] body;

    public TestWebRequest(String method, String path, Map<String, ?> body) {
        this.setMethod(method);
        this.setPath(path);
        this.setPathInfo(path);
        this.setHeaders(Map.of("Content-Type", "application/json"));
        this.body = JsonUtils.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public InputStream getBody() {
        return new ByteArrayInputStream(this.body);
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }
}
