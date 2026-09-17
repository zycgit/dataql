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
 * 二元比较运算，负责处理：">", ">=", "<", "<=", "==", "!=", "&&", "||"
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class CompareDOP extends AbstractDOP {
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
        //
        // .大于
        if (">".equals(operator)) {
            return OperatorUtils.gt((Number) fstObject, (Number) secObject);
        }
        // .大于等于
        if (">=".equals(operator)) {
            return OperatorUtils.gteq((Number) fstObject, (Number) secObject);
        }
        // .小于
        if ("<".equals(operator)) {
            return OperatorUtils.lt((Number) fstObject, (Number) secObject);
        }
        // .小于等于
        if ("<=".equals(operator)) {
            return OperatorUtils.lteq((Number) fstObject, (Number) secObject);
        }
        // .等于
        if ("==".equals(operator)) {
            return OperatorUtils.eq((Number) fstObject, (Number) secObject);
        }
        // .不等于
        if ("!=".equals(operator)) {
            return !OperatorUtils.eq((Number) fstObject, (Number) secObject);
        }
        // .逻辑比较运算
        if ("&&".equals(operator) || "||".equals(operator)) {
            boolean fstBool, secBool;
            if (OperatorUtils.isNumber(fstObject)) {
                fstBool = !OperatorUtils.eq((Number) fstObject, 0);
            } else {
                fstBool = Boolean.TRUE.equals(fstObject);
            }
            if (OperatorUtils.isNumber(secObject)) {
                secBool = !OperatorUtils.eq((Number) secObject, 0);
            } else {
                secBool = Boolean.TRUE.equals(secObject);
            }
            //
            if ("&&".equals(operator)) {
                return fstBool && secBool;
            }
            if ("||".equals(operator)) {
                return fstBool || secBool;
            }
        }
        throw throwError(location, operator, fstObject, secObject, "this operator nonsupport.");
    }
}
