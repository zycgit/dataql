/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.parser.ast.token;
import net.hasor.dataql.parser.location.BlockLocation;

/**
 * 表示一个 操作符
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-06-11
 */
public class SymbolToken extends BlockLocation {
    private final String symbol;

    public SymbolToken(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return this.symbol;
    }
}
