/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.script;

/** Continues script execution with the supplied interceptor context. */
@FunctionalInterface
public interface ApiInterceptorChain {
    Object proceed(ApiInterceptorContext context) throws Exception;
}
