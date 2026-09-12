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
import net.hasor.dataql.parser.ast.inst.InstSet;
import net.hasor.dataql.parser.ast.token.StringToken;
import net.hasor.dataql.parser.ast.value.LambdaVariable;

/**
 * lambda 函数对象
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class LambdaVariableInstCompiler implements InstCompiler<LambdaVariable> {
    @Override
    public void doCompiler(LambdaVariable astInst, InstQueue queue, CompilerContext compilerContext) {
        //
        // .声明函数参数的变量位置
        List<StringToken> paramList = astInst.getParamList();
        InstQueue newMethodInst = queue.newMethodInst();
        compilerContext.newFrame();
        for (int i = 0; i < paramList.size(); i++) {
            StringToken nameToken = paramList.get(i);
            String name = nameToken.getValue();
            int index = compilerContext.push(name);//将变量名压栈，并返回栈中的位置
            instLocation(newMethodInst, nameToken);
            newMethodInst.inst(LOCAL, i, index, name);  //为栈中某个位置的变量命名
        }
        compilerContext.findInstCompilerByInst(astInst, InstSet.class).doCompiler(newMethodInst);
        compilerContext.dropFrame();
        //
        // .指向函数的指针
        instLocation(queue, astInst);
        queue.inst(M_REF, newMethodInst.getName());
    }
}
