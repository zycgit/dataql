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
import net.hasor.dataql.parser.ast.Expression;
import net.hasor.dataql.parser.ast.expr.TernaryExpression;

/**
 * 三元运算表达式
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class TernaryExprInstCompiler implements InstCompiler<TernaryExpression> {
    @Override
    public void doCompiler(TernaryExpression astInst, InstQueue queue, CompilerContext compilerContext) {
        Expression testExpr = astInst.getTestExpression();
        Expression thenExpr = astInst.getThenExpression();
        Expression elseExpr = astInst.getElseExpression();
        Label elseLabel = queue.labelDef();
        Label endLabel = queue.labelDef();
        //
        // .测试表达式
        compilerContext.findInstCompilerByInst(testExpr).doCompiler(queue);
        instLocation(queue, testExpr.expressCodeLocation());
        queue.inst(IF, elseLabel);//如果判断失败，执行第二个表达式
        //
        // .第一个表达式
        compilerContext.findInstCompilerByInst(thenExpr).doCompiler(queue);
        queue.inst(GOTO, endLabel);//第一个表达式执行成功的话就跳转到 end
        //
        // .第二个表达式
        queue.inst(LABEL, elseLabel);
        compilerContext.findInstCompilerByInst(elseExpr).doCompiler(queue);
        queue.inst(LABEL, endLabel);
    }
}
