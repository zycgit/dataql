/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.model.api;
/**
 * 支持的语言枚举
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-09-08
 */
public enum ApiTypeEnum {
    /** DataQL 语言 */
    DataQL("DataQL"),
    /** SQL 语言 */
    SQL("SQL");
    private final String typeString;

    ApiTypeEnum(String typeStr) {
        this.typeString = typeStr;
    }

    public static ApiTypeEnum typeOf(Object codeType) {
        if (codeType == null) {
            return null;
        }
        String target = codeType.toString();
        for (ApiTypeEnum typeEnum : values()) {
            if (String.valueOf(typeEnum.typeString).equalsIgnoreCase(target)) {
                return typeEnum;
            }
            if (typeEnum.name().equalsIgnoreCase(target)) {
                return typeEnum;
            }
        }
        return null;
    }

    public String typeString() {
        return this.typeString;
    }
}
