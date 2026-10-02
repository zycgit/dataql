/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.domain;
/**
 * TYPEOF   // 计算表达式值的类型。
 * - 参数说明：共0参数；
 * - 栈行为：消费1，产出1，产出内容为：string、number、boolean、object、list、udf、null
 * - 堆行为：无
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-01-24
 */
public enum TypeOfEnum {
    String("string"),   //
    Number("number"),   //
    Boolean("boolean"), //
    Object("object"),   //
    List("list"),       //
    Udf("udf"),         //
    Binary("binary"),   //
    Null("null");       //
    private final String typeOfEnum;

    public String typeCode() {
        return typeOfEnum;
    }

    TypeOfEnum(String typeOfEnum) {
        this.typeOfEnum = typeOfEnum;
    }
}
