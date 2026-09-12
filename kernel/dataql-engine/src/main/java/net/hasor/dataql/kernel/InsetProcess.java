/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel;
import net.hasor.dataql.compiler.qil.Opcodes;
import net.hasor.dataql.kernel.mem.DataHeap;
import net.hasor.dataql.kernel.mem.DataStack;
import net.hasor.dataql.kernel.mem.EnvStack;

/**
 * 指令执行器接口
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-14
 */
public interface InsetProcess extends Opcodes {
    /** 执行器，用于处理的指令 Code */
    int getOpcode();

    /** 执行指令 */
    void doWork(                    //
            InstSequence sequence,  // 指令序列
            DataHeap dataHeap,      // 数据堆
            DataStack dataStack,    // 数据栈
            EnvStack envStack,      // 环境栈
            InsetProcessContext context   // 执行器上下文
    ) throws QueryRuntimeException;
}
