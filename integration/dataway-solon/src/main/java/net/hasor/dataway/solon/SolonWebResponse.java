/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon;
import java.io.IOException;
import java.io.OutputStream;
import net.hasor.dataway.web.WebResponse;
import org.noear.solon.core.handle.Context;

public class SolonWebResponse extends WebResponse {
    private final Context context;

    public SolonWebResponse(Context context) {
        this.context = context;
    }

    @Override
    protected OutputStream openBody(int status) throws IOException {
        this.context.status(status);
        this.context.setHandled(true);
        this.context.setRendered(true);
        return this.context.outputStream();
    }

    @Override
    protected void writeHeader(String name, String value, boolean append) {
        if (append) {
            this.context.headerAdd(name, value);
        } else {
            this.context.headerSet(name, value);
        }
    }

    @Override
    public boolean isCommitted() {
        return this.context.isHeadersSent();
    }
}
