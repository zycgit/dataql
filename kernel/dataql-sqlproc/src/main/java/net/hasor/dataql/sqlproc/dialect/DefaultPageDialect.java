/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dialect;
/**
 * 默认 SqlDialect 实现
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-10-31
 */
public class DefaultPageDialect implements PageDialect {
    public static final DefaultPageDialect DEFAULT = new DefaultPageDialect();

    @Override
    public BoundSql pageSql(BoundSql boundSql, long start, long limit) {
        throw new UnsupportedOperationException();
    }
}
