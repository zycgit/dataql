/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.model.api;
/**
 * API 状态枚举
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-09-08
 */
public enum ApiStatusEnum {
    /** 编辑中 */
    Editor(0),
    /** 已发布 */
    Published(1),
    /** 已发布有修改 */
    Changes(2),
    /** 已禁用 */
    Disable(3),
    /** 已删除 */
    Delete(-1);
    private final int typeNum;

    ApiStatusEnum(int typeNum) {
        this.typeNum = typeNum;
    }

    public static ApiStatusEnum typeOf(Object codeType) {
        if (codeType == null) {
            return null;
        }
        String target = codeType.toString();
        for (ApiStatusEnum typeEnum : values()) {
            if (String.valueOf(typeEnum.typeNum).equalsIgnoreCase(target)) {
                return typeEnum;
            }
            if (typeEnum.name().equalsIgnoreCase(target)) {
                return typeEnum;
            }
        }
        return null;
    }

    public int typeNum() {
        return typeNum;
    }
}
