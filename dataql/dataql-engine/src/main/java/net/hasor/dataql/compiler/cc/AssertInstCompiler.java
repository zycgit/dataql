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
import net.hasor.dataql.domain.TypeOfEnum;
import net.hasor.dataql.parser.ast.Variable;
import net.hasor.dataql.parser.ast.inst.AssertInst;

/**
 * Assert
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class AssertInstCompiler implements InstCompiler<AssertInst> {
    @Override
    public void doCompiler(AssertInst astInst, InstQueue queue, CompilerContext compilerContext) {
        Variable varValue = astInst.getValue();
        compilerContext.findInstCompilerByInst(varValue).doCompiler(queue);
        instLocation(queue, varValue);
        Label finalLabel = queue.labelDef();// 总出口 Label
        //
        // .数据类型判断
        Label nextLabel = queue.labelDef();
        queue.inst(COPY);   // 表达式值Copy 一份用来计算 typeof
        queue.inst(TYPEOF);
        queue.inst(LDC_S, TypeOfEnum.Boolean.typeCode());
        queue.inst(DO, "!=");
        queue.inst(IF, nextLabel);
        queue.inst(POP);
        queue.inst(LDC_S, "assert expression value is not 'boolean' type.");
        queue.inst(THROW, 500);
        queue.inst(GOTO, finalLabel);
        queue.inst(LABEL, nextLabel);
        //
        // .判断断言
        nextLabel = queue.labelDef();
        queue.inst(IF, nextLabel);
        queue.inst(GOTO, finalLabel);
        queue.inst(LABEL, nextLabel);
        queue.inst(LDC_S, "assert test failed.");
        queue.inst(THROW, 500);
        //
        // .if 的结束点
        queue.inst(LABEL, finalLabel);
    }
}
