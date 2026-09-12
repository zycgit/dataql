/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel;
import net.hasor.dataql.compiler.qil.Instruction;

/**
 * 用于圈定执行序列，当isExit返回 true 之后。表示圈定结束。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-14
 */
public interface InstFilter {
    /** 测试该指令是否作为圈定的结束位置。 */
    boolean isExit(Instruction inst);
}
