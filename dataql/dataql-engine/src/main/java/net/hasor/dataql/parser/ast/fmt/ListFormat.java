/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.parser.ast.fmt;
import java.io.IOException;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.parser.ast.*;
import net.hasor.dataql.parser.ast.value.ListVariable;
import net.hasor.dataql.parser.location.BlockLocation;

/**
 * 函数调用的返回值处理格式，List格式。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class ListFormat extends BlockLocation implements Inst, Variable {
    private final RouteVariable form;
    private final ListVariable  formatTo;

    public ListFormat(RouteVariable form, ListVariable formatTo) {
        this.form = form;
        this.formatTo = formatTo;
    }

    public RouteVariable getForm() {
        return form;
    }

    public ListVariable getFormatTo() {
        return formatTo;
    }

    @Override
    public void accept(AstVisitor astVisitor) {
        astVisitor.visitInst(new InstVisitorContext(this) {
            @Override
            public void visitChildren(AstVisitor astVisitor) {
                form.accept(astVisitor);
                formatTo.accept(astVisitor);
            }
        });
    }

    @Override
    public void doFormat(int depth, Hints formatOption, FormatWriter writer) throws IOException {
        this.form.doFormat(depth, formatOption, writer);
        writer.write(" => ");
        this.formatTo.doFormat(depth, formatOption, writer);
    }
}
