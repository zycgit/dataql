/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web.support;
import java.io.InputStream;
import com.sun.net.httpserver.HttpExchange;
import net.hasor.dataway.model.WebRequest;

/** Minimal HTTP host; multipart is intentionally left to the framework adapter tests. */
final class ExchangeRequest extends WebRequest {
    private final HttpExchange exchange;

    ExchangeRequest(HttpExchange exchange, String prefix) {
        this.exchange = exchange;
        this.setMethod(exchange.getRequestMethod());
        this.setPath(exchange.getRequestURI().getRawPath());
        this.setPathInfo(this.getPath().substring(prefix.length()));
        this.setQuery(exchange.getRequestURI().getRawQuery());
        this.setHeaderValues(exchange.getRequestHeaders());
    }

    @Override
    public InputStream getBody() {
        return this.exchange.getRequestBody();
    }

    @Override
    public Object getAttribute(String name) {
        return this.exchange.getAttribute(name);
    }
}
