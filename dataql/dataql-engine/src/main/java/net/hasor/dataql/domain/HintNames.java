/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.domain;
import static net.hasor.dataql.domain.HintValue.*;

/**
 * Hint 的 keys 定义。
 */
public enum HintNames {
    /** 设置索引溢出的行为，可选的行为有：throw、null、near，默认为：near */
    INDEX_OVERFLOW(INDEX_OVERFLOW_NEAR),
    /** 最大保留的小数位数，默认为：20。超出该范围将会根据 NUMBER_ROUNDING 选项指定的舍入模式进行舍入，默认是四舍五入。 */
    MAX_DECIMAL_DIGITS("20"),
    /** 小数的舍入模式，参考 RoundingEnum 定义的舍入模式(一共八种)，默认为：四舍五入。详细配置参考：RoundingEnum 枚举。 */
    NUMBER_ROUNDING(NUMBER_ROUNDING_HALF_UP),
    /** 浮点数计算使用的最小数值宽度，可选值有：float,double,big。默认为：float */
    MIN_DECIMAL_WIDTH(MIN_DECIMAL_WIDTH_FLOAT),
    /** 整数计算使用的最小数值宽度，可选值有：byte,short,int,long,big。默认为：byte */
    MIN_INTEGER_WIDTH(MIN_INTEGER_WIDTH_BYTE),
    /** 外部片段调用的类型，例如 @@insert 会传递 "insert"，@@select 传递 "select" */
    FRAGMENT_TYPE(null);
    //
    private final String defaultVal;

    public String getDefaultVal() {
        return this.defaultVal;
    }

    HintNames(String defaultVal) {
        this.defaultVal = defaultVal;
    }
}
