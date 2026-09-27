/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.mem;
import net.hasor.cobble.ExceptionUtils;
import net.hasor.dataql.DataQueryException;
import net.hasor.dataql.domain.*;
import net.hasor.dataql.kernel.Finder;
import net.hasor.dataql.kernel.QueryRuntimeException;
import net.hasor.dataql.parser.location.RuntimeLocation;

/**
 * 栈数据
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2019-11-22
 */
public class RefCall {
    private final RuntimeLocation location;
    private final boolean         autoUnwrap;
    private final Udf             refCall;

    public RefCall(RuntimeLocation location, boolean autoUnwrap, Udf refCall) {
        this.location = location;
        this.autoUnwrap = autoUnwrap;
        this.refCall = refCall;
    }

    public Object invokeMethod(Object[] paramArrays, Hints optionSet, Finder finder) throws DataQueryException {
        try {
            Object[] objects = paramArrays.clone();
            if (this.autoUnwrap) {
                for (int i = 0; i < objects.length; i++) {
                    if (objects[i] instanceof DataModel) {
                        objects[i] = ((DataModel) objects[i]).unwrap();
                    }
                }
            }

            Object result = this.refCall.call(optionSet, objects);
            if (result instanceof UdfSource) {
                result = ((UdfSource) result).getUdfResource(finder).get();
            }
            return DomainHelper.convertTo(result);
        } catch (Throwable e) {
            if (e instanceof DataQueryException) {
                throw (DataQueryException) e;
            }
            throw ExceptionUtils.toRuntime(e, throwable -> {
                String message = e.getClass().getName() + ": " + throwable.getLocalizedMessage();
                return new QueryRuntimeException(location, message, throwable);
            });
        }
    }
}
