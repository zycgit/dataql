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
package net.hasor.dataql.sqlproc;

import net.hasor.dataql.sqlproc.dynamic.DynamicSql;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.rule.SqlRule;
import net.hasor.dataql.sqlproc.types.TypeHandlerRegistry;
import net.hasor.dataql.sqlproc.utils.DsUtils;
import net.hasor.dbvisitor.jdbc.core.JdbcTemplate;
import org.junit.After;
import org.junit.Before;

import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 统一的 SQL 相关测试基类，提供：
 *   - H2 连接生命周期管理 ({@link #conn})
 *   - JDBC mock 工具 (CallableStatement / PreparedStatement / ResultSet)
 *   - {@link QueryContext} 工厂方法
 *   - 便捷的 H2 连接创建方法
 */
public abstract class AbstractSqlProcTest {

    /** 通过 {@link DsUtils#h2Conn()} 获取的 H2 连接，包含 tb_h2_types 表。在 @Before 中初始化，@After 中关闭。 */
    protected Connection conn;

    @Before
    public void setUpConnection() throws SQLException {
        this.conn = DsUtils.h2Conn();
    }

    @After
    public void tearDownConnection() throws SQLException {
        if (this.conn != null && !this.conn.isClosed()) {
            this.conn.close();
        }
    }

    // ----------------------------------------------------------------
    // H2 helpers
    // ----------------------------------------------------------------

    /** 创建一个独立的 H2 内存连接（不会影响 {@link #conn}），不使用预置的 tb_h2_types 表。 */
    protected Connection newH2Connection() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:h2:mem:test_" + UUID.randomUUID().toString().substring(0, 8), "sa", "");
    }

    /** 创建一个包含 users 表的 H2 连接。 */
    protected Connection newH2WithUsers() throws SQLException {
        Connection c = newH2Connection();
        try {
            JdbcTemplate tpl = new JdbcTemplate(c);
            tpl.execute("create table users (id bigint auto_increment, name varchar(100), age int, primary key (id))");
            tpl.execute("insert into users (name, age) values ('Alice', 25)");
            tpl.execute("insert into users (name, age) values ('Bob', 30)");
            tpl.execute("insert into users (name, age) values ('Charlie', 35)");
            return c;
        } catch (Exception e) {
            c.close();
            throw new SQLException(e);
        }
    }

    // ----------------------------------------------------------------
    // JDBC mock helpers
    // ----------------------------------------------------------------

    /** 创建一个 mock CallableStatement，根据传入的 Map 返回 getXxx 的结果。 */
    @SuppressWarnings("unchecked")
    protected CallableStatement mockCallableStatement(Map<String, Object> returnValues) {
        return (CallableStatement) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class[]{CallableStatement.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.startsWith("get") && args != null && args.length > 0) {
                        Object val = returnValues.get(name);
                        if (val != null) return val;
                        if ("getObject".equals(name)) return returnValues.get("default");
                    }
                    if ("wasNull".equals(name)) {
                        return returnValues.getOrDefault("wasNull", false);
                    }
                    return null;
                });
    }

    /** 创建一个 mock PreparedStatement，将 setXxx 参数捕获到 captured Map 中。 */
    protected PreparedStatement mockPreparedStatement(Map<Integer, Object> captured) {
        return (PreparedStatement) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class[]{PreparedStatement.class},
                (proxy, method, args) -> {
                    if (method.getName().startsWith("set") && args.length >= 2) {
                        captured.put((Integer) args[0], args[1]);
                    }
                    return null;
                });
    }

    /** 创建一个 mock ResultSet，根据传入的 Map 返回 getXxx 的结果。 */
    protected ResultSet mockResultSet(Map<String, Object> values) {
        return (ResultSet) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class[]{ResultSet.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.startsWith("get")) {
                        if (values.containsKey(name)) return values.get(name);
                        if (values.containsKey("default")) return values.get("default");
                    }
                    if ("wasNull".equals(name)) return values.getOrDefault("wasNull", false);
                    return null;
                });
    }

    // ----------------------------------------------------------------
    // QueryContext
    // ----------------------------------------------------------------

    /** 创建一个最小实现的 QueryContext，可用于无需规则/宏的动态 SQL 编译。 */
    protected QueryContext newQueryContext() {
        return new QueryContext() {
            public TypeHandlerRegistry getTypeRegistry() { return TypeHandlerRegistry.DEFAULT; }
            public ClassLoader getClassLoader() { return getClass().getClassLoader(); }
            public SqlRule findRule(String ruleName) { return null; }
            public DynamicSql findMacro(String name) { return null; }
            public Class<?> loadClass(String typeName) throws ClassNotFoundException { return Class.forName(typeName); }
        };
    }
}
