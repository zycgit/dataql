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
import net.hasor.dataql.parser.ast.inst.ExitInst;

/**
 * exit指令
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class ExitInstCompiler implements InstCompiler<ExitInst> {
    @Override
    public void doCompiler(ExitInst astInst, InstQueue queue, CompilerContext compilerContext) {
        Variable dataValue = astInst.getExitData();
        compilerContext.findInstCompilerByInst(dataValue).doCompiler(queue);
        //
        this.instLocation(queue, astInst);
        queue.inst(EXIT, astInst.getExitCode().getValue());
    }
}
