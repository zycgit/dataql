/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.mem;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.kernel.QueryRuntimeException;
import net.hasor.dataql.parser.location.RuntimeLocation;

/**
 * 代理 Lambda 使其成为 UDF.
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class RefLambdaCallException extends QueryRuntimeException {
    private final int       resultCode;
    private final DataModel result;

    public RefLambdaCallException(RuntimeLocation location, int resultCode, DataModel result) {
        super(location, "udf or lambda failed.");
        this.resultCode = resultCode;
        this.result = result;
    }

    public int getResultCode() {
        return resultCode;
    }

    public DataModel getResult() {
        return result;
    }
}
