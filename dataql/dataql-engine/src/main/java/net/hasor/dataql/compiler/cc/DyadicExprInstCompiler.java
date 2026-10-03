/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.compiler.cc;
import net.hasor.dataql.compiler.qil.CompilerContext;
import net.hasor.dataql.compiler.qil.InstCompiler;
import net.hasor.dataql.compiler.qil.InstQueue;
import net.hasor.dataql.compiler.qil.Label;
import net.hasor.dataql.parser.ast.expr.DyadicExpression;
import net.hasor.dataql.parser.ast.token.SymbolToken;

/** Compiles binary expressions in the evaluation order defined by the AST. */
public class DyadicExprInstCompiler implements InstCompiler<DyadicExpression> {
    @Override
    public void doCompiler(DyadicExpression astInst, InstQueue queue, CompilerContext compilerContext) {
        SymbolToken symbol = astInst.getDyadicSymbol();
        compilerContext.findInstCompilerByInst(astInst.getFstExpression()).doCompiler(queue);
        if ("&&".equals(symbol.getSymbol()) || "||".equals(symbol.getSymbol())) {
            this.compileLogical(astInst, queue, compilerContext);
        } else {
            compilerContext.findInstCompilerByInst(astInst.getSecExpression()).doCompiler(queue);
            this.instLocation(queue, symbol);
            queue.inst(DO, symbol.getSymbol());
        }
    }

    private void compileLogical(DyadicExpression astInst, InstQueue queue, CompilerContext compilerContext) {
        SymbolToken symbol = astInst.getDyadicSymbol();
        Label end = queue.labelDef();
        this.instLocation(queue, symbol);
        queue.inst(COPY);
        // Negation checks the Boolean operand before deciding whether to evaluate the right side.
        queue.inst(UO, "!");
        if ("&&".equals(symbol.getSymbol())) {
            queue.inst(UO, "!");
        }
        queue.inst(IF, end);
        compilerContext.findInstCompilerByInst(astInst.getSecExpression()).doCompiler(queue);
        this.instLocation(queue, symbol);
        queue.inst(DO, symbol.getSymbol());
        queue.inst(LABEL, end);
    }
}
