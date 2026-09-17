/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.operator.ops;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.kernel.operator.OperatorProcess;
import net.hasor.dataql.parser.location.RuntimeLocation;

/**
 * 字符串拼接
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class StringJointDOP implements OperatorProcess {
    @Override
    public Object doProcess(RuntimeLocation location, String operator, Object[] args, Hints option) {
        String str1 = args[0] == null ? "null" : args[0].toString();
        String str2 = args[1] == null ? "null" : args[1].toString();
        return str1 + str2;
    }
}
