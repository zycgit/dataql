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
 * STORE   // 栈顶数据存储到堆（例：STORE，2）
 * - 参数说明：共1参数；参数1：存入堆的位置；
 * - 栈行为：消费1，产出0
 * - 堆行为：存入数据
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-19
 * @see net.hasor.dataql.runtime.inset.LOAD
 */
class STORE implements InsetProcess {
    @Override
    public int getOpcode() {
        return STORE;
    }

    @Override
    public void doWork(InstSequence sequence, DataHeap dataHeap, DataStack dataStack, EnvStack envStack, InsetProcessContext context) {
        int index = sequence.currentInst().getInt(0);
        Object data = dataStack.pop();
        dataHeap.saveData(index, data);
    }
}
