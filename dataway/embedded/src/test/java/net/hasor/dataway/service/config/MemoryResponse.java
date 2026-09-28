/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.config;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.model.WebResponse;

/** Captures the actual HTTP representation without a framework or server. */
public class MemoryResponse extends WebResponse {
    private final ByteArrayOutputStream     body    = new ByteArrayOutputStream();
    private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private       IOException               failure;
    private       int                       status;

    @Override
    protected OutputStream openBody(int status) throws IOException {
        if (this.failure != null) {
            throw this.failure;
        }
        this.status = status;
        return this.body;
    }

    @Override
    protected void writeHeader(String name, String value, boolean append) {
        if (!append) {
            this.headers.put(name, new ArrayList<>());
        }
        this.headers.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
    }

    @Override
    public boolean isCommitted() {
        return this.status != 0;
    }

    public void failWith(IOException failure) {
        this.failure = failure;
    }

    public int getStatus() {
        return this.status;
    }

    public Map<String, List<String>> getHeaders() {
        return this.headers;
    }

    public byte[] bytes() {
        return this.body.toByteArray();
    }

    public String text() {
        return this.body.toString(StandardCharsets.UTF_8);
    }

    public Object json() {
        return JsonUtils.readValue(this.text(), Object.class);
    }
}
