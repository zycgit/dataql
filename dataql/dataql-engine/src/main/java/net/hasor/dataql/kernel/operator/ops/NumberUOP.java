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
 * 一元运算。number类型的只处理：负号
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class NumberUOP extends AbstractUOP {
    @Override
    public Object doUnaryProcess(RuntimeLocation location, String operator, Object object, Hints option) throws QueryRuntimeException {
        if ("-".equals(operator) && object instanceof Number) {
            return OperatorUtils.negate((Number) object);
        }
        String dataType = object == null ? "null" : object.getClass().getName();
        throw new QueryRuntimeException(location, dataType + " , Cannot be used as '" + operator + "'.");
    }
}
