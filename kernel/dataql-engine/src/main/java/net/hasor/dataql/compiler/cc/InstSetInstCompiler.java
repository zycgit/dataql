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
import net.hasor.dataql.parser.ast.Inst;
import net.hasor.dataql.parser.ast.inst.HintInst;
import net.hasor.dataql.parser.ast.inst.InstSet;

/**
 * 指令序列
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class InstSetInstCompiler implements InstCompiler<InstSet> {
    @Override
    public void doCompiler(InstSet astInst, InstQueue queue, CompilerContext compilerContext) {
        boolean multipleInst = astInst.isMultipleInst();
        if (multipleInst) {
            queue.inst(HINT_S);
            List<HintInst> optionSet = astInst.getOptionSet();
            for (HintInst inst : optionSet) {
                compilerContext.findInstCompilerByInst(inst).doCompiler(queue);
            }
        }
        for (Inst inst : astInst) {
            compilerContext.findInstCompilerByInst(inst).doCompiler(queue);
        }
        if (multipleInst) {
            queue.inst(HINT_D);
        }
    }
}
