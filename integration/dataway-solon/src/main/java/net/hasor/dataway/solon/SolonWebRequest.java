/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon;
import java.io.IOException;
import java.io.InputStream;
import net.hasor.dataway.model.WebRequest;
import org.noear.solon.core.handle.Context;

public class SolonWebRequest extends WebRequest {
    private final Context context;

    public SolonWebRequest(Context context) {
        this.context = context;
    }

    @Override
    public InputStream getBody() throws IOException {
        return this.context.bodyAsStream();
    }

    @Override
    public Object getAttribute(String name) {
        return this.context.attr(name);
    }
}
