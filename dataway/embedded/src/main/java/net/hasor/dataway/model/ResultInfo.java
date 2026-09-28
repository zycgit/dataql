/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.model;
import java.util.Map;
import java.util.TreeMap;

/** Mutable response data containing HTTP status, headers, content and encoding mode. */
public class ResultInfo {
    private int                 status  = 200;
    private Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private Object              data;
    private boolean             json    = true;

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Object getData() {
        return this.data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public boolean isJson() {
        return this.json;
    }

    public void setJson(boolean json) {
        this.json = json;
    }
}
