/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel;
import net.hasor.dataql.DataQueryException;
import net.hasor.dataql.parser.location.RuntimeLocation;

/**
 * DataQL 运行时异常
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-14
 */
public class QueryRuntimeException extends DataQueryException {
    public QueryRuntimeException(RuntimeLocation location, String errorMessage) {
        super(location, errorMessage);
    }

    public QueryRuntimeException(RuntimeLocation location, String errorMessage, Throwable e) {
        super(location, errorMessage, e);
    }

    public QueryRuntimeException(RuntimeLocation location, Throwable e) {
        super(location, e);
    }

    public int getProgramAddress() {
        return ((RuntimeLocation) this.location).getProgramAddress();
    }

    public int getMethodAddress() {
        return ((RuntimeLocation) this.location).getMethodAddress();
    }
}
