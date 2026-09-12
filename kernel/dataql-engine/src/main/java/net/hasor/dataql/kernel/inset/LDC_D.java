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
import net.hasor.dataql.kernel.operator.OperatorUtils;
import static net.hasor.dataql.domain.HintValue.MIN_DECIMAL_WIDTH;
import static net.hasor.dataql.domain.HintValue.MIN_INTEGER_WIDTH;

/**
 * LDC_D   // 将数字压入栈（例：LDC_D 12345）
 * - 参数说明：共1参数；参数1：数据；
 * - 栈行为：消费0，产出1
 * - 堆行为：无
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-19
 */
class LDC_D implements InsetProcess {
    @Override
    public int getOpcode() {
        return LDC_D;
    }

    @Override
    public void doWork(InstSequence sequence, DataHeap dataHeap, DataStack dataStack, EnvStack envStack, InsetProcessContext context) {
        Number number = sequence.currentInst().getNumber(0);
        String decimalWidth = (String) context.currentHints().getHint(MIN_DECIMAL_WIDTH);
        String integerWidth = (String) context.currentHints().getHint(MIN_INTEGER_WIDTH);
        dataStack.push(OperatorUtils.fixNumberWidth(number, decimalWidth, integerWidth));
    }
}
