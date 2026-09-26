/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.logic;
import net.hasor.dataql.sqlproc.dynamic.segment.PlanDynamicSql;

/**
 * <include> 标签
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-05-24
 */
public class MacroDynamicSql extends PlanDynamicSql {
    public MacroDynamicSql(String refSql) {
        super("@{macro, " + refSql + "}");
    }
}
