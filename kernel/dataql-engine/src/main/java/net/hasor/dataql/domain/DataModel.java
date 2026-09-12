/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.domain;
/**
 * 结果集
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public interface DataModel {
    /** 得到本来面目 */
    Object asOri();

    /** 解开 DataModel 包裹，采用 Map 和 List 封装。 */
    Object unwrap();

    /** 判断是否为 ValueModel 类型值 */
    default boolean isValue() {
        return false;
    }

    /** 判断是否为 ListModel 类型值 */
    default boolean isList() {
        return false;
    }

    /** 判断是否为 ObjectModel 类型值 */
    default boolean isObject() {
        return false;
    }

    /** 判断是否为 UdfModel 类型值 */
    default boolean isUdf() {
        return false;
    }
}
