/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.operator;
/**
 * 运算操作匹配。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2019-12-11
 */
public interface OperatorMatch extends OperatorProcess {
    boolean testMatch(Class<?>... fstType);
}
