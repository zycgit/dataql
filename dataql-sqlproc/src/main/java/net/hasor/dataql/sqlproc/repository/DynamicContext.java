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
package net.hasor.dataql.sqlproc.repository;
import net.hasor.dataql.sqlproc.dialect.ClassLoaderProvider;
import net.hasor.dataql.sqlproc.repository.rule.RuleRegistry;
import net.hasor.dataql.sqlproc.repository.rule.SqlBuildRule;
import net.hasor.dataql.sqlproc.types.TypeHandler;
import net.hasor.dataql.sqlproc.types.TypeHandlerRegistry;

/**
 * 解析动态 SQL 配置
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-06-05
 */
public interface DynamicContext extends ClassLoaderProvider {

    DynamicSql findDynamic(String dynamicId);

    default TypeHandler<?> findTypeHandler(Class<?> handlerType) {
        if (TypeHandlerRegistry.hasTypeHandler(handlerType)) {
            return TypeHandlerRegistry.getTypeHandler(handlerType);
        } else {
            return null;
        }
    }

    default TypeHandler<?> findJdbcTypeHandler(int jdbcType) {
        TypeHandlerRegistry typeRegistry = getTypeRegistry();
        if (typeRegistry.hasJdbcTypeHandler(jdbcType)) {
            return typeRegistry.getJdbcTypeHandler(jdbcType);
        } else {
            return null;
        }
    }

    default TypeHandler<?> findJavaTypeHandler(Class<?> javaType) {
        TypeHandlerRegistry typeRegistry = getTypeRegistry();
        if (typeRegistry.hasJavaTypeHandler(javaType)) {
            return typeRegistry.getJavaTypeHandler(javaType);
        } else {
            return null;
        }
    }

    default TypeHandler<?> findJavaTypeHandler(Class<?> javaType, int jdbcType) {
        TypeHandlerRegistry typeRegistry = getTypeRegistry();
        if (typeRegistry.hasJavaTypeHandler(javaType, jdbcType)) {
            return typeRegistry.getJavaTypeHandler(javaType, jdbcType);
        } else {
            return null;
        }
    }

    default SqlBuildRule findRule(String ruleName) {
        return getRuleRegistry().findByName(ruleName);
    }

    default TypeHandlerRegistry getTypeRegistry() {
        return TypeHandlerRegistry.DEFAULT;
    }

    default RuleRegistry getRuleRegistry() {
        return RuleRegistry.DEFAULT;
    }
}