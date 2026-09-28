/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.authorization.IdentityProvider;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.model.WebRequest;
import net.hasor.dataway.model.WebResponse;

/** Resolves request identity and writes entry results while preserving host exception handling. */
public abstract class WebHandler {
    private final IdentityProvider identityProvider;

    protected WebHandler(BeanContainer beans) {
        this.identityProvider = beans.getBean(IdentityProvider.class);
    }

    /** Relative paths; a trailing /* denotes a subtree. The host adds its configured prefix. */
    public List<String> paths() {
        return List.of("", "/*");
    }

    /** Invoked after host routing. Failures propagate to the host without an error response. */
    public final void handle(WebRequest request, WebResponse response) throws Exception {
        response.prepare(request);

        UserIdentity identity = this.identityProvider.resolve(request);
        request.setIdentity(Objects.requireNonNull(identity, "IdentityProvider returned null"));
        ResultInfo result = this.handleRequest(request, response);
        Object data = result.getData();
        InputStream source = !result.isJson() && data instanceof InputStream stream ? stream : null;
        try (source) {
            OutputStream output = response.write(result.getStatus(), result.getHeaders());
            if (StringUtils.equalsIgnoreCase(request.getMethod(), "HEAD")) {
                return;
            }

            if (source != null) {
                source.transferTo(output);
                return;
            }

            if (!result.isJson() && data instanceof byte[] bytes) {
                output.write(bytes);
                return;
            }
            JsonUtils.writeValue(output, data);
        }
    }

    protected abstract ResultInfo handleRequest(WebRequest request, WebResponse response) throws Exception;
}
