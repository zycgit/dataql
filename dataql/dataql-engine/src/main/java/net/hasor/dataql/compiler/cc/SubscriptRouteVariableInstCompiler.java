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
import net.hasor.dataql.parser.ast.Expression;
import net.hasor.dataql.parser.ast.token.StringToken;
import net.hasor.dataql.parser.ast.value.SubscriptRouteVariable;
import net.hasor.dataql.parser.ast.value.SubscriptRouteVariable.SubType;

/**
 * 对 RouteVariable 的下标操作
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class SubscriptRouteVariableInstCompiler implements InstCompiler<SubscriptRouteVariable> {
    @Override
    public void doCompiler(SubscriptRouteVariable astInst, InstQueue queue, CompilerContext compilerContext) {
        compilerContext.findInstCompilerByInst(astInst.getParent()).doCompiler(queue);
        //
        SubType subType = astInst.getSubType();
        if (subType == SubType.String) {
            StringToken subValue = astInst.getSubValue();
            instLocation(queue, subValue);
            queue.inst(GET, subValue.getValue());
        }
        if (subType == SubType.Integer) {
            StringToken subValue = astInst.getSubValue();
            instLocation(queue, subValue);
            queue.inst(PULL, Integer.parseInt(subValue.getValue()));
        }
        if (subType == SubType.Expr) {
            Label nextLabel = null;
            Label finalLabel = queue.labelDef();
            //
            Expression exprValue = astInst.getExprValue();
            compilerContext.findInstCompilerByInst(exprValue).doCompiler(queue);
            instLocation(queue, exprValue);
            queue.inst(COPY);   // 表达式值Copy 一份用来计算 typeof
            queue.inst(TYPEOF);
            queue.inst(COPY);   // typeof 的值Copy 一份用来做两次 if 判断
            //
            nextLabel = queue.labelDef();
            queue.inst(LDC_S, TypeOfEnum.String.typeCode());
            queue.inst(DO, "==");
            queue.inst(IF, nextLabel);
            queue.inst(POP);
            queue.inst(GET);
            queue.inst(GOTO, finalLabel);
            queue.inst(LABEL, nextLabel);
            //
            nextLabel = queue.labelDef();
            queue.inst(LDC_S, TypeOfEnum.Number.typeCode());
            queue.inst(DO, "==");
            queue.inst(IF, nextLabel);
            queue.inst(PULL);
            queue.inst(GOTO, finalLabel);
            queue.inst(LABEL, nextLabel);
            //
            queue.inst(LDC_S, "type is not string or number.");
            queue.inst(THROW, 500);
            queue.inst(LABEL, finalLabel);
        }
    }
}
