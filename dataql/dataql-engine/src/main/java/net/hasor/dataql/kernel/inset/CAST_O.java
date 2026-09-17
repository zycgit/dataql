/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.inset;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.domain.ListModel;
import net.hasor.dataql.kernel.InsetProcess;
import net.hasor.dataql.kernel.InsetProcessContext;
import net.hasor.dataql.kernel.InstSequence;
import net.hasor.dataql.kernel.mem.DataHeap;
import net.hasor.dataql.kernel.mem.DataStack;
import net.hasor.dataql.kernel.mem.EnvStack;

/**
 * CAST_O  // 将栈顶元素转换为一个对象，如果是集合那么取第一条记录（可以通过CAST_I方式解决，但会多消耗大约8条左右的指令）
 * - 参数说明：共0参数
 * - 栈行为：消费1，产出1
 * - 堆行为：无
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-19
 */
class CAST_O implements InsetProcess {
    @Override
    public int getOpcode() {
        return CAST_O;
    }

    @Override
    public void doWork(InstSequence sequence, DataHeap dataHeap, DataStack dataStack, EnvStack envStack, InsetProcessContext context) {
        Object data = dataStack.pop();
        //
        if (data == null) {
            //
        } else if (data instanceof ListModel) {
            List<DataModel> modelList = ((ListModel) data).asOri();
            if (modelList == null || modelList.isEmpty()) {
                data = null;
            } else {
                data = modelList.get(0);
            }
        } else if (data instanceof Collection) {
            Iterator dataSet = ((Collection) data).iterator();
            if (dataSet.hasNext()) {
                data = dataSet.next();
            } else {
                data = null;
            }
        } else if (data.getClass().isArray()) {
            Object[] dataSet = (Object[]) data;
            if (dataSet.length != 0) {
                data = dataSet[0];
            } else {
                data = null;
            }
        }
        //
        dataStack.push(data);
    }
}
