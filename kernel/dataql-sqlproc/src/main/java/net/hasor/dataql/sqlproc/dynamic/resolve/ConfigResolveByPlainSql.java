/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.resolve;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import net.hasor.dataql.sqlproc.dynamic.logic.ArrayDynamicSql;
import net.hasor.dataql.sqlproc.dynamic.segment.PlanDynamicSql;

/**
 * parse dynamic SQL from mapperFile
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-06-05
 */
public class ConfigResolveByPlainSql extends ConfigResolve {
    @Override
    public SqlConfig parseConfig(String fragmentName, Hints hint, String config) {
        QueryType queryType = QueryType.valueOfTag(fragmentName.toLowerCase().trim());
        if (queryType == null) {
            throw new UnsupportedOperationException("fragment '" + fragmentName + "' Unsupported.");
        }

        ArrayDynamicSql dynamicSql = new ArrayDynamicSql();
        dynamicSql.addChildNode(new PlanDynamicSql(config));
        return super.createConfig(queryType, hint, dynamicSql);
    }
}
