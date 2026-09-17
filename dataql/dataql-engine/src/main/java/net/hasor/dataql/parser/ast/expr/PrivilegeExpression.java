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
 * 权限提升，用于表示表达式中的括号
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class PrivilegeExpression extends BlockLocation implements Expression {
    private final Expression expression;

    public PrivilegeExpression(Expression expression) {
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return "( " + this.expression.toString() + " )";
    }

    @Override
    public void accept(AstVisitor astVisitor) {
        astVisitor.visitInst(new InstVisitorContext(this) {
            @Override
            public void visitChildren(AstVisitor astVisitor) {
                expression.accept(astVisitor);
            }
        });
    }

    @Override
    public void doFormat(int depth, Hints formatOption, FormatWriter writer) throws IOException {
        if (this.expression instanceof PrivilegeExpression) {
            this.expression.doFormat(depth, formatOption, writer);
        } else {
            writer.write("(");
            this.expression.doFormat(depth, formatOption, writer);
            writer.write(")");
        }
    }
}
