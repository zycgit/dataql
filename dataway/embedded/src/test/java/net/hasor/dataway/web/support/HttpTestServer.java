/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web.support;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.WebHandler;
import net.hasor.dataway.service.config.MemoryResponse;

/** Real loopback HTTP transport with host-owned exception-to-status translation. */
public final class HttpTestServer implements AutoCloseable {
    private final HttpServer server;
    private final HttpClient client;

    public HttpTestServer(String prefix, WebHandler handler) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.createContext(prefix, exchange -> this.handle(exchange, prefix, handler));
        this.server.start();
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    private void handle(HttpExchange exchange, String prefix, WebHandler handler) throws IOException {
        try (exchange) {
            MemoryResponse response = new MemoryResponse();
            int status;
            byte[] bytes;
            try {
                handler.handle(new ExchangeRequest(exchange, prefix), response);
                status = response.getStatus();
                bytes = response.bytes();
                exchange.getResponseHeaders().putAll(response.getHeaders());
            } catch (Exception error) {
                status = error instanceof DatawayException failure ? failure.status() : 500;
                bytes = JsonUtils.writeValueAsString(Map.of("status", status, "message", error.getMessage())).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            }
            boolean empty = "HEAD".equals(exchange.getRequestMethod()) || bytes.length == 0;
            exchange.sendResponseHeaders(status, empty ? -1 : bytes.length);
            if (!empty) {
                exchange.getResponseBody().write(bytes);
            }
        }
    }

    public HttpResponse<String> send(String method, String path, String contentType, byte[] body, String... headers) throws IOException, InterruptedException {
        URI uri = URI.create("http://127.0.0.1:" + this.server.getAddress().getPort() + path);
        HttpRequest.Builder request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10));
        if (headers.length > 0) {
            request.headers(headers);
        }
        if (contentType != null) {
            request.header("Content-Type", contentType);
        }
        request.method(method, HttpRequest.BodyPublishers.ofByteArray(body));
        return this.client.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    @Override
    public void close() {
        this.server.stop(0);
    }
}
