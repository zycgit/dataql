/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.parser;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.parser.ast.Visitor;

/**
 * 查询模型 -> Data QL 的 AST Tree
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public interface QueryModel extends Visitor {
    default String toQueryString() throws IOException {
        StringWriter stringWriter = new StringWriter();
        this.toQueryString(stringWriter);
        return stringWriter.toString();
    }

    default void toQueryString(Writer writer) throws IOException {
        this.toQueryString(new HintsSet(), writer);
    }

    void toQueryString(HintsSet formatOptions, Writer writer) throws IOException;
}
