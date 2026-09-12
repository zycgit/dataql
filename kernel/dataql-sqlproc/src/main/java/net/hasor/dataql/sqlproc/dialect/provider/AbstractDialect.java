/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dialect.provider;
import net.hasor.dataql.sqlproc.dialect.BoundSql;
import net.hasor.dataql.sqlproc.dialect.PageDialect;

/**
 * 公共 SqlDialect 实现
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-10-31
 */
public abstract class AbstractDialect implements PageDialect {

    /** 生成 count 查询 SQL */
    public BoundSql countSql(BoundSql boundSql) {
        return new BoundSql.BoundSqlObj("SELECT COUNT(*) FROM (" + boundSql.getSqlString() + ") as TEMP_T", boundSql.getArgs());
    }
}
