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
import net.hasor.dataql.parser.ast.inst.HintInst;
import net.hasor.dataql.parser.ast.token.StringToken;
import net.hasor.dataql.parser.ast.value.PrimitiveVariable;

/**
 * 查询选项
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class HintInstCompiler implements InstCompiler<HintInst> {
    @Override
    public void doCompiler(HintInst astInst, InstQueue queue, CompilerContext compilerContext) {
        if (!astInst.hasValue()) {
            return;
        }
        StringToken instHint = astInst.getHint();
        instLocation(queue, instHint);
        queue.inst(LDC_S, instHint.getValue());
        //
        PrimitiveVariable hintValue = astInst.getValue();
        compilerContext.findInstCompilerByInst(hintValue).doCompiler(queue);
        //
        instLocation(queue, astInst);
        queue.inst(HINT);
    }
}
