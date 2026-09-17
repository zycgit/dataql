/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.rule;
import java.sql.Types;
import java.util.UUID;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.types.*;
import net.hasor.dataql.sqlproc.types.string.StringTypeHandler;

/**
 * 产生一个 36 字符长度的 `UUID`，并加入到 SQL 参数中
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-10-31
 */
public class UUID36Rule implements SqlRule {
    private static final TypeHandler typeHandler = TypeHandlerRegistry.DEFAULT.getHandlerByHandlerType(StringTypeHandler.class);
    public static final  UUID36Rule  INSTANCE    = new UUID36Rule();

    @Override
    public boolean test(SqlArgSource data, QueryContext context, String activeExpr) {
        return true;
    }

    @Override
    public void executeRule(SqlArgSource data, QueryContext context, SqlBuilder sqlBuilder, String activeExpr, String ruleValue) {
        String uuidValue = UUID.randomUUID().toString();
        SqlArg sqlArg = new SqlArg(ruleValue, uuidValue, SqlMode.In, Types.VARCHAR, typeHandler);
        sqlBuilder.appendSql("?", sqlArg);
    }

    @Override
    public String toString() {
        return "uuid36 [" + this.hashCode() + "]";
    }
}
