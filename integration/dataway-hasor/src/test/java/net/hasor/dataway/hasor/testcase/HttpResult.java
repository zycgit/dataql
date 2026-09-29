/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.testcase;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.hasor.dataql.util.JsonUtils;
import okhttp3.Headers;

/** Fully consumes and closes the socket response before returning it to assertions. */
public final class HttpResult {
    public final int     status;
    public final Headers headers;
    public final byte[]  bytes;

    HttpResult(int status, Headers headers, byte[] bytes) {
        this.status = status;
        this.headers = headers;
        this.bytes = bytes;
    }

    public String text() {
        return new String(this.bytes, StandardCharsets.UTF_8);
    }

    public Map<String, Object> json() {
        return JsonUtils.readValue(this.text(), Map.class);
    }
}
