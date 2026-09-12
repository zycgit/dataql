/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.operator.ops;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.kernel.QueryRuntimeException;
import net.hasor.dataql.kernel.operator.OperatorProcess;
import net.hasor.dataql.parser.location.RuntimeLocation;

/**
 * 二元运算
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
abstract class AbstractDOP implements OperatorProcess {
    /** 执行运算 */
    @Override
    public Object doProcess(RuntimeLocation location, String operator, Object[] args, Hints option) throws QueryRuntimeException {
        if (args == null) {
            throw new QueryRuntimeException(location, "dyadic operator error, args is null.");
        }
        if (args.length != 2) {
            throw new QueryRuntimeException(location, "dyadic operator error, args count expect 2 , but " + args.length);
        }
        if (!testIn(new String[] { "+", "-", "*", "/", "%", "\\", ">", ">=", "<", "<=", "==", "!=", "&", "|", "^", "<<", ">>", ">>>", "||", "&&" }, operator)) {
            throw new QueryRuntimeException(location, "does not support dyadic Operator -> " + operator);
        }
        return this.doDyadicProcess(location, operator, args[0], args[1], option);
    }

    protected static QueryRuntimeException throwError(RuntimeLocation location, String operator, Object realFstObject, Object realSecObject, String message) {
        String fstDataType = realFstObject == null ? "null" : realFstObject.getClass().getName();
        String secDataType = realSecObject == null ? "null" : realSecObject.getClass().getName();
        message = StringUtils.isBlank(message) ? "no message." : message;
        return new QueryRuntimeException(location, fstDataType + " and " + secDataType + " , Cannot be used as '" + operator + "' -> " + message);
    }

    /** 执行运算 */
    public abstract Object doDyadicProcess(RuntimeLocation location, String operator, Object fstObject, Object secObject, Hints option) throws QueryRuntimeException;
}
