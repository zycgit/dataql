/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service;
import java.util.Objects;
import net.hasor.dataway.Dataway;
import net.hasor.dataway.model.*;

/** Writes entry results while preserving host exception handling and HEAD semantics. */
public abstract class AbstractWebHandler implements WebHandler {
    private final Dataway dataway;

    protected AbstractWebHandler(Dataway dataway) {
        this.dataway = Objects.requireNonNull(dataway);
    }

    public final Dataway getDataway() {
        return this.dataway;
    }

    @Override
    public final void handle(WebRequest request, WebResponse response) throws Exception {
        response.prepare(request);
        ResultInfoUtils.writeTo(this.handleRequest(request, response), request, response);
    }

    protected abstract ResultInfo handleRequest(WebRequest request, WebResponse response) throws Exception;
}
