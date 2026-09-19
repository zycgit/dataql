/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import net.hasor.dataway.web.WebResponse;

public class SpringWebResponse extends WebResponse {
    private final HttpServletResponse response;

    public SpringWebResponse(HttpServletResponse response) {
        this.response = response;
    }

    @Override
    protected OutputStream openBody(int status, Map<String, String> headers) throws IOException {
        this.response.setStatus(status);
        headers.forEach(this.response::setHeader);
        return this.response.getOutputStream();
    }

    @Override
    public boolean isCommitted() {
        return this.response.isCommitted();
    }
}
