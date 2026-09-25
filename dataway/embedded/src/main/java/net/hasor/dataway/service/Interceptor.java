/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service;

/** Intercepts a published API call or administration action identified by its explicit operation. */
@FunctionalInterface
public interface Interceptor {
    Object invoke(InterceptorContext context, InterceptorChain chain) throws Exception;
}
