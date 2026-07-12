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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import net.hasor.cobble.function.EFunction;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import net.hasor.dataql.sqlproc.dynamic.config.StatementType;
import net.hasor.dataql.sqlproc.dynamic.resolve.ConfigResolveRoot;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContext;
import net.hasor.dataql.sqlproc.execute.support.SqlQueryContextFactory;
import net.hasor.dataql.sqlproc.utils.DsUtils;
import net.hasor.dbvisitor.jdbc.core.JdbcTemplate;
import org.junit.After;
import org.junit.Before;

/**
 * 统一的 SQL 相关测试基类，提供：
 *   - H2 连接生命周期管理 ({@link #conn})
 *   - {@link ExecuteContext} 工厂方法
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
        return DriverManager.getConnection("jdbc:h2:mem:test_" + UUID.randomUUID().toString().substring(0, 8), "sa", "");
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

    /** 创建 SQL 执行测试使用的 QueryContext。 */
    protected ExecuteContext newQueryContext() {
        return newQueryContext((ConnectionProvider) null);
    }

    protected ExecuteContext newQueryContext(EFunction<String, Connection, SQLException> function) {
        return newQueryContext((sourceName, hints) -> function.eApply(sourceName));
    }

    protected ExecuteContext newQueryContext(ConnectionProvider provider) {
        ExecuteContext context = new SqlQueryContextFactory().create(new HostConfiguration());
        context.setConnectionProvider(provider);
        return context;
    }

    // ----------------------------------------------------------------
    // SqlConfig helpers
    // ----------------------------------------------------------------

    protected HintsSet hints() {
        return new HintsSet();
    }

    protected HintsSet hints(String key, String value) {
        HintsSet hints = hints();
        hints.setHint(key, value);
        return hints;
    }

    protected HintsSet hints(String key, Number value) {
        HintsSet hints = hints();
        hints.setHint(key, value);
        return hints;
    }

    protected HintsSet hints(String key, boolean value) {
        HintsSet hints = hints();
        hints.setHint(key, value);
        return hints;
    }

    protected HintsSet statementHints(StatementType statementType) {
        HintsSet hints = hints();
        if (statementType != null && statementType != StatementType.Prepared) {
            hints.setHint("statementType", statementType.getValue());
        }
        return hints;
    }

    protected SqlConfig sqlConfig(QueryType type, String sql) {
        return sqlConfig(type, hints(), sql);
    }

    protected SqlConfig sqlConfig(QueryType type, HintsSet hints, String sql) {
        return new ConfigResolveRoot().parsePlainConfig(type.getTagString(), hints, sql);
    }

    protected SqlConfig xmlConfig(QueryType type, String xml) {
        return xmlConfig(type, hints(), xml);
    }

    protected SqlConfig xmlConfig(QueryType type, HintsSet hints, String xml) {
        return new ConfigResolveRoot().parseXmlConfig(type.getTagString(), hints, xml);
    }

}
