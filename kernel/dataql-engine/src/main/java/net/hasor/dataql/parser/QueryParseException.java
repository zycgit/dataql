/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.parser;
import net.hasor.dataql.DataQueryException;
import net.hasor.dataql.parser.location.CodeLocation;
import net.hasor.dataql.parser.location.LocationUtils;

/**
 * DataQL 解析异常。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-01-22
 */
public class QueryParseException extends DataQueryException {
    public QueryParseException(int line, int column, String message) {
        super(LocationUtils.atLocation(new CodeLocation(line, column), null), message);
    }

    public int getLine() {
        return this.getLocation().getStartPosition().lineNumber();
    }

    public int getColumn() {
        return this.getLocation().getStartPosition().columnNumber();
    }
}
