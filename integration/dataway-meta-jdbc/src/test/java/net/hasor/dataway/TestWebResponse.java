/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.model.WebResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Captures a successful JSON response without a web framework. */
public class TestWebResponse extends WebResponse {
    private final ByteArrayOutputStream body = new ByteArrayOutputStream();
    private int status;

    @Override
    protected OutputStream openBody(int status) {
        this.status = status;
        return this.body;
    }

    @Override
    protected void writeHeader(String name, String value, boolean append) {
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    public Object getResult() {
        assertEquals(200, this.status);
        return JsonUtils.readValue(this.body.toString(StandardCharsets.UTF_8), Object.class);
    }
}
