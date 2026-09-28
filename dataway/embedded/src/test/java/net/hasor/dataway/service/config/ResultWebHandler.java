/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.config;
import java.util.function.BiFunction;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.model.WebRequest;
import net.hasor.dataway.model.WebResponse;
import net.hasor.dataway.service.BeanContainer;
import net.hasor.dataway.service.WebHandler;

class ResultWebHandler extends WebHandler {
    private final BiFunction<WebRequest, WebResponse, ResultInfo> action;

    ResultWebHandler(BeanContainer beans, BiFunction<WebRequest, WebResponse, ResultInfo> action) {
        super(beans);
        this.action = action;
    }

    @Override
    protected ResultInfo handleRequest(WebRequest request, WebResponse response) {
        return this.action.apply(request, response);
    }
}
