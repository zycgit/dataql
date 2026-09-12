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
 * HINT     // 设置 Hint，影响执行引擎的参数选项。
 * - 参数说明：共2参数；参数1：选项Key；参数2：选项Value
 * - 栈行为：消费2，产出0
 * - 堆行为：无
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-19
 */
class HINT implements InsetProcess {
    @Override
    public int getOpcode() {
        return HINT;
    }

    @Override
    public void doWork(InstSequence sequence, DataHeap dataHeap, DataStack dataStack, EnvStack envStack, InsetProcessContext context) {
        Object value = dataStack.pop();
        String key = (String) dataStack.pop();
        //
        if (value == null) {
            context.currentHints().removeHint(key);
        } else if (value instanceof Boolean) {
            context.currentHints().setHint(key, (Boolean) value);
        } else if (value instanceof Number) {
            context.currentHints().setHint(key, (Number) value);
        } else {
            context.currentHints().setHint(key, value.toString());
        }
    }
}
