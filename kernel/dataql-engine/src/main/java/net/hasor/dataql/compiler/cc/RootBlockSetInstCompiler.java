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
import net.hasor.dataql.parser.ast.inst.ImportInst;
import net.hasor.dataql.parser.ast.inst.RootBlockSet;

/**
 * 指令序列
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2019-11-07
 */
public class RootBlockSetInstCompiler implements InstCompiler<RootBlockSet> {
    @Override
    public void doCompiler(RootBlockSet rootBlockSet, InstQueue queue, CompilerContext compilerContext) {
        List<HintInst> optionSet = rootBlockSet.getOptionSet();
        if (optionSet != null) {
            for (HintInst hintInst : optionSet) {
                compilerContext.findInstCompilerByInst(hintInst).doCompiler(queue);
            }
        }
        List<ImportInst> importSet = rootBlockSet.getImportSet();
        if (importSet != null) {
            for (ImportInst importInst : importSet) {
                compilerContext.findInstCompilerByInst(importInst).doCompiler(queue);
            }
        }
        if (!rootBlockSet.isEmpty()) {
            for (Inst inst : rootBlockSet) {
                compilerContext.findInstCompilerByInst(inst).doCompiler(queue);
            }
        }
    }
}
