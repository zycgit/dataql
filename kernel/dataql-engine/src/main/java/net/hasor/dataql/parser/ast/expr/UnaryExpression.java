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
import net.hasor.dataql.parser.ast.token.SymbolToken;
import net.hasor.dataql.parser.location.BlockLocation;

/**
 * 一元运算表达式
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class UnaryExpression extends BlockLocation implements Expression {
    private final Expression  target;      //表达式
    private final SymbolToken symbolToken;//操作符

    public UnaryExpression(Expression target, SymbolToken symbolToken) {
        this.target = target;
        this.symbolToken = symbolToken;
    }

    public Expression getTarget() {
        return target;
    }

    public SymbolToken getDyadicSymbol() {
        return symbolToken;
    }

    @Override
    public void accept(AstVisitor astVisitor) {
        astVisitor.visitInst(new InstVisitorContext(this) {
            @Override
            public void visitChildren(AstVisitor astVisitor) {
                target.accept(astVisitor);
            }
        });
    }

    @Override
    public void doFormat(int depth, Hints formatOption, FormatWriter writer) throws IOException {
        writer.write(this.symbolToken.getSymbol());
        this.target.doFormat(depth, formatOption, writer);
    }
}
