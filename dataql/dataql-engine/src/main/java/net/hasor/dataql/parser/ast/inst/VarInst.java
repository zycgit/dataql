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
import net.hasor.dataql.parser.ast.*;
import net.hasor.dataql.parser.ast.token.StringToken;
import net.hasor.dataql.parser.ast.value.LambdaVariable;
import net.hasor.dataql.parser.location.BlockLocation;

/**
 * var指令
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class VarInst extends BlockLocation implements Inst {
    private final StringToken varName; //变量名
    private final Variable    value;   //变量表达式

    public VarInst(StringToken varName, Variable value) {
        this.varName = varName;
        this.value = value;
    }

    public StringToken getVarName() {
        return varName;
    }

    public Variable getValue() {
        return value;
    }

    @Override
    public void accept(AstVisitor astVisitor) {
        astVisitor.visitInst(new InstVisitorContext(this) {
            @Override
            public void visitChildren(AstVisitor astVisitor) {
                value.accept(astVisitor);
            }
        });
    }

    @Override
    public void doFormat(int depth, Hints formatOption, FormatWriter writer) throws IOException {
        String fixedString = StringUtils.repeat(' ', depth * fixedLength);
        //
        writer.write(fixedString + String.format("var %s = ", this.varName.getValue()));
        this.value.doFormat(depth, formatOption, writer);
        if (this.value instanceof LambdaVariable) {
            return;
        }
        writer.write(";\n");
    }
}
