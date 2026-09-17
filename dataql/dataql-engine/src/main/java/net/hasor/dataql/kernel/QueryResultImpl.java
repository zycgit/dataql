/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.kernel.mem.ExitType;

/**
 * 结果
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
class QueryResultImpl implements QueryResult {
    private final ExitType  exitType;
    private final int       exitCode;
    private final DataModel dataModel;
    private final long      executionTime;

    QueryResultImpl(ExitType exitType, int exitCode, DataModel dataModel, long executionTime) {
        this.exitType = exitType;
        this.exitCode = exitCode;
        this.dataModel = dataModel;
        this.executionTime = executionTime;
    }

    @Override
    public ExitType getExitType() {
        return this.exitType;
    }

    @Override
    public int getCode() {
        return this.exitCode;
    }

    @Override
    public DataModel getData() {
        return this.dataModel;
    }

    @Override
    public long executionTime() {
        return this.executionTime;
    }
}
