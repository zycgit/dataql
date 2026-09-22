/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.authorization;
import net.hasor.dataway.web.WebRequest;

/** Resolves the host identity once for each request, including page and asset requests. */
@FunctionalInterface
public interface IdentityProvider {
    UserIdentity resolve(WebRequest request);
}
