/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.config;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dynamic.logic.ArrayDynamicSql;

/**
 * <selectKey> 标签
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-10-04
 */
public class SelectKeyConfig extends SqlConfig {
    private int           fetchSize     = 256;
    private ResultSetType resultSetType = ResultSetType.DEFAULT;
    private String        keyProperty   = null;
    private String        keyColumn     = null;
    private String        order         = null;

    public SelectKeyConfig(ArrayDynamicSql target, Hints config) {
        super(target, config);

        if (config != null) {
            String fetchSizeStr = SqlHintNames.getValue(config, SqlHintNames.FRAGMENT_SQL_FETCH_SIZE);
            String resultSetTypeStr = SqlHintNames.getValue(config, SqlHintNames.FRAGMENT_SQL_RESULT_SET_TYPE);
            String keyProperty = SqlHintNames.getValue(config, SqlHintNames.FRAGMENT_SQL_KEY_PROPERTY);
            String keyColumn = SqlHintNames.getValue(config, SqlHintNames.FRAGMENT_SQL_KEY_COLUMN);
            String order = SqlHintNames.getValue(config, SqlHintNames.FRAGMENT_SQL_ORDER);

            this.fetchSize = Integer.parseInt(StringUtils.isBlank(fetchSizeStr) ? "256" : fetchSizeStr);
            this.resultSetType = ResultSetType.valueOfCode(resultSetTypeStr, ResultSetType.DEFAULT);
            this.keyProperty = keyProperty;
            this.keyColumn = keyColumn;
            this.order = order;
        }
    }

    @Override
    public QueryType getType() {
        return QueryType.Select;
    }

    public ArrayDynamicSql getTarget() {
        return this.target;
    }

    public int getFetchSize() {
        return this.fetchSize;
    }

    public ResultSetType getResultSetType() {
        return this.resultSetType;
    }

    public String getKeyProperty() {
        return this.keyProperty;
    }

    public String getKeyColumn() {
        return this.keyColumn;
    }

    public String getOrder() {
        return this.order;
    }
}
