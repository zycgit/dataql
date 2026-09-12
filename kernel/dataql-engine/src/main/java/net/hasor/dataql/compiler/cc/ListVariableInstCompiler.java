/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.compiler.cc;
import java.util.List;
import net.hasor.dataql.compiler.qil.CompilerContext;
import net.hasor.dataql.compiler.qil.InstCompiler;
import net.hasor.dataql.compiler.qil.InstQueue;
import net.hasor.dataql.parser.ast.Variable;
import net.hasor.dataql.parser.ast.value.ListVariable;

/**
 * 列表
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class ListVariableInstCompiler implements InstCompiler<ListVariable> {
    @Override
    public void doCompiler(ListVariable astInst, InstQueue queue, CompilerContext compilerContext) {
        instLocation(queue, astInst);
        queue.inst(NEW_A);
        List<Variable> varList = astInst.getExpressionList();
        for (Variable var : varList) {
            compilerContext.findInstCompilerByInst(var).doCompiler(queue);
            instLocation(queue, var);
            queue.inst(PUSH);
        }
    }
}
