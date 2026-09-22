/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor;

import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.http.HttpServletResponse;
import net.hasor.dataway.web.WebResponse;

public class HasorWebResponse extends WebResponse {
    private final HttpServletResponse response;

    public HasorWebResponse(HttpServletResponse response) {
        this.response = response;
    }

    @Override
    protected OutputStream openBody(int status) throws IOException {
        this.response.setStatus(status);
        return this.response.getOutputStream();
    }

    @Override
    protected void writeHeader(String name, String value, boolean append) {
        if (append) {
            this.response.addHeader(name, value);
        } else {
            this.response.setHeader(name, value);
        }
    }

    @Override
    public boolean isCommitted() {
        return this.response.isCommitted();
    }
}
