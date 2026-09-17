/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.types.handler;

import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;

abstract class TypeHandlerMockSupport extends AbstractSqlProcTest {
    @SuppressWarnings("unchecked")
    protected CallableStatement mockCallableStatement(Map<String, Object> returnValues) {
        return (CallableStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { CallableStatement.class }, (proxy, method, args) -> {
            String name = method.getName();
            if (name.startsWith("get") && args != null && args.length > 0) {
                Object val = returnValues.get(name);
                if (val != null) {
                    return val;
                }
                if ("getObject".equals(name)) {
                    return returnValues.get("default");
                }
            }
            if ("wasNull".equals(name)) {
                return returnValues.getOrDefault("wasNull", false);
            }
            return null;
        });
    }

    protected PreparedStatement mockPreparedStatement(Map<Integer, Object> captured) {
        return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { PreparedStatement.class }, (proxy, method, args) -> {
            if (method.getName().startsWith("set") && args.length >= 2) {
                captured.put((Integer) args[0], args[1]);
            }
            return null;
        });
    }

    protected ResultSet mockResultSet(Map<String, Object> values) {
        return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { ResultSet.class }, (proxy, method, args) -> {
            String name = method.getName();
            if (name.startsWith("get")) {
                if (values.containsKey(name)) {
                    return values.get(name);
                }
                if (values.containsKey("default")) {
                    return values.get("default");
                }
            }
            if ("wasNull".equals(name)) {
                return values.getOrDefault("wasNull", false);
            }
            return null;
        });
    }
}
