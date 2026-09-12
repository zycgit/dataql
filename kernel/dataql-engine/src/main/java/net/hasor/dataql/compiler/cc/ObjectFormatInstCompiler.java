/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.compiler.cc;
import java.util.List;
import java.util.Map;
import net.hasor.dataql.compiler.qil.CompilerContext;
import net.hasor.dataql.compiler.qil.InstCompiler;
import net.hasor.dataql.compiler.qil.InstQueue;
import net.hasor.dataql.parser.ast.RouteVariable;
import net.hasor.dataql.parser.ast.Variable;
import net.hasor.dataql.parser.ast.fmt.ObjectFormat;
import net.hasor.dataql.parser.ast.token.StringToken;
import net.hasor.dataql.parser.ast.value.ObjectVariable;

/**
 * 函数调用的返回值处理格式，Object格式。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class ObjectFormatInstCompiler implements InstCompiler<ObjectFormat> {
    @Override
    public void doCompiler(ObjectFormat astInst, InstQueue queue, CompilerContext compilerContext) {
        // .特殊情况优化处理
        if (astInst.getFormatTo().getFieldSort().isEmpty()) {
            RouteVariable formVariable = astInst.getForm();
            compilerContext.findInstCompilerByInst(formVariable).doCompiler(queue);
            instLocation(queue, astInst);
            queue.inst(POP);    // 丢弃表达式结果
            queue.inst(NEW_O);  // 构造对象
            return;
        }
        //
        // .编译表达式
        RouteVariable formVariable = astInst.getForm();
        compilerContext.findInstCompilerByInst(formVariable).doCompiler(queue);
        instLocation(queue, astInst);
        queue.inst(CAST_O);
        queue.inst(E_PUSH);
        {
            queue.inst(NEW_O);
            ObjectVariable formatTo = astInst.getFormatTo();
            Map<String, StringToken> keyObjects = formatTo.getObjectKeys();
            List<String> keyFields = formatTo.getFieldSort();
            Map<String, Variable> objectData = formatTo.getObjectValues();
            for (String fieldKey : keyFields) {
                StringToken keyVal = keyObjects.get(fieldKey);
                Variable variable = objectData.get(fieldKey);
                compilerContext.findInstCompilerByInst(variable).doCompiler(queue);
                instLocation(queue, keyVal);
                queue.inst(PUT, fieldKey);
            }
        }
        queue.inst(E_POP);
    }
}
