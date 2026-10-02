/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.config;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.function.WebFile;
import net.hasor.dataway.model.WebRequest;

/** In-process host request with real parsing and resource cleanup. */
public class MemoryRequest extends WebRequest {
    private InputStream body = InputStream.nullInputStream();
    private boolean     closed;
    private int         reads;

    public void json(Object body) {
        this.body = new ByteArrayInputStream(JsonUtils.writeValueAsString(body).getBytes(StandardCharsets.UTF_8));
        this.setHeaders(Map.of("Content-Type", "application/json"));
    }

    @Override
    public InputStream getBody() {
        this.reads++;
        return this.body;
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    public void resource(Closeable resource) {
        this.registerResource(resource);
    }

    public WebFile upload(String content) throws IOException {
        return this.cacheFile("upload.txt", "text/plain", new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
        super.close();
    }

    public boolean isClosed() {
        return this.closed;
    }

    public int getReads() {
        return this.reads;
    }
}
