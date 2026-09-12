/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.host.function;

/** Access to all arguments of the current UDF call. */
@FunctionalInterface
public interface UdfParams {
    Object[] allParams();
}
