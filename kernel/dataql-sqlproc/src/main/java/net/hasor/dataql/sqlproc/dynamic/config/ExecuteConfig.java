/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.config;
import net.hasor.cobble.ArrayUtils;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dynamic.logic.ArrayDynamicSql;

/**
 * Execute SqlConfig
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-06-19
 */
public class ExecuteConfig extends SqlConfig {
    private String[] bindOut = ArrayUtils.EMPTY_STRING_ARRAY;

    public ExecuteConfig(ArrayDynamicSql target, Hints config) {
        super(target, config);

        if (config != null) {
            String bindOutStr = SqlHintNames.getValue(config, SqlHintNames.FRAGMENT_SQL_BIND_OUT);

            this.bindOut = StringUtils.isNotBlank(bindOutStr) ? bindOutStr.split(",") : ArrayUtils.EMPTY_STRING_ARRAY;
        }
    }

    @Override
    public QueryType getType() {
        return QueryType.Execute;
    }

    public String[] getBindOut() {
        return this.bindOut;
    }
}
