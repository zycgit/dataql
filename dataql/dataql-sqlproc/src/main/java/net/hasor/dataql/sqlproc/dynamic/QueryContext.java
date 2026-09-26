/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic;
import net.hasor.dataql.sqlproc.dynamic.rule.SqlRule;
import net.hasor.dataql.sqlproc.types.TypeHandlerRegistry;

public interface QueryContext {

    SqlRule findRule(String ruleName);

    DynamicSql findMacro(String name);

    Class<?> loadClass(String typeName) throws ClassNotFoundException;

    TypeHandlerRegistry getTypeRegistry();

    ClassLoader getClassLoader();
}
