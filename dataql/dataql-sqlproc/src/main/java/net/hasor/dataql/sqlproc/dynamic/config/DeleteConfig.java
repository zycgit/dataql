/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.config;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.dynamic.logic.ArrayDynamicSql;

/**
 * Delete SqlConfig
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-06-19
 */
public class DeleteConfig extends DmlConfig {
    public DeleteConfig(ArrayDynamicSql target, Hints config) {
        super(target, config);
    }

    @Override
    public QueryType getType() {
        return QueryType.Delete;
    }
}
