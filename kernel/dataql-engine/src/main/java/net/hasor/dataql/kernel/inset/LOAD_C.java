/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.inset;
import java.util.Collections;
import java.util.Map;
import net.hasor.dataql.kernel.InsetProcess;
import net.hasor.dataql.kernel.InsetProcessContext;
import net.hasor.dataql.kernel.InstSequence;
import net.hasor.dataql.kernel.mem.DataHeap;
import net.hasor.dataql.kernel.mem.DataStack;
import net.hasor.dataql.kernel.mem.EnvStack;

/**
 * LOAD_C  // 加载自定义路由
 * - 参数说明：共1参数；参数1：@#$符号之一
 * - 栈行为：消费0，产出1
 * - 堆行为：无
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-19
 */
class LOAD_C implements InsetProcess {
    @Override
    public int getOpcode() {
        return LOAD_C;
    }

    @Override
    public void doWork(InstSequence sequence, DataHeap dataHeap, DataStack dataStack, EnvStack envStack, InsetProcessContext context) {
        String symbol = sequence.currentInst().getString(0);
        Map<String, ?> envMap = context.findCustomizeEnvironment(symbol);
        if (envMap == null) {
            envMap = Collections.emptyMap();
        }
        dataStack.push(envMap);
    }
}
