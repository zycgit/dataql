/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.parser.location.RuntimeLocation;

/**
 * DataQL 运行时异常
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-14
 */
public class ThrowRuntimeException extends QueryRuntimeException {
    protected int       throwCode     = 500;
    protected long      executionTime = -1;
    protected DataModel result        = null;

    public ThrowRuntimeException(RuntimeLocation location, String errorMessage) {
        super(location, errorMessage);
    }

    public ThrowRuntimeException(RuntimeLocation location, String errorMessage, Throwable e) {
        super(location, errorMessage, e);
    }

    public ThrowRuntimeException(RuntimeLocation location, String errorMessage, int throwCode, long executionTime, DataModel result) {
        this(location, errorMessage);
        this.throwCode = throwCode;
        this.executionTime = executionTime;
        this.result = result;
    }

    public int getThrowCode() {
        return throwCode;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public DataModel getResult() {
        return result;
    }
}
