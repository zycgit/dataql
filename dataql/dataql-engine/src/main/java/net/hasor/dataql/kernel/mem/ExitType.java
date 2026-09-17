/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.mem;
/**
 * 退出模式
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2019-12-11
 */
public enum ExitType {
    /** 正常退出，后续指令序列继续执行 */
    Return,
    /** 非正常退出，终止后续指令序列执行并抛出异常 */
    Throw,
    /** 中断正常执行，并退出整个执行序列 */
    Exit,
}
