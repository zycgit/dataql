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
import net.hasor.dataql.parser.ast.RouteVariable;
import net.hasor.dataql.parser.ast.Variable;
import net.hasor.dataql.parser.ast.value.FunCallRouteVariable;

/**
 * 函数调用
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class FunCallRouteVariableInstCompiler implements InstCompiler<FunCallRouteVariable> {
    @Override
    public void doCompiler(FunCallRouteVariable astInst, InstQueue queue, CompilerContext compilerContext) {
        //
        RouteVariable enter = astInst.getParent();
        compilerContext.findInstCompilerByInst(enter).doCompiler(queue);
        //
        // .声明当前栈顶元素为函数入口
        instLocation(queue, astInst);
        queue.inst(M_DEF);
        //
        // .输出参数
        List<Variable> paramList = astInst.getParamList();
        for (Variable var : paramList) {
            compilerContext.findInstCompilerByInst(var).doCompiler(queue);
        }
        // .执行函数调用
        instLocation(queue, astInst);
        queue.inst(CALL, paramList.size());
    }
}
