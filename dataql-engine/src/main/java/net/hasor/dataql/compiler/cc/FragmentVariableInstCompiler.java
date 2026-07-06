/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.dataql.compiler.cc;
import net.hasor.dataql.compiler.qil.CompilerContext;
import net.hasor.dataql.compiler.qil.InstCompiler;
import net.hasor.dataql.compiler.qil.InstQueue;
import net.hasor.dataql.parser.ast.value.FragmentVariable;
import net.hasor.dataql.parser.ast.value.FragmentVariable.FragmentParam;
import static net.hasor.dataql.compiler.qil.CompilerContext.ContainsIndex;

/**
 * Fragment 片段（支持 `@@type(name=val, ...)<% body %>` 语法）。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class FragmentVariableInstCompiler implements InstCompiler<FragmentVariable> {
    @Override
    public void doCompiler(FragmentVariable astInst, InstQueue queue, CompilerContext compilerContext) {
        InstQueue newMethodInst = queue.newMethodInst();
        boolean isBatch = astInst.isBatchMode();
        compilerContext.newFrame();

        // 1. LOCAL 变量表：声明所有参数名
        for (FragmentParam param : astInst.getParamList()) {
            String name = param.name().getValue();
            int index = compilerContext.push(name); //将变量名压栈，并返回栈中的位置
            instLocation(newMethodInst, param.name());
            newMethodInst.inst(LOCAL, index, name); //为栈中某个位置的变量命名
        }

        // 2. M_FRAG 片段入口
        instLocation(newMethodInst, astInst.getFragmentName());
        newMethodInst.inst(M_FRAG, isBatch, astInst.getFragmentName().getValue());

        // 3. 构建 params map
        newMethodInst.inst(NEW_O);
        for (FragmentParam param : astInst.getParamList()) {
            String name = param.name().getValue();
            if (param.hasValue()) {
                // @@type(name = value) — 编译 value 表达式
                compilerContext.findInstCompilerByInst(param.value()).doCompiler(queue);
                newMethodInst.inst(PUT, name);
            } else {
                // @@type(name) — 从外层作用域加载同名变量
                ContainsIndex index = compilerContext.containsWithTree(name);
                instLocation(newMethodInst, param.name());
                newMethodInst.inst(LOAD, index.depth, index.index);
                newMethodInst.inst(PUT, name);
            }
        }

        // 4. 片段内容作为最后一个参数
        instLocation(newMethodInst, astInst.getFragmentString());
        newMethodInst.inst(LDC_S, astInst.getFragmentString().getValue());

        // 5. 调用 FragmentProcess
        instLocation(newMethodInst, astInst);
        newMethodInst.inst(CALL, 2);
        newMethodInst.inst(RETURN, 0);
        compilerContext.dropFrame();

        // 6. M_REF 指向函数
        instLocation(queue, astInst);
        queue.inst(M_REF, newMethodInst.getName());
    }
}
