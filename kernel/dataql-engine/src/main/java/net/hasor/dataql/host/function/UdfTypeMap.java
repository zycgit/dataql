/*
 * Copyright 2015-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.dataql.host.function;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.hasor.cobble.BeanUtils;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.convert.ConverterUtils;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.domain.Udf;

class UdfTypeMap extends HashMap<String, Udf> {
    UdfTypeMap(Class<?> utilType, Supplier<?> provider, Predicate<Method> methodTypeMatcher) {
        List<Method> methodList = BeanUtils.getMethods(utilType);
        for (Method method : methodList) {
            this.initMethod(provider, methodTypeMatcher, method);
        }
    }

    private void initMethod(Supplier<?> provider, Predicate<Method> methodTypeMatcher, Method method) {
        int modifiers = method.getModifiers();
        if (!Modifier.isPublic(modifiers) || (!Modifier.isStatic(modifiers) && provider == null)) {
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
        if (udfName == null) {
            udfName = new UdfName() {
                public Class<? extends Annotation> annotationType() {
                    return UdfName.class;
                }

                public String value() {
                    return method.getName();
                }
            };
        }
        if (StringUtils.isBlank(udfName.value())) {
            throw new NullPointerException("udfName is null -> " + method);
        }

        if (Modifier.isStatic(modifiers)) {
            this.put(udfName.value(), new StaticUdf(method));
        } else {
            this.put(udfName.value(), new ObjectUdf(method, provider));
        }
    }

    private static Object doInvoke(Method targetMethod, Object target, Object[] values, Hints hints) throws Exception {
        Class<?>[] parameterTypes = targetMethod.getParameterTypes();
        Object[] inData = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Object paramData;
            if (Hints.class.isAssignableFrom(parameterTypes[i])) {
                paramData = hints;
            } else if (UdfParams.class.isAssignableFrom(parameterTypes[i])) {
                paramData = (UdfParams) () -> values;
            } else {
                paramData = i < values.length ? values[i] : null;
            }
            if (paramData instanceof DataModel) {
                paramData = ((DataModel) paramData).asOri();
            }
            inData[i] = ConverterUtils.convert(parameterTypes[i], paramData);
        }
        return targetMethod.invoke(target, inData);
    }

    private record StaticUdf(Method target) implements Udf {
        private StaticUdf(Method target) {
            this.target = target;
            this.target.setAccessible(true);
        }

        public Object call(Hints hints, Object... values) throws Throwable {
            return doInvoke(this.target, null, values, hints);
        }
    }

    private record ObjectUdf(Method target, Supplier<?> provider) implements Udf {
        private ObjectUdf(Method target, Supplier<?> provider) {
            this.target = target;
            this.target.setAccessible(true);
            this.provider = provider;
        }

        public Object call(Hints hints, Object... values) throws Throwable {
            Object targetObject = this.provider.get();
            if (targetObject == null) {
                throw new NullPointerException("target Object is null.");
            }
            return doInvoke(this.target, targetObject, values, hints);
        }
    }
}
