/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.compiler.cc;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.compiler.qil.CompilerContext;
import net.hasor.dataql.compiler.qil.CompilerContext.ContainsIndex;
import net.hasor.dataql.compiler.qil.InstCompiler;
import net.hasor.dataql.compiler.qil.InstQueue;
import net.hasor.dataql.parser.ast.RouteVariable;
import net.hasor.dataql.parser.ast.token.StringToken;
import net.hasor.dataql.parser.ast.value.EnterRouteVariable;
import net.hasor.dataql.parser.ast.value.EnterRouteVariable.RouteType;
import net.hasor.dataql.parser.ast.value.NameRouteVariable;

/**
 * 编译 NameRouteVariable
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class NameRouteVariableInstCompiler implements InstCompiler<NameRouteVariable> {
    @Override
    public void doCompiler(NameRouteVariable astInst, InstQueue queue, CompilerContext compilerContext) {
        StringToken nameRouteToken = astInst.getName();
        RouteVariable parent = astInst.getParent();
        if (parent instanceof NameRouteVariable) {
            if (StringUtils.isBlank(((NameRouteVariable) parent).getName().getValue())) {
                parent = parent.getParent();
            }
        }
        if (parent instanceof EnterRouteVariable enterParent) {
            if (enterParent.getRouteType() == RouteType.Expr) {
                ContainsIndex withTree = compilerContext.containsWithTree(nameRouteToken.getValue());
                if (withTree.isValid()) {
                    this.instLocation(queue, nameRouteToken);
                    queue.inst(LOAD, withTree.depth, withTree.index);
                    return;
                }
            }
        }
        //
        compilerContext.findInstCompilerByInst(parent).doCompiler(queue);
        this.instLocation(queue, nameRouteToken);
        queue.inst(GET, nameRouteToken.getValue());
    }
}
