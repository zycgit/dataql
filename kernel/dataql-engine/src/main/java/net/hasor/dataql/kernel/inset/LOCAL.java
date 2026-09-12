/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.inset;
import net.hasor.dataql.kernel.InsetProcess;
import net.hasor.dataql.kernel.InsetProcessContext;
import net.hasor.dataql.kernel.InstSequence;
import net.hasor.dataql.kernel.mem.DataHeap;
import net.hasor.dataql.kernel.mem.DataStack;
import net.hasor.dataql.kernel.mem.EnvStack;
import net.hasor.dataql.kernel.mem.RefLambdaCallStruts;

/**
 * LOCAL   // 将入参存入堆，也用于标记变量名称
 * - 参数说明：共3参数；参数1：调用时的入参位置；参数2：存储到堆中的位置；参数3：参数名助记符；
 * - 栈行为：消费0，产出0
 * - 堆行为：存入数据
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-19
 */
class LOCAL implements InsetProcess {
    @Override
    public int getOpcode() {
        return LOCAL;
    }

    @Override
    public void doWork(InstSequence sequence, DataHeap dataHeap, DataStack dataStack, EnvStack envStack, InsetProcessContext context) {
        int localArgsIdx = sequence.currentInst().getInt(0);
        int localPosition = sequence.currentInst().getInt(1);
        String name = sequence.currentInst().getString(2);
        RefLambdaCallStruts lambdaCall = (RefLambdaCallStruts) dataStack.peek();
        Object[] callParams = lambdaCall.params();
        //
        Object paramValue = null;
        if (localArgsIdx < callParams.length) {
            paramValue = callParams[localArgsIdx];
        }
        dataHeap.saveData(localPosition, paramValue);
        dataHeap.defineName(localPosition, name);
    }
}
