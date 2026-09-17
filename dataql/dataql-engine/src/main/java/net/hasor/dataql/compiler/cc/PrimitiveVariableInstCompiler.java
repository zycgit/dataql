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
import net.hasor.dataql.parser.ast.value.PrimitiveVariable;
import net.hasor.dataql.parser.ast.value.PrimitiveVariable.ValueType;

/**
 * 基础类型值，用于表示【String、Number、Null、Boolean】四种基本类型
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class PrimitiveVariableInstCompiler implements InstCompiler<PrimitiveVariable> {
    @Override
    public void doCompiler(PrimitiveVariable astInst, InstQueue queue, CompilerContext compilerContext) {
        this.instLocation(queue, astInst);
        ValueType valueType = astInst.getValueType();
        if (valueType == ValueType.Boolean) {
            queue.inst(LDC_B, Boolean.parseBoolean(astInst.getValue().toString()));
        }
        if (valueType == ValueType.Null) {
            queue.inst(LDC_N);
        }
        if (valueType == ValueType.Number) {
            queue.inst(LDC_D, astInst.getValue());
        }
        if (valueType == ValueType.String) {
            queue.inst(LDC_S, astInst.getValue().toString());
        }
    }
}
