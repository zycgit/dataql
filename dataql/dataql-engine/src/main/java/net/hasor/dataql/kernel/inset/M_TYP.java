/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.inset;
import net.hasor.dataql.domain.UdfSource;
import net.hasor.dataql.kernel.InsetProcess;
import net.hasor.dataql.kernel.InsetProcessContext;
import net.hasor.dataql.kernel.InstSequence;
import net.hasor.dataql.kernel.QueryRuntimeException;
import net.hasor.dataql.kernel.mem.DataHeap;
import net.hasor.dataql.kernel.mem.DataStack;
import net.hasor.dataql.kernel.mem.EnvStack;

/**
 * M_TYP   // 加载一个类型对象到栈顶.
 * - 参数说明：共1参数；参数为要加载的Bean名
 * - 栈行为：消费0，产出1
 * - 堆行为：无
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-19
 */
class M_TYP implements InsetProcess {
    @Override
    public int getOpcode() {
        return M_TYP;
    }

    @Override
    public void doWork(InstSequence sequence, DataHeap dataHeap, DataStack dataStack, EnvStack envStack, InsetProcessContext context) throws QueryRuntimeException {
        String udfType = sequence.currentInst().getString(0);
        Object loadObject = null;
        try {
            loadObject = context.loadObject(udfType);
        } catch (ClassNotFoundException e) {
            throw new QueryRuntimeException(sequence.programLocation(), udfType + " ClassNotFoundException.", e);
        }
        if (loadObject == null) {
            throw new QueryRuntimeException(sequence.programLocation(), "loadObject is null.");
        }
        //
        if (loadObject instanceof UdfSource) {
            loadObject = ((UdfSource) loadObject).getUdfResource(context.getFinder()).get();
        }
        //
        dataStack.push(loadObject);
    }
}
