/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.config;
import net.hasor.cobble.StringUtils;

/**
 * 查询类型
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-06-19
 */
public enum QueryType {
    /** Insert 类型 */
    Insert("insert"),
    /** Delete 类型 */
    Delete("delete"),
    /** Update 类型 */
    Update("update"),
    /** 任意类型语句 */
    Execute("execute"),
    /** 查询类型 类型 */
    Select("select"),
    /** 查询类型 类型 */
    Call("call"),
    ;

    private final String tagString;

    public String getTagString() {
        return this.tagString;
    }

    QueryType(String tagString) {
        this.tagString = tagString;
    }

    public static QueryType valueOfTag(String xmlTag) {
        if (StringUtils.isBlank(xmlTag)) {
            return null;
        }

        for (QueryType tableType : QueryType.values()) {
            if (StringUtils.startsWithIgnoreCase(xmlTag, tableType.tagString)) {
                return tableType;
            }
        }
        return null;
    }
}
