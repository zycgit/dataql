/*
 * Copyright 2015-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.dataql.sqlproc.types;
import net.hasor.cobble.ClassUtils;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * JDBC 4.2 full  compatible
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-10-31
 */
public final class TypeHandlerRegistry {
    private static final Map<String, Class<?>> typeHandlerTypeCache = new ConcurrentHashMap<>();
    public static final  TypeHandlerRegistry   DEFAULT              = new TypeHandlerRegistry();

    private final UnknownTypeHandler       defaultTypeHandler  = new UnknownTypeHandler(this);
    private final Map<String, TypeHandler> cachedByHandlerType = new ConcurrentHashMap<>();
    private final Map<String, TypeHandler> cachedByTypeName    = new ConcurrentHashMap<>();

    private static void registerTypeHandlerType(Class<?> typeHandler) {
        String name = typeHandler.getName();
        if (!typeHandlerTypeCache.containsKey(name) && !typeHandler.isAnnotationPresent(NoCache.class)) {
            typeHandlerTypeCache.put(name, typeHandler);
        }
    }

    public TypeHandler createTypeHandler(Class<?> typeHandler) {
        return this.createTypeHandler(typeHandler, null);
    }

    public TypeHandler createTypeHandler(Class<?> typeHandler, Class<?> argType) {
        return this.createTypeHandler(typeHandler, argType, type -> {
            try {
                Constructor<?> constructor = typeHandler.getConstructor(Class.class);
                return this.createByConstructor(constructor, argType);
            } catch (NoSuchMethodException e) {
                return this.createByClass(typeHandler, argType);
            }
        });
    }

    public TypeHandler createTypeHandler(Class<?> typeHandler, Class<?> argType, Function<Class<?>, TypeHandler> supplier) {
        if (!TypeHandler.class.isAssignableFrom(typeHandler)) {
            throw new ClassCastException(typeHandler.getName() + " is not a subclass of " + TypeHandler.class.getName());
        }

        if (typeHandler.isAnnotationPresent(NoCache.class)) {
            if (typeHandler == UnknownTypeHandler.class) {
                return this.defaultTypeHandler;
            } else {
                TypeHandler handler = supplier.apply(argType);
                if (handler == null) {
                    return this.defaultTypeHandler;
                } else {
                    return handler;
                }
            }
        } else {
            registerTypeHandlerType(typeHandler);
            String cacheName = typeHandler.getName() + (argType == null ? "" : ("," + argType.getName()));
            return this.cachedByHandlerType.computeIfAbsent(cacheName, type -> {
                if (typeHandler == UnknownTypeHandler.class) {
                    return this.defaultTypeHandler;
                } else {
                    TypeHandler handler = supplier.apply(argType);
                    if (handler == null) {
                        return this.defaultTypeHandler;
                    } else {
                        return handler;
                    }
                }
            });
        }
    }

    protected TypeHandler createByClass(Class<?> typeHandlerClass, Class<?> argType) {
        return ClassUtils.newInstance(typeHandlerClass);
    }

    protected TypeHandler createByConstructor(Constructor<?> typeHandlerConstructor, Class<?> argType) {
        try {
            return (TypeHandler) typeHandlerConstructor.newInstance(argType);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public TypeHandler getHandlerByHandlerType(String handlerType) {
        return this.cachedByHandlerType.getOrDefault(handlerType, null);
    }

    public TypeHandler getHandlerByHandlerType(Class<?> handlerType) {
        return this.cachedByHandlerType.getOrDefault(handlerType.getName(), null);
    }
}
