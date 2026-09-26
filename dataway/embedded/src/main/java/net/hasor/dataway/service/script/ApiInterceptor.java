/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.script;

/** Intercepts script execution for short circuits, result transformation and error handling. */
@FunctionalInterface
public interface ApiInterceptor {
    Object invoke(ApiInterceptorContext context, ApiInterceptorChain chain) throws Exception;
}
