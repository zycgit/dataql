/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.operator.ops;
import java.util.Objects;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.kernel.QueryRuntimeException;
import net.hasor.dataql.parser.location.RuntimeLocation;

/**
 * 字符串拼接
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class ObjectEqDOP extends AbstractDOP {
    @Override
    public Object doDyadicProcess(RuntimeLocation location, String operator, Object fstObject, Object secObject, Hints option) throws QueryRuntimeException {
        if ("==".equals(operator)) {
            return Objects.equals(fstObject, secObject);
        }
        if ("!=".equals(operator)) {
            return !Objects.equals(fstObject, secObject);
        }
        throw throwError(location, operator, fstObject, secObject, "symbol unsupported.");
    }
}
