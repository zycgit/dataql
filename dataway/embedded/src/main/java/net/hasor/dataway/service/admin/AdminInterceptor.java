/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.admin;

/** Intercepts administration requests before their controller is invoked. */
@FunctionalInterface
public interface AdminInterceptor {
    Object invoke(AdminInterceptorContext context, AdminInterceptorChain chain) throws Exception;
}
