/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.parser.location;
/**
 * 代码文本块的位置
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-06-11
 */
public class BlockLocation implements Location {
    private CodeLocation startPosition;
    private CodeLocation endPosition;

    public CodeLocation getStartPosition() {
        return this.startPosition;
    }

    public CodeLocation getEndPosition() {
        return this.endPosition;
    }

    public void setStartPosition(CodeLocation codeLocation) {
        this.startPosition = codeLocation;
    }

    public void setEndPosition(CodeLocation codeLocation) {
        this.endPosition = codeLocation;
    }

    @Override
    public String toString() {
        String starStr = getStartPosition().toString();
        String endStr = getEndPosition().toString();
        if ("Unknown".equalsIgnoreCase(starStr) && "Unknown".equalsIgnoreCase(endStr)) {
            return "Unknown";
        }
        if ("Unknown".equalsIgnoreCase(endStr)) {
            return "line " + starStr;
        } else {
            return "line " + starStr + "~" + endStr;
        }
    }
}
