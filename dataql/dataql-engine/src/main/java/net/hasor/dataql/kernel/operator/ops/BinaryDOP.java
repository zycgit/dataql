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
import net.hasor.dataql.kernel.operator.OperatorUtils;
import net.hasor.dataql.parser.location.RuntimeLocation;

/**
 * Bitwise operations: {@code &}, {@code |}, {@code ^}, {@code <<}, {@code >>}, {@code >>>}.
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class BinaryDOP extends AbstractDOP {
    private static final Integer BOOL_FALSE = 0;
    private static final Integer BOOL_TRUE  = 1;

    @Override
    public Object doDyadicProcess(RuntimeLocation location, String operator, Object fstObject, Object secObject, Hints option) throws QueryRuntimeException {
        //
        // .Boolean 和 Number 混杂模式下，先统一成为 number 在做判断
        if (OperatorUtils.isBoolean(fstObject) && OperatorUtils.isBoolean(secObject)) {
            fstObject = Boolean.TRUE.equals(fstObject) ? BOOL_TRUE : BOOL_FALSE;
            secObject = Boolean.TRUE.equals(secObject) ? BOOL_TRUE : BOOL_FALSE;
        }
        if (OperatorUtils.isBoolean(fstObject) && OperatorUtils.isNumber(secObject)) {
            fstObject = Boolean.TRUE.equals(fstObject) ? BOOL_TRUE : BOOL_FALSE;
            secObject = OperatorUtils.eq((Number) secObject, 0) ? BOOL_FALSE : BOOL_TRUE;
        }
        if (OperatorUtils.isNumber(fstObject) && OperatorUtils.isBoolean(secObject)) {
            fstObject = OperatorUtils.eq((Number) fstObject, 0) ? BOOL_FALSE : BOOL_TRUE;
            secObject = Boolean.TRUE.equals(secObject) ? BOOL_TRUE : BOOL_FALSE;
        }
        // .与
        if ("&".equals(operator)) {
            return OperatorUtils.and((Number) fstObject, (Number) secObject);
        }
        // .或
        if ("|".equals(operator)) {
            return OperatorUtils.or((Number) fstObject, (Number) secObject);
        }
        // .异或
        if ("^".equals(operator)) {
            return OperatorUtils.xor((Number) fstObject, (Number) secObject);
        }
        // .左位移
        if ("<<".equals(operator)) {
            return OperatorUtils.shiftLeft((Number) fstObject, (Number) secObject);
        }
        // .带符号右位移
        if (">>".equals(operator)) {
            return OperatorUtils.shiftRight((Number) fstObject, (Number) secObject);
        }
        // .无符号右位移
        if (">>>".equals(operator)) {
            return OperatorUtils.shiftRightWithUnsigned((Number) fstObject, (Number) secObject);
        }
        throw throwError(location, operator, fstObject, secObject, "this operator nonsupport.");
    }
}
