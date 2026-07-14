/*
 * Copyright 2015-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.dataql.domain;
import static net.hasor.dataql.domain.HintValue.INDEX_OVERFLOW_NEAR;
import static net.hasor.dataql.domain.HintValue.MIN_DECIMAL_WIDTH_DOUBLE;
import static net.hasor.dataql.domain.HintValue.MIN_INTEGER_WIDTH_INT;
import static net.hasor.dataql.domain.HintValue.NUMBER_ROUNDING_HALF_UP;

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
    /** 浮点数计算使用的最小数值宽度，可选值有：float,double,big。默认为：double */
    MIN_DECIMAL_WIDTH(MIN_DECIMAL_WIDTH_DOUBLE),
    /** 整数计算使用的最小数值宽度，可选值有：byte,short,int,long,big。默认为：int */
    MIN_INTEGER_WIDTH(MIN_INTEGER_WIDTH_INT),
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