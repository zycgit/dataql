/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.inset;
import net.hasor.dataql.domain.ListModel;
import net.hasor.dataql.kernel.InsetProcess;
import net.hasor.dataql.kernel.InsetProcessContext;
import net.hasor.dataql.kernel.InstSequence;
import net.hasor.dataql.kernel.QueryRuntimeException;
import net.hasor.dataql.kernel.mem.DataHeap;
import net.hasor.dataql.kernel.mem.DataStack;
import net.hasor.dataql.kernel.mem.EnvStack;

/**
 * PUSH    // 将栈顶元素压入集合（例：PUSH）
 * - 参数说明：共0参数；
 * - 栈行为：消费1，产出0
 * - 堆行为：无
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-19
 */
class PUSH implements InsetProcess {
    @Override
    public int getOpcode() {
        return PUSH;
    }

    @Override
    public void doWork(InstSequence sequence, DataHeap dataHeap, DataStack dataStack, EnvStack envStack, InsetProcessContext context) throws QueryRuntimeException {
        Object data = dataStack.pop();
        Object ors = dataStack.peek();
        //
        if (ors instanceof ListModel) {
            ((ListModel) ors).add(data);
            return;
        }
        throw new QueryRuntimeException(sequence.programLocation(), "output data error, target type must be ListModel.");
    }
}
