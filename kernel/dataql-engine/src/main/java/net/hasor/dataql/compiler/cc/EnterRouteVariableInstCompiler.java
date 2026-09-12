/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.compiler.cc;
import net.hasor.dataql.compiler.QueryCompilerException;
import net.hasor.dataql.compiler.qil.CompilerContext;
import net.hasor.dataql.compiler.qil.InstCompiler;
import net.hasor.dataql.compiler.qil.InstQueue;
import net.hasor.dataql.parser.ast.value.EnterRouteVariable;
import net.hasor.dataql.parser.ast.value.EnterRouteVariable.RouteType;
import net.hasor.dataql.parser.ast.value.EnterRouteVariable.SpecialType;

/**
 * 路由的入口，一切路由操作都要有一个入口
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class EnterRouteVariableInstCompiler implements InstCompiler<EnterRouteVariable> {
    @Override
    public void doCompiler(EnterRouteVariable astInst, InstQueue queue, CompilerContext compilerContext) {
        RouteType routeType = astInst.getRouteType();
        SpecialType specialType = astInst.getSpecialType();
        this.instLocation(queue, astInst);//行号
        //
        // 表达式
        if (routeType == RouteType.Expr) {
            specialType = (specialType == null) ? SpecialType.Special_A : specialType;
            queue.inst(E_LOAD, specialType.getCode());
            return;
        }
        //
        // 程序传参
        if (routeType == RouteType.Params) {
            queue.inst(LOAD_C, specialType.getCode());
            return;
        }
        throw new QueryCompilerException("routeType is null.");
    }
}
