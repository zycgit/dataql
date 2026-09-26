/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute.fragment;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContext;

public class UpdateFragmentProcess extends AbstractSqlFragment {
    public UpdateFragmentProcess(ExecuteContext context) {
        super(context);
    }

    @Override
    protected QueryType queryType(String fragmentString, Hints hints) {
        return QueryType.Update;
    }
}
