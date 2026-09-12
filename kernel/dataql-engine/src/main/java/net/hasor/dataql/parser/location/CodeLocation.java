/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.parser.location;
/**
 * 具体到行/列到位置
 * @param lineNumber 代码行号
 * @param columnNumber 代码行的第几个字符
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-06-11
 */
public record CodeLocation(int lineNumber, int columnNumber) {
    public CodeLocation() {
        this(-1, -1);
    }

    @Override
    public String toString() {
        if (lineNumber <= 0 && columnNumber < 0) {
            return "Unknown";
        }
        String lineNumStr = lineNumber >= 0 ? String.valueOf(lineNumber) : "Unknown";
        String columnNumStr = columnNumber >= 0 ? String.valueOf(columnNumber) : "Unknown";
        if ("Unknown".equalsIgnoreCase(columnNumStr)) {
            return lineNumStr;
        } else {
            return lineNumStr + ":" + columnNumStr;
        }
    }
}
