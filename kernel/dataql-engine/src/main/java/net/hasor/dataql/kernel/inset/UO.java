/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.inset;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.kernel.InsetProcess;
import net.hasor.dataql.kernel.InsetProcessContext;
import net.hasor.dataql.kernel.InstSequence;
import net.hasor.dataql.kernel.QueryRuntimeException;
import net.hasor.dataql.kernel.mem.DataHeap;
import net.hasor.dataql.kernel.mem.DataStack;
import net.hasor.dataql.kernel.mem.EnvStack;
import net.hasor.dataql.kernel.operator.OperatorProcess;

/**
 * UO      // 一元运算
 * - 参数说明：共1参数；参数1：一元操作符
 * - 栈行为：消费1，产出1
 * - 堆行为：无
 * 开发者可以通过实现 OperatorProcess 接口，覆盖某个运算符实现 运算符重载功能。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-19
 */
class UO implements InsetProcess {
    @Override
    public int getOpcode() {
        return UO;
    }

    @Override
    public void doWork(InstSequence sequence, DataHeap dataHeap, DataStack dataStack, EnvStack envStack, InsetProcessContext context) throws QueryRuntimeException {
        String dyadicSymbol = sequence.currentInst().getString(0);
        Object expData = dataStack.pop();
        //
        if (expData instanceof DataModel) {
            expData = ((DataModel) expData).asOri();
        }
        //
        Class<?> expType = (expData == null) ? Void.class : expData.getClass();
        OperatorProcess process = context.findUnaryOperator(dyadicSymbol, expType);
        //
        if (process == null) {
            throw new QueryRuntimeException(sequence.programLocation(), "UO -> " + dyadicSymbol + " OperatorProcess is Undefined");
        }
        //
        Object result = process.doProcess(sequence.programLocation(), dyadicSymbol, new Object[] { expData }, context.currentHints());
        dataStack.push(result);
    }
}
