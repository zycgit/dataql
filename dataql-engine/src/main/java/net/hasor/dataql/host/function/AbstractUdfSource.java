/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.host.function;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.hasor.cobble.ClassUtils;
import net.hasor.cobble.function.ESupplier;
import net.hasor.dataql.domain.Udf;
import net.hasor.dataql.domain.UdfSource;
import net.hasor.dataql.host.HostContext;
import net.hasor.dataql.host.spi.UdfSourceFactory;
import net.hasor.dataql.kernel.Finder;

/**
 * UDF source reflection assembly base class. Function overloading is not supported.
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2019-12-11
 */
public abstract class AbstractUdfSource implements UdfSource, UdfSourceFactory {
    public <T> T get(Class<? extends T> targetType) {
        return ClassUtils.newInstance(targetType);
    }

    public Predicate<Method> getPredicate(Class<?> targetType) {
        return method -> {
            boolean testA = method.getDeclaringClass() != Object.class;
            boolean testB = method.getDeclaringClass() != UdfSource.class;
            boolean testC = method.getDeclaringClass() != AbstractUdfSource.class;
            return testA && testB && testC;
        };
    }

    protected Map<String, Udf> buildUdfMap(Object target, Predicate<Method> predicate) {
        Class<?> targetType = target.getClass();
        return new UdfTypeMap(targetType, () -> target, predicate);
    }

    @Override
    public ESupplier<Map<String, Udf>, Exception> getUdfResource(Finder finder) {
        Class<?> targetType = this.getClass();
        return () -> {
            Predicate<Method> predicate = this.getPredicate(targetType);
            Supplier<?> supplier = () -> this.get(targetType);
            return new UdfTypeMap(targetType, supplier, predicate);
        };
    }

    @Override
    public String getResourceName() {
        return this.getClass().getName();
    }

    @Override
    public UdfSource create(HostContext context) {
        return ClassUtils.newInstance(this.getClass());
    }
}
