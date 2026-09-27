/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.model.WebRequest;
import net.hasor.dataway.model.WebResponse;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.WebHandler;

/** Verifies that a handler failure reaches the host exception mechanism unchanged. */
class FailingWebHandler extends WebHandler {
    private final Exception failure;

    FailingWebHandler(Dataway dataway, Exception failure) {
        super(dataway);
        this.failure = failure;
    }

    @Override
    protected ResultInfo handleRequest(WebRequest request, WebResponse response) throws Exception {
        throw this.failure;
    }
}
