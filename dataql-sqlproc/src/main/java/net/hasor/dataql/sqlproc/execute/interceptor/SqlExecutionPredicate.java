package net.hasor.dataql.sqlproc.execute.interceptor;

import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;

/** Selects whether an interceptor applies to a SQL fragment. */
@FunctionalInterface
public interface SqlExecutionPredicate {
    boolean accept(QueryType type, String fragmentString, Hints hints);
}
