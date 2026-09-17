/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute;
import java.sql.*;
import java.util.Map;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.dialect.BoundSql;
import net.hasor.dataql.sqlproc.dialect.Page;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.config.DqlConfig;
import net.hasor.dataql.sqlproc.dynamic.config.ResultSetType;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import net.hasor.dataql.sqlproc.types.TypeHandlerRegistry;

/**
 * 负责参数化SQL调用的执行器
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-07-20
 */
public class PreparedStatementExecute extends AbstractStatementExecute {
    public PreparedStatementExecute(QueryContext context) {
        super(context);
    }

    @Override
    protected void doCheck(Connection conn, Hints hints, SqlConfig config, Map<String, Object> data, Page pageInfo) throws SQLException {
        super.doCheck(conn, hints, config, data, pageInfo);
    }

    @Override
    protected PreparedStatement createStatement(Connection conn, SqlConfig config, BoundSql execSql) throws SQLException {
        if (config instanceof DqlConfig) {
            ResultSetType resultSetType = ((DqlConfig) config).getResultSetType();
            if (resultSetType == null || resultSetType == ResultSetType.DEFAULT) {
                return conn.prepareStatement(execSql.getSqlString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            } else {
                int resultSetTypeInt = resultSetType.getResultSetType();
                return conn.prepareStatement(execSql.getSqlString(), resultSetTypeInt, ResultSet.CONCUR_READ_ONLY);
            }
        } else {
            return conn.prepareStatement(execSql.getSqlString());
        }
    }

    @Override
    protected boolean executeQuery(Statement stat, SqlConfig config, BoundSql execSql) throws SQLException {
        try {
            PreparedStatement ps = (PreparedStatement) stat;
            Object[] args = execSql.getArgs();
            for (int j = 0; j < args.length; j++) {
                TypeHandlerRegistry.DEFAULT.setParameterValue(ps, j + 1, args[j]);
            }

            return ps.execute();
        } catch (SQLException e) {
            logger.error("executeQuery failed, " + ExecuteHelper.fmtBoundSql(execSql), e);
            throw e;
        }
    }
}
