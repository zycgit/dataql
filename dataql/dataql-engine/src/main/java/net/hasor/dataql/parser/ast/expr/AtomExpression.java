/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.parser.ast.expr;
import java.io.IOException;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.parser.ast.*;
import net.hasor.dataql.parser.location.BlockLocation;

/**
 * Variable 类型的 Expression 形态
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2019-11-07
 */
public class AtomExpression extends BlockLocation implements Expression {
    private final Variable variableExpression; // 把值类型转换为表达式

    public AtomExpression(Variable variableExpression) {
        this.variableExpression = variableExpression;
    }

    public Variable getVariableExpression() {
        return variableExpression;
    }

    @Override
    public void accept(AstVisitor astVisitor) {
        astVisitor.visitInst(new InstVisitorContext(this) {
            @Override
            public void visitChildren(AstVisitor astVisitor) {
                variableExpression.accept(astVisitor);
            }
        });
    }

    @Override
    public void doFormat(int depth, Hints formatOption, FormatWriter writer) throws IOException {
        this.variableExpression.doFormat(depth, formatOption, writer);
    }
}
