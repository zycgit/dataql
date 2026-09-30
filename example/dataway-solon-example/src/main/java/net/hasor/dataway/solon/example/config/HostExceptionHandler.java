/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.example.config;
import java.util.Map;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.service.DatawayException;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Handler;
import org.noear.solon.core.route.RouterInterceptor;
import org.noear.solon.core.route.RouterInterceptorChain;

/** Converts application failures at the host MVC boundary. */
public class HostExceptionHandler implements RouterInterceptor {
    @Override
    public void doIntercept(Context context, Handler handler, RouterInterceptorChain chain) throws Throwable {
        try {
            chain.doIntercept(context, handler);
        } catch (DatawayException failure) {
            context.status(failure.status());
            context.setHandled(true);
            context.outputAsJson(JsonUtils.writeValueAsString(Map.of("message", failure.getMessage())));
        }
    }
}
