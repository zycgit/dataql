/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.inset;
import net.hasor.dataql.domain.ObjectModel;
import net.hasor.dataql.kernel.InsetProcess;
import net.hasor.dataql.kernel.InsetProcessContext;
import net.hasor.dataql.kernel.InstSequence;
import net.hasor.dataql.kernel.QueryRuntimeException;
import net.hasor.dataql.kernel.mem.DataHeap;
import net.hasor.dataql.kernel.mem.DataStack;
import net.hasor.dataql.kernel.mem.EnvStack;

/**
 * PUT     // 将栈顶对象元素放入对象元素中（例：PUT,"xxxx"）
 * - 参数说明：共1参数；参数1：属性名称（Map的Key 或 对象的属性名）
 * - 栈行为：消费1，产出0
 * - 堆行为：无
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-19
 */
class PUT implements InsetProcess {
    @Override
    public int getOpcode() {
        return PUT;
    }

    @Override
    public void doWork(InstSequence sequence, DataHeap dataHeap, DataStack dataStack, EnvStack envStack, InsetProcessContext context) throws QueryRuntimeException {
        String fieldName = sequence.currentInst().getString(0);
        Object useData = dataStack.pop();
        Object containerData = dataStack.peek();
        //
        if (containerData instanceof ObjectModel) {
            ((ObjectModel) containerData).put(fieldName, useData);
            return;
        }
        throw new QueryRuntimeException(sequence.programLocation(), "output data error, target type must be ObjectModel.");
    }
}
