/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.compiler.qil;
/**
 * 生成指令序列
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public interface InstCompilerExecutor extends Opcodes {
    /** 生成指令序列 */
    void doCompiler(InstQueue queue);
}
