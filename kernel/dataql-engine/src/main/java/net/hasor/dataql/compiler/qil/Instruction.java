/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.compiler.qil;
/**
 * QL 指令
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-03
 */
public interface Instruction {
    /** 获取指令码。 */
    byte getInstCode();

    /** 获取 字符串数据 */
    String getString(int index);

    /** 获取 布尔数据 */
    Boolean getBoolean(int index);

    /** 获取 数字数据 */
    Number getNumber(int index);

    /** 获取 数字数据 */
    int getInt(int index);

    /** 获取 字符串数据 */
    Object[] getArrays();

    String toString();
}
