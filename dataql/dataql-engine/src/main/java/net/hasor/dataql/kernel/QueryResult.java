/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.kernel.mem.ExitType;

/**
 * 结果集
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public interface QueryResult {
    /** 执行结果是否通过 EXIT 形式返回的 */
    default boolean isExit() {
        return ExitType.Exit == getExitType();
    }

    /** 执行结果是否通过 EXIT 形式返回的 */
    ExitType getExitType();

    /** 获得退出码。如果未指定退出码，则默认值为 0 */
    int getCode();

    /** 获得返回值 */
    DataModel getData();

    /** 获得本次执行耗时 */
    long executionTime();
}
