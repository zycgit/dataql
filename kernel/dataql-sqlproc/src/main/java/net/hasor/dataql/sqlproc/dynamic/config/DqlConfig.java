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
 * has result query.
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-06-19
 */
public abstract class DqlConfig extends SqlConfig {
    private int           fetchSize     = 256;
    private ResultSetType resultSetType = ResultSetType.DEFAULT;
    private String[]      bindOut       = ArrayUtils.EMPTY_STRING_ARRAY;

    public DqlConfig(ArrayDynamicSql target, Hints config) {
        super(target, config);

        if (config != null) {
            String fetchSizeStr = SqlHintNames.getValue(config, SqlHintNames.FRAGMENT_SQL_FETCH_SIZE);
            String resultSetTypeStr = SqlHintNames.getValue(config, SqlHintNames.FRAGMENT_SQL_RESULT_SET_TYPE);
            String bindOutStr = SqlHintNames.getValue(config, SqlHintNames.FRAGMENT_SQL_BIND_OUT);

            this.fetchSize = Integer.parseInt(StringUtils.isBlank(fetchSizeStr) ? "256" : fetchSizeStr);
            this.resultSetType = ResultSetType.valueOfCode(resultSetTypeStr, ResultSetType.DEFAULT);
            this.bindOut = StringUtils.isNotBlank(bindOutStr) ? bindOutStr.split(",") : ArrayUtils.EMPTY_STRING_ARRAY;
        }
    }

    public int getFetchSize() {
        return this.fetchSize;
    }

    public ResultSetType getResultSetType() {
        return this.resultSetType;
    }

    public String[] getBindOut() {
        return this.bindOut;
    }
}
