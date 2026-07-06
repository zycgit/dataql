package net.hasor.dataql.sqlproc.execute;

import net.hasor.dataql.Hints;
import net.hasor.dataql.runtime.HintsSet;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.config.*;
import net.hasor.dataql.sqlproc.dynamic.logic.ArrayDynamicSql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;

/**
 * Execute 包测试基类，继承 {@link AbstractSqlProcTest}，追加 SqlConfig 快捷工厂方法。
 */
public abstract class AbstractExecuteTest extends AbstractSqlProcTest {

    /** @deprecated 使用 {@link #newH2WithUsers()} */
    protected Connection newConnection() throws SQLException {
        return newH2WithUsers();
    }

    protected SqlConfig newSelectConfig(StatementType stmtType) {
        ArrayDynamicSql ads = new ArrayDynamicSql() {
            public boolean isHaveInjection() { return false; }
            public boolean isHavePlaceholder() { return false; }
            public void buildQuery(net.hasor.dataql.sqlproc.dynamic.SqlArgSource data, QueryContext ctx, SqlBuilder builder) {
                builder.appendSql("SELECT id, name, age FROM users");
            }
        };
        Hints hints = new HintsSet();
        if (stmtType != null && stmtType != StatementType.Prepared) {
            hints.setHint("statementType", stmtType.getValue());
        }
        return new SelectConfig(ads, hints);
    }

    protected SqlConfig newInsertConfig() {
        ArrayDynamicSql ads = new ArrayDynamicSql() {
            public boolean isHaveInjection() { return false; }
            public boolean isHavePlaceholder() { return false; }
            public void buildQuery(net.hasor.dataql.sqlproc.dynamic.SqlArgSource data, QueryContext ctx, SqlBuilder builder) {
                builder.appendSql("INSERT INTO users (name, age) VALUES ('Test', 99)");
            }
        };
        return new InsertConfig(ads, new HintsSet());
    }

    protected SqlConfig newUpdateConfig() {
        ArrayDynamicSql ads = new ArrayDynamicSql() {
            public boolean isHaveInjection() { return false; }
            public boolean isHavePlaceholder() { return false; }
            public void buildQuery(net.hasor.dataql.sqlproc.dynamic.SqlArgSource data, QueryContext ctx, SqlBuilder builder) {
                builder.appendSql("UPDATE users SET age = 100 WHERE name = 'Alice'");
            }
        };
        return new UpdateConfig(ads, new HintsSet());
    }

    protected SqlConfig newDeleteConfig() {
        ArrayDynamicSql ads = new ArrayDynamicSql() {
            public boolean isHaveInjection() { return false; }
            public boolean isHavePlaceholder() { return false; }
            public void buildQuery(net.hasor.dataql.sqlproc.dynamic.SqlArgSource data, QueryContext ctx, SqlBuilder builder) {
                builder.appendSql("DELETE FROM users WHERE id > 100");
            }
        };
        return new DeleteConfig(ads, new HintsSet());
    }

    protected SqlConfig newPreparedSelectConfig() {
        ArrayDynamicSql ads = new ArrayDynamicSql() {
            public boolean isHaveInjection() { return false; }
            public boolean isHavePlaceholder() { return true; }
            public void buildQuery(net.hasor.dataql.sqlproc.dynamic.SqlArgSource data, QueryContext ctx, SqlBuilder builder) {
                builder.appendSql("SELECT id, name, age FROM users WHERE name = ?", "Alice");
            }
        };
        return new SelectConfig(ads, new HintsSet());
    }

    protected SqlConfig newPreparedInsertConfig() {
        ArrayDynamicSql ads = new ArrayDynamicSql() {
            public boolean isHaveInjection() { return false; }
            public boolean isHavePlaceholder() { return true; }
            public void buildQuery(net.hasor.dataql.sqlproc.dynamic.SqlArgSource data, QueryContext ctx, SqlBuilder builder) {
                builder.appendSql("INSERT INTO users (name, age) VALUES (?, ?)", "Test", 99);
            }
        };
        return new InsertConfig(ads, new HintsSet());
    }

    protected SqlConfig newExecuteConfig() {
        ArrayDynamicSql ads = new ArrayDynamicSql() {
            public boolean isHaveInjection() { return false; }
            public boolean isHavePlaceholder() { return false; }
            public void buildQuery(net.hasor.dataql.sqlproc.dynamic.SqlArgSource data, QueryContext ctx, SqlBuilder builder) {
                builder.appendSql("SELECT id, name, age FROM users");
            }
        };
        Hints hints = new HintsSet();
        hints.setHint("bindOut", "col1,col2");
        return new ExecuteConfig(ads, hints);
    }
}
