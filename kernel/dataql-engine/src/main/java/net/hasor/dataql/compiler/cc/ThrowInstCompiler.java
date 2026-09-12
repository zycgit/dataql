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
import net.hasor.dataql.parser.ast.inst.ThrowInst;

/**
 * throw指令
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class ThrowInstCompiler implements InstCompiler<ThrowInst> {
    @Override
    public void doCompiler(ThrowInst astInst, InstQueue queue, CompilerContext compilerContext) {
        Variable dataValue = astInst.getThrowData();
        compilerContext.findInstCompilerByInst(dataValue).doCompiler(queue);
        //
        this.instLocation(queue, astInst);
        queue.inst(THROW, astInst.getErrorCode().getValue());
    }
}
