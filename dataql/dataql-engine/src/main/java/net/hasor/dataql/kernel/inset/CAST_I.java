/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.inset;
import java.util.*;
import net.hasor.dataql.domain.ListModel;
import net.hasor.dataql.domain.ValueModel;
import net.hasor.dataql.kernel.InsetProcess;
import net.hasor.dataql.kernel.InsetProcessContext;
import net.hasor.dataql.kernel.InstSequence;
import net.hasor.dataql.kernel.mem.DataHeap;
import net.hasor.dataql.kernel.mem.DataIterator;
import net.hasor.dataql.kernel.mem.DataStack;
import net.hasor.dataql.kernel.mem.EnvStack;

/**
 * CAST_I  // 将栈顶元素转换为迭代器，作为迭代器有三个特殊操作：data(数据)、next(移动到下一个，如果成功返回true)
 * - 参数说明：共0参数
 * - 栈行为：消费1，产出1
 * - 堆行为：无
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-19
 */
class CAST_I implements InsetProcess {
    @Override
    public int getOpcode() {
        return CAST_I;
    }

    @Override
    public void doWork(InstSequence sequence, DataHeap dataHeap, DataStack dataStack, EnvStack envStack, InsetProcessContext context) {
        Object data = dataStack.pop();
        Iterator iterator = null;
        Object oriData = null;
        //
        if (data == null) {
            oriData = Collections.EMPTY_LIST;
            iterator = Collections.EMPTY_LIST.iterator();
        } else if (data instanceof ValueModel && ((ValueModel) data).isNull()) {
            oriData = Collections.EMPTY_LIST;
            iterator = Collections.EMPTY_LIST.iterator();
        } else if (data instanceof ListModel) {
            oriData = ((ListModel) data).asOri();
            iterator = ((ListModel) data).asOri().iterator();
        } else if (data instanceof Collection) {
            oriData = data;
            iterator = ((Collection) data).iterator();
        } else if (data.getClass().isArray()) {
            List<Object> objects = Arrays.asList((Object[]) data);
            oriData = objects;
            iterator = objects.iterator();
        } else {
            List<Object> objects = Collections.singletonList(data);
            oriData = objects;
            iterator = objects.iterator();
        }
        //
        dataStack.push(new DataIterator(oriData, iterator));
    }
}
