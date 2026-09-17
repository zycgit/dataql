/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.operator;
/**
 * 一元运算操作对象的注册和查找。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2019-12-11
 */
public interface UnaryOperatorRegistry {
    /** 添加 操作符 实现 */
    default void registryOperator(String symbolName, Class[] opeTypeSet, OperatorProcess process) {
        if (opeTypeSet == null || opeTypeSet.length == 0) {
            throw new NullPointerException("classSetA or classSetB is empty.");
        }
        for (Class opeType : opeTypeSet) {
            this.registryOperator(symbolName, opeType, process);
        }
    }

    /** 添加 操作符 实现 */
    void registryOperator(String symbolName, Class<?> opeType, OperatorProcess process);

    /** 查找 操作符 实现 */
    OperatorProcess findUnaryProcess(String symbolName, Class<?> fstType);
}
