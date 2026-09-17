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
import net.hasor.dataql.kernel.mem.RefLambdaCall;

/**
 * M_REF   // 引用另一处的指令序列地址，并将其作为 UDF 形态存放到栈顶
 * - 参数说明：共1参数；参数1：内置lambda函数的入口地址
 * - 栈行为：消费0，产出1
 * - 堆行为：无
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-19
 */
class M_REF implements InsetProcess {
    @Override
    public int getOpcode() {
        return M_REF;
    }

    @Override
    public void doWork(InstSequence sequence, DataHeap dataHeap, DataStack dataStack, EnvStack envStack, InsetProcessContext context) {
        int callAddress = sequence.currentInst().getInt(0);
        InstSequence methodSeq = sequence.methodSet(callAddress);
        RefLambdaCall refLambdaCall = new RefLambdaCall(//
                methodSeq,  //
                dataHeap,   //
                envStack,   //
                context     //
        );
        dataStack.push(refLambdaCall);
    }
}
