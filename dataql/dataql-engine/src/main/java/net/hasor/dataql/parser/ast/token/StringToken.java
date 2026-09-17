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
 * 表示一个 数字符串
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-06-11
 */
public class StringToken extends BlockLocation {
    private final String value;

    public StringToken(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
