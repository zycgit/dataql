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
import net.hasor.dataql.parser.ast.Variable;
import net.hasor.dataql.parser.ast.inst.VarInst;
import net.hasor.dataql.parser.ast.token.StringToken;

/**
 * var指令
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class VarInstCompiler implements InstCompiler<VarInst> {
    @Override
    public void doCompiler(VarInst astInst, InstQueue queue, CompilerContext compilerContext) {
        // .如果当前堆栈中存在该变量的定义，那么直接覆盖。否则新增一个本地变量
        StringToken varNameToken = astInst.getVarName();
        String varName = varNameToken.getValue();
        int index = compilerContext.containsWithCurrent(varName);
        if (index < 0) {
            index = compilerContext.push(varName);
        }
        //
        // .编译表达式
        Variable varValue = astInst.getValue();
        compilerContext.findInstCompilerByInst(varValue).doCompiler(queue);
        //
        instLocation(queue, varNameToken);
        queue.inst(STORE, index);
    }
}
