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
 * GOTO    // 执行跳转
 * - 参数说明：共1参数；参数1：GOTO 的位置
 * - 栈行为：消费0，产出0
 * - 堆行为：无
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-19
 */
class GOTO implements InsetProcess {
    @Override
    public int getOpcode() {
        return GOTO;
    }

    @Override
    public void doWork(InstSequence sequence, DataHeap dataHeap, DataStack dataStack, EnvStack envStack, InsetProcessContext context) {
        int jumpTo = sequence.currentInst().getInt(0);
        sequence.jumpTo(jumpTo);
    }
}
