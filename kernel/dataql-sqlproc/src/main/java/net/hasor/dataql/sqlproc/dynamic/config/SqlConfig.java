/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.config;
import java.sql.SQLException;
import java.util.Objects;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dynamic.DynamicSql;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.logic.ArrayDynamicSql;
import net.hasor.dataql.sqlproc.types.SqlArgSource;

/**
 * Segment SqlConfig
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-06-19
 */
public abstract class SqlConfig implements DynamicSql {
    protected ArrayDynamicSql target;
    private   StatementType   statementType = StatementType.Prepared;
    private   int             timeout       = -1;

    public SqlConfig(ArrayDynamicSql target, Hints config) {
        this.target = Objects.requireNonNull(target, "target is null.");

        if (config != null) {
            String statementTypeStr = SqlHintNames.getValue(config, SqlHintNames.FRAGMENT_SQL_STATEMENT);
            String timeoutStr = SqlHintNames.getValue(config, SqlHintNames.FRAGMENT_SQL_TIMEOUT);

            this.statementType = StatementType.valueOfCode(statementTypeStr, StatementType.Prepared);
            this.timeout = Integer.parseInt(StringUtils.isBlank(timeoutStr) ? "-1" : timeoutStr);
        }
    }

    public StatementType getStatementType() {
        return this.statementType;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public abstract QueryType getType();

    @Override
    public boolean isHaveInjection() {
        return this.target.isHaveInjection();
    }

    @Override
    public void buildQuery(SqlArgSource data, QueryContext context, SqlBuilder sqlBuilder) throws SQLException {
        this.target.buildQuery(data, context, sqlBuilder);
    }
}
