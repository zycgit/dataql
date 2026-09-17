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
import net.hasor.dataql.parser.ast.expr.AtomExpression;

/**
 * Atom
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class AtomExprInstCompiler implements InstCompiler<AtomExpression> {
    @Override
    public void doCompiler(AtomExpression astInst, InstQueue queue, CompilerContext compilerContext) {
        compilerContext.findInstCompilerByInst(astInst.getVariableExpression()).doCompiler(queue);
    }
}
