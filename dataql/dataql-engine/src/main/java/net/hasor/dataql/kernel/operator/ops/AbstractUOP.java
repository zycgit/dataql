/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.operator.ops;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.kernel.QueryRuntimeException;
import net.hasor.dataql.kernel.operator.OperatorProcess;
import net.hasor.dataql.parser.location.RuntimeLocation;

/**
 * 一元运算
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
abstract class AbstractUOP implements OperatorProcess {
    /** 执行运算 */
    public Object doProcess(RuntimeLocation location, String operator, Object[] args, Hints option) throws QueryRuntimeException {
        if (args == null) {
            throw new QueryRuntimeException(location, "unary operator error, args is null.");
        }
        if (args.length != 1) {
            throw new QueryRuntimeException(location, "unary operator error, args count expect 1 , but " + args.length);
        }
        if (!testIn(new String[] { "!", "-", "+" }, operator)) {
            throw new QueryRuntimeException(location, "does not support unary Operator -> " + operator);
        }
        //
        return this.doUnaryProcess(location, operator, args[0], option);
    }

    /** 执行运算 */
    public abstract Object doUnaryProcess(RuntimeLocation location, String operator, Object object, Hints option) throws QueryRuntimeException;
}
