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
import net.hasor.dataql.parser.location.Location;

/**
 * 二元运算表达式
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class DyadicExpression extends BlockLocation implements Expression {
    private final Expression  fstExpression;   //第一个表达式
    private final SymbolToken symbolToken;     //运算符
    private final Expression  secExpression;   //第二个表达式

    public DyadicExpression(Expression fstExpression, SymbolToken symbolToken, Expression secExpression) {
        this.fstExpression = fstExpression;
        this.symbolToken = symbolToken;
        this.secExpression = secExpression;
    }

    public Location expressCodeLocation() {
        BlockLocation codeLocation = new BlockLocation();
        codeLocation.setStartPosition(this.fstExpression.getStartPosition());
        codeLocation.setEndPosition(this.secExpression.getEndPosition());
        return codeLocation;
    }

    public Expression getFstExpression() {
        return fstExpression;
    }

    public SymbolToken getDyadicSymbol() {
        return symbolToken;
    }

    public Expression getSecExpression() {
        return secExpression;
    }

    @Override
    public void accept(AstVisitor astVisitor) {
        astVisitor.visitInst(new InstVisitorContext(this) {
            @Override
            public void visitChildren(AstVisitor astVisitor) {
                fstExpression.accept(astVisitor);
                secExpression.accept(astVisitor);
            }
        });
    }

    @Override
    public void doFormat(int depth, Hints formatOption, FormatWriter writer) throws IOException {
        this.fstExpression.doFormat(depth, formatOption, writer);
        writer.write(" " + symbolToken.getSymbol() + " ");
        this.secExpression.doFormat(depth, formatOption, writer);
    }
}
