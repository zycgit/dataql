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

/** Internal signal that unwinds function calls after a script exits the query. */
public final class QueryExitException extends QueryRuntimeException {
    private final int       resultCode;
    private final DataModel result;

    public QueryExitException(RuntimeLocation location, int resultCode, DataModel result) {
        super(location, "query exited.");
        this.resultCode = resultCode;
        this.result = result;
    }

    public int getResultCode() {
        return this.resultCode;
    }

    public DataModel getResult() {
        return this.result;
    }
}
