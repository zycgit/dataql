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
import net.hasor.dataql.parser.ast.AstVisitor;
import net.hasor.dataql.parser.ast.Expression;
import net.hasor.dataql.parser.ast.FormatWriter;
import net.hasor.dataql.parser.ast.InstVisitorContext;
import net.hasor.dataql.parser.location.BlockLocation;

/**
 * 三元运算表达式
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class TernaryExpression extends BlockLocation implements Expression {
    private final Expression testExpression;  //三元运算符，条件表达式
    private final Expression thenExpression;  //第一个表达式
    private final Expression elseExpression;  //第二个表达式

    public TernaryExpression(Expression testExp, Expression thenExp, Expression elseExp) {
        this.testExpression = testExp;
        this.thenExpression = thenExp;
        this.elseExpression = elseExp;
    }

    public Expression getTestExpression() {
        return testExpression;
    }

    public Expression getThenExpression() {
        return thenExpression;
    }

    public Expression getElseExpression() {
        return elseExpression;
    }

    @Override
    public void accept(AstVisitor astVisitor) {
        astVisitor.visitInst(new InstVisitorContext(this) {
            @Override
            public void visitChildren(AstVisitor astVisitor) {
                testExpression.accept(astVisitor);
                thenExpression.accept(astVisitor);
                elseExpression.accept(astVisitor);
            }
        });
    }

    @Override
    public void doFormat(int depth, Hints formatOption, FormatWriter writer) throws IOException {
        this.testExpression.doFormat(depth, formatOption, writer);
        writer.write(" ? ");
        this.thenExpression.doFormat(depth, formatOption, writer);
        writer.write(" : ");
        this.elseExpression.doFormat(depth, formatOption, writer);
    }
}
