/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.testcase;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import net.hasor.dataql.util.JsonUtils;
import okhttp3.*;

public final class HttpClient implements AutoCloseable {
    private final String       baseUrl;
    private final OkHttpClient client = new OkHttpClient.Builder().cookieJar(new SessionCookies()).followRedirects(false).callTimeout(Duration.ofSeconds(10)).build();

    public HttpClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public HttpResult login(String username) throws IOException {
        return this.send("POST", "/session/login", new FormBody.Builder().add("username", username).add("password", "example-password").build());
    }

    public HttpResult get(String path) throws IOException {
        return this.send("GET", path, null);
    }

    public HttpResult json(String path, Map<String, ?> value) throws IOException {
        return this.send("POST", path, RequestBody.create(JsonUtils.writeValueAsString(value), MediaType.get("application/json")));
    }

    public HttpResult send(String method, String path, RequestBody body, String... headers) throws IOException {
        Request.Builder request = new Request.Builder().url(this.baseUrl + path).method(method, body);
        for (int index = 0; index < headers.length; index += 2) {
            request.addHeader(headers[index], headers[index + 1]);
        }
        try (Response response = this.client.newCall(request.build()).execute()) {
            return new HttpResult(response.code(), response.headers(), response.body().bytes());
        }
    }

    @Override
    public void close() {
        this.client.dispatcher().executorService().shutdownNow();
        this.client.connectionPool().evictAll();
    }
}
