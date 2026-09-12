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

/**
 * E_PUSH  // 取出当前栈顶数据，并压入环境栈
 * - 参数说明：共0参数；
 * - 栈行为：消费1，产出0
 * - 环境栈行为：消费0，产出1
 * - 堆行为：无
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-19
 */
class E_PUSH implements InsetProcess {
    @Override
    public int getOpcode() {
        return E_PUSH;
    }

    @Override
    public void doWork(InstSequence sequence, DataHeap dataHeap, DataStack dataStack, EnvStack envStack, InsetProcessContext context) {
        Object test = dataStack.pop();
        envStack.push(test);
    }
}
