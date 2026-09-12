/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.operator;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.kernel.QueryRuntimeException;
import net.hasor.dataql.parser.location.RuntimeLocation;

/**
 * 一元或二元运算，用于运算符重载。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public interface OperatorProcess {
    /** 执行运算 */
    Object doProcess(RuntimeLocation location, String operator, Object[] args, Hints option) throws QueryRuntimeException;

    default boolean testIn(String[] dataSet, String test) {
        if (dataSet == null || dataSet.length == 0 || StringUtils.isBlank(test)) {
            return false;
        }
        for (String str : dataSet) {
            if (test.equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }
}
