/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;
import java.io.IOException;
import java.io.InputStream;
import jakarta.servlet.http.HttpServletRequest;
import net.hasor.dataway.model.WebRequest;

/** Delegates body access to the request passed through the host's MVC chain. */
public class SpringWebRequest extends WebRequest {
    private final HttpServletRequest request;

    public SpringWebRequest(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public InputStream getBody() throws IOException {
        return this.request.getInputStream();
    }

    @Override
    public Object getAttribute(String name) {
        return this.request.getAttribute(name);
    }
}
