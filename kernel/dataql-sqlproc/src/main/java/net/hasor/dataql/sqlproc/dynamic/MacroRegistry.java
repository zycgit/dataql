/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic;
import java.util.HashMap;
import java.util.Map;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.sqlproc.dynamic.segment.DynamicParsed;

/**
 * 解析动态 SQL 配置
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-06-05
 */
public class MacroRegistry {
    private final Map<String, DynamicSql> macroMap = new HashMap<>();

    public DynamicSql findMacro(String dynamicId) {
        return this.macroMap.get(dynamicId);
    }

    public void register(String macroName, String sqlSegment) {
        if (StringUtils.isNotBlank(macroName)) {
            this.macroMap.put(macroName, DynamicParsed.getParsedSql(sqlSegment));
        }
    }

    public void register(String macroName, DynamicSql sqlSegment) {
        if (StringUtils.isNotBlank(macroName)) {
            this.macroMap.put(macroName, sqlSegment);
        }
    }
}
