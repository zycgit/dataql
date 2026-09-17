/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.parser.location;
/**
 * AST 和代码文本的位置关系
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-06-11
 */
public interface Location {
    CodeLocation getStartPosition();

    CodeLocation getEndPosition();

    void setStartPosition(CodeLocation codeLocation);

    void setEndPosition(CodeLocation codeLocation);
}
