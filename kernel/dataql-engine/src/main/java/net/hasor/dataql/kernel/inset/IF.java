/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.inset;
import net.hasor.dataql.domain.ValueModel;
import net.hasor.dataql.kernel.InsetProcess;
import net.hasor.dataql.kernel.InsetProcessContext;
import net.hasor.dataql.kernel.InstSequence;
import net.hasor.dataql.kernel.mem.DataHeap;
import net.hasor.dataql.kernel.mem.DataStack;
import net.hasor.dataql.kernel.mem.EnvStack;

/**
 * IF      // if 条件判断，如果条件判断失败那么 GOTO 到指定位置，否则继续往下执行
 * - 参数说明：共1参数；参数1：GOTO 的位置
 * - 栈行为：消费1，产出0
 * - 堆行为：无
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-19
 */
class IF implements InsetProcess {
    @Override
    public int getOpcode() {
        return IF;
    }

    @Override
    public void doWork(InstSequence sequence, DataHeap dataHeap, DataStack dataStack, EnvStack envStack, InsetProcessContext context) {
        Object test = dataStack.pop();
        if (test instanceof ValueModel) {
            test = ((ValueModel) test).asOri();
        }
        //
        int jumpLabel = sequence.currentInst().getInt(0);
        //
        boolean testFailed = (test == null || Boolean.FALSE.equals(test));
        if (!testFailed) {
            String testStr = test.toString();
            testFailed = ("false".equalsIgnoreCase(testStr) || "off".equalsIgnoreCase(testStr) || "0".equalsIgnoreCase(testStr));
        }
        //
        if (testFailed) {
            sequence.jumpTo(jumpLabel);
        }
    }
}
