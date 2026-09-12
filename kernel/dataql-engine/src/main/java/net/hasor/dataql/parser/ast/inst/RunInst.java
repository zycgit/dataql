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
import net.hasor.dataql.parser.ast.value.LambdaVariable;
import net.hasor.dataql.parser.location.BlockLocation;

/**
 * run指令
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class RunInst extends BlockLocation implements Inst {
    private final Variable value;   //执行表达式

    public RunInst(Variable value) {
        this.value = value;
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
        writer.write(fixedString + "run ");
        this.value.doFormat(depth, formatOption, writer);
        writer.write((this.value instanceof LambdaVariable) ? "\n" : ";\n");
    }
}
