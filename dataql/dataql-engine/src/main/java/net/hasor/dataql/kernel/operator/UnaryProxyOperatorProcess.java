/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.operator;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.kernel.QueryRuntimeException;
import net.hasor.dataql.parser.location.RuntimeLocation;

/**
 * 一元运算代理。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
class UnaryProxyOperatorProcess implements OperatorMatch {
    private final Class<?>        unaryType;
    private final OperatorProcess process;

    public UnaryProxyOperatorProcess(Class<?> unaryType, OperatorProcess process) {
        this.unaryType = unaryType;
        this.process = process;
    }

    @Override
    public Object doProcess(RuntimeLocation location, String operator, Object[] args, Hints option) throws QueryRuntimeException {
        return this.process.doProcess(location, operator, args, option);
    }

    @Override
    public boolean testMatch(Class<?>... fstType) {
        return this.unaryType.isAssignableFrom(fstType[0]);
    }
}
