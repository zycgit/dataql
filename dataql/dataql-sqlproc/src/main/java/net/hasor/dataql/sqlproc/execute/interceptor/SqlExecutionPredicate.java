/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute.interceptor;

import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;

/** Selects whether an interceptor applies to a SQL fragment. */
@FunctionalInterface
public interface SqlExecutionPredicate {
    boolean accept(QueryType type, String fragmentString, Hints hints);
}
