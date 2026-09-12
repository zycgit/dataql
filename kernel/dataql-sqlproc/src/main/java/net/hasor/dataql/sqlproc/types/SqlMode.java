/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.types;
/**
 * 参数模式
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-05-24
 */
public enum SqlMode {
    In(true, false),
    Out(false, true),
    Cursor(false, true),
    InOut(true, true);

    private final boolean out;
    private final boolean in;

    SqlMode(boolean in, boolean out) {
        this.in = in;
        this.out = out;
    }

    public boolean isIn() {
        return this.in;
    }

    public boolean isOut() {
        return this.out;
    }
}
