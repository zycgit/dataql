/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.host.function;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.hasor.cobble.BeanUtils;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.convert.ConverterUtils;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.domain.Udf;
import net.hasor.dataql.domain.UdfParams;

class UdfTypeMap extends HashMap<String, Udf> {
    UdfTypeMap(Class<?> utilType, Supplier<?> provider, Predicate<Method> methodTypeMatcher) {
        for (Method method : BeanUtils.getMethods(utilType)) {
            this.initMethod(provider, methodTypeMatcher, method);
        }
    }

    private void initMethod(Supplier<?> provider, Predicate<Method> methodTypeMatcher, Method method) {
        int modifiers = method.getModifiers();
        boolean staticMethod = Modifier.isStatic(modifiers);
        if (!Modifier.isPublic(modifiers) || (!staticMethod && provider == null)) {
            return;
        }
        if (methodTypeMatcher != null && !methodTypeMatcher.test(method)) {
            return;
        }
        if (method.getDeclaringClass() == Object.class || this.containsKey(method.getName())) {
            return;
        }

        UdfName udfName = method.getAnnotation(UdfName.class);
        if (udfName == null) {
            udfName = method.getDeclaringClass().getAnnotation(UdfName.class);
        }

        String name = udfName == null ? method.getName() : udfName.value();
        if (StringUtils.isBlank(name)) {
            throw new NullPointerException("udfName is null -> " + method);
        }

        method.setAccessible(true);
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (staticMethod) {
            this.put(name, (hints, params) -> this.invoke(method, null, parameterTypes, hints, params));
        } else {
            this.put(name, (hints, params) -> {
                Object target = provider.get();
                if (target == null) {
                    throw new NullPointerException("target Object is null.");
                }
                return this.invoke(method, target, parameterTypes, hints, params);
            });
        }
    }

    private Object invoke(Method method, Object target, Class<?>[] parameterTypes, Hints hints, UdfParams params) throws Exception {
        Object[] values = params.allParams();
        Object[] arguments = new Object[parameterTypes.length];
        int paramIndex = 0;
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            Object value;
            if (Hints.class.isAssignableFrom(parameterType)) {
                value = hints;
            } else if (UdfParams.class.isAssignableFrom(parameterType)) {
                value = params;
            } else {
                value = paramIndex < values.length ? values[paramIndex] : null;
                paramIndex++;
            }

            if (value instanceof DataModel dataModel) {
                value = dataModel.asOri();
            }

            arguments[i] = ConverterUtils.convert(parameterType, value);
        }

        return method.invoke(target, arguments);
    }
}
