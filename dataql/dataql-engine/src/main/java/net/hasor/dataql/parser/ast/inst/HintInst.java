/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.parser.ast.inst;
import java.io.IOException;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.parser.ast.AstVisitor;
import net.hasor.dataql.parser.ast.FormatWriter;
import net.hasor.dataql.parser.ast.Inst;
import net.hasor.dataql.parser.ast.InstVisitorContext;
import net.hasor.dataql.parser.ast.token.StringToken;
import net.hasor.dataql.parser.ast.value.PrimitiveVariable;
import net.hasor.dataql.parser.location.BlockLocation;

/**
 * 查询选项
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class HintInst extends BlockLocation implements Inst {
    private final StringToken       hint;
    private final PrimitiveVariable value;

    public HintInst(StringToken hint, PrimitiveVariable value) {
        this.hint = hint;
        this.value = value;
    }

    public StringToken getHint() {
        return hint;
    }

    public PrimitiveVariable getValue() {
        return value;
    }

    public boolean hasValue() {
        return this.value != null;
    }

    @Override
    public void accept(AstVisitor astVisitor) {
        astVisitor.visitInst(new InstVisitorContext(this) {
            @Override
            public void visitChildren(AstVisitor astVisitor) {
                if (value != null) {
                    value.accept(astVisitor);
                }
            }
        });
    }

    @Override
    public void doFormat(int depth, Hints formatOption, FormatWriter writer) throws IOException {
        String fixedString = StringUtils.repeat(' ', depth * fixedLength);
        writer.write(fixedString + "hint " + this.hint.getValue());
        if (this.value != null) {
            writer.write(" = ");
            this.value.doFormat(depth + 1, formatOption, writer);
        }
        writer.write(";\n");
    }
}
