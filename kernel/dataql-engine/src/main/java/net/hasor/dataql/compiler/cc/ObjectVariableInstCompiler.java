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
import net.hasor.dataql.parser.ast.Variable;
import net.hasor.dataql.parser.ast.token.StringToken;
import net.hasor.dataql.parser.ast.value.ObjectVariable;

/**
 * 对象
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class ObjectVariableInstCompiler implements InstCompiler<ObjectVariable> {
    @Override
    public void doCompiler(ObjectVariable astInst, InstQueue queue, CompilerContext compilerContext) {
        instLocation(queue, astInst);
        queue.inst(NEW_O);
        List<String> keyFields = astInst.getFieldSort();
        Map<String, StringToken> objectKeys = astInst.getObjectKeys();
        Map<String, Variable> objectData = astInst.getObjectValues();
        //
        for (String fieldKey : keyFields) {
            StringToken keyVal = objectKeys.get(fieldKey);
            Variable variable = objectData.get(fieldKey);
            compilerContext.findInstCompilerByInst(variable).doCompiler(queue);
            //
            instLocation(queue, keyVal);
            queue.inst(PUT, fieldKey);
        }
    }
}
