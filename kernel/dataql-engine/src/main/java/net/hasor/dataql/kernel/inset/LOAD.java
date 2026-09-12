/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.inset;
import java.util.function.Supplier;
import net.hasor.dataql.kernel.InsetProcess;
import net.hasor.dataql.kernel.InsetProcessContext;
import net.hasor.dataql.kernel.InstSequence;
import net.hasor.dataql.kernel.mem.DataHeap;
import net.hasor.dataql.kernel.mem.DataStack;
import net.hasor.dataql.kernel.mem.EnvStack;

/**
 * LOAD    // 从指定深度的堆中加载n号元素到栈（例：LOAD 1 ,1 ）
 * - 参数说明：共2参数；参数1：堆深度；参数2：元素序号；
 * - 栈行为：消费0，产出1
 * - 堆行为：取出数据（不删除）
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-19
 * @see net.hasor.dataql.runtime.inset.STORE
 */
class LOAD implements InsetProcess {
    @Override
    public int getOpcode() {
        return LOAD;
    }

    @Override
    public void doWork(InstSequence sequence, DataHeap dataHeap, DataStack dataStack, EnvStack envStack, InsetProcessContext context) {
        int depth = sequence.currentInst().getInt(0);
        int index = sequence.currentInst().getInt(1);
        Object data = dataHeap.loadData(depth, index);
        if (data instanceof Supplier) {
            data = ((Supplier) data).get();
        }
        dataStack.push(data);
    }
}
