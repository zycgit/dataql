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
import net.hasor.dataql.compiler.qil.Opcodes;
import net.hasor.dataql.parser.ast.expr.UnaryExpression;
import net.hasor.dataql.parser.ast.token.SymbolToken;

/**
 * 一元运算表达式
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class UnaryExprInstCompiler implements InstCompiler<UnaryExpression> {
    @Override
    public void doCompiler(UnaryExpression astInst, InstQueue queue, CompilerContext compilerContext) {
        compilerContext.findInstCompilerByInst(astInst.getTarget()).doCompiler(queue);
        //
        SymbolToken dyadicSymbol = astInst.getDyadicSymbol();
        this.instLocation(queue, dyadicSymbol);
        queue.inst(Opcodes.UO, dyadicSymbol.getSymbol());
    }
}
