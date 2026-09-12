/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.inset;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.domain.DomainHelper;
import net.hasor.dataql.kernel.InsetProcess;
import net.hasor.dataql.kernel.InsetProcessContext;
import net.hasor.dataql.kernel.InstSequence;
import net.hasor.dataql.kernel.ThrowRuntimeException;
import net.hasor.dataql.kernel.mem.DataHeap;
import net.hasor.dataql.kernel.mem.DataStack;
import net.hasor.dataql.kernel.mem.EnvStack;
import net.hasor.dataql.kernel.mem.ExitType;

/**
 * THROW   // 结束所有指令序列的执行，并抛出异常
 * - 参数说明：共1参数；参数1：错误码
 * - 栈行为：消费1，产出0
 * - 堆行为：无
 * 提示：区别于 RETURN 指令的是，THROW 指令将会终结整个查询的执行并抛出异常。
 * 而 RETURN 指令只会终止当前指令序列的执行。同时有别于 EXIT 指令的是，THROW 执行将会得到异常抛出。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-19
 * @see net.hasor.dataql.runtime.inset.RETURN
 * @see net.hasor.dataql.runtime.inset.EXIT
 */
class THROW implements InsetProcess {
    @Override
    public int getOpcode() {
        return THROW;
    }

    @Override
    public void doWork(InstSequence sequence, DataHeap dataHeap, DataStack dataStack, EnvStack envStack, InsetProcessContext context) {
        int resultCode = sequence.currentInst().getInt(0);
        Object result = dataStack.pop();
        DataModel dataModel = DomainHelper.convertTo(result);
        dataStack.setResultCode(resultCode);
        dataStack.setResult(dataModel);
        dataStack.setExitType(ExitType.Throw);
        sequence.jumpTo(sequence.exitPosition());
        //
        String errorMessage = dataModel.isValue() ? dataModel.unwrap().toString() : "";
        throw new ThrowRuntimeException(    //
                sequence.programLocation(), // location
                errorMessage,               // errorMessage
                resultCode,                 // throwCode
                context.executionTime(),    // executionTime
                dataModel                   // result
        );
    }
}
