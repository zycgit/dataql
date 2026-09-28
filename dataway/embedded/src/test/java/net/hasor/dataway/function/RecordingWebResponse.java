/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.function;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.hasor.dataway.model.WebResponse;

/** Records host header operations while retaining the real buffering and validation. */
class RecordingWebResponse extends WebResponse {
    private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private       boolean                   committed;

    @Override
    protected OutputStream openBody(int status) {
        this.committed = true;
        return OutputStream.nullOutputStream();
    }

    @Override
    protected void writeHeader(String name, String value, boolean append) {
        if (append) {
            this.headers.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
        } else {
            this.headers.put(name, new ArrayList<>(List.of(value)));
        }
    }

    @Override
    public boolean isCommitted() {
        return this.committed;
    }

    void commit() {
        this.committed = true;
    }

    Map<String, List<String>> getHeaders() {
        return this.headers;
    }
}
