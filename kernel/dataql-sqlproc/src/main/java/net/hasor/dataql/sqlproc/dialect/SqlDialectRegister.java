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
package net.hasor.dataql.sqlproc.dialect;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import net.hasor.cobble.ClassUtils;
import net.hasor.cobble.ResourcesUtils;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.ref.LinkedCaseInsensitiveMap;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dialect.provider.*;

/**
 * 方言管理器
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-10-31
 */
public class SqlDialectRegister {
    private static final Map<String, Class<?>>    dialectAliasMap = new LinkedCaseInsensitiveMap<>();
    private static final Map<String, PageDialect> dialectCache    = new LinkedCaseInsensitiveMap<>();

    static {
        registerDialectAlias(JdbcHelper.DB2, Db2Dialect.class);
        registerDialectAlias(JdbcHelper.DERBY, DerbyDialect.class);// Apache Derby
        registerDialectAlias(JdbcHelper.DM, DmDialect.class);
        registerDialectAlias(JdbcHelper.H2, H2Dialect.class);
        //registerDialectAlias(JdbcHelper.HIVE, HiveDialect.class);
        registerDialectAlias(JdbcHelper.HSQL, HSQLDialect.class);
        registerDialectAlias(JdbcHelper.IMPALA, ImpalaDialect.class);
        registerDialectAlias(JdbcHelper.INFORMIX, InformixDialect.class);
        registerDialectAlias(JdbcHelper.KINGBASE, PostgreSqlDialect.class);
        registerDialectAlias(JdbcHelper.POSTGRESQL, PostgreSqlDialect.class);
        registerDialectAlias(JdbcHelper.MARIADB, MySqlDialect.class);
        registerDialectAlias(JdbcHelper.MYSQL, MySqlDialect.class);
        registerDialectAlias(JdbcHelper.ORACLE, OracleDialect.class);
        registerDialectAlias(JdbcHelper.SQLITE, SqlLiteDialect.class);
        registerDialectAlias(JdbcHelper.SQL_SERVER, SqlServerDialect.class);
        registerDialectAlias(JdbcHelper.JTDS, SqlServerDialect.class);
        registerDialectAlias(JdbcHelper.XUGU, XuGuDialect.class);
    }

    public static void clearDialectCache() {
        dialectCache.clear();
    }

    public static void registerDialectAlias(String dialectName, Class<? extends PageDialect> dialectClass) {
        dialectAliasMap.put(dialectName, dialectClass);
        dialectAliasMap.put(dialectClass.getName(), dialectClass);
    }

    public static PageDialect findDialect(Connection conn, Hints hints, ClassLoader loader) throws SQLException {
        // .优先从 hint 中取方言，取不到在自动推断
        String dialectName = hints.getOrDefault(SqlHintNames.FRAGMENT_SQL_PAGE_DIALECT.name(), "").toString();
        if (StringUtils.isBlank(dialectName)) {
            String jdbcUrl = conn.getMetaData().getURL();
            String jdbcDriverName = conn.getMetaData().getDriverName();
            dialectName = JdbcHelper.getDbType(jdbcUrl, jdbcDriverName);

            if (StringUtils.isBlank(dialectName)) {
                throw new IllegalArgumentException("Query dialect missing.");
            }
        }

        if (StringUtils.isBlank(dialectName)) {
            return DefaultPageDialect.DEFAULT;
        }
        PageDialect dialect = dialectCache.get(dialectName);
        if (dialect != null) {
            return dialect;
        }
        //
        loader = (loader == null) ? Thread.currentThread().getContextClassLoader() : loader;
        String lastMessage = null;
        Class<?> aClass = dialectAliasMap.get(dialectName);
        if (aClass == null) {
            try {
                aClass = ResourcesUtils.classForName(loader, dialectName);
            } catch (ClassNotFoundException e) {
                lastMessage = "load dialect '" + dialectName + "' class not found";
            }
        }
        if (aClass != null) {
            try {
                dialect = (PageDialect) ClassUtils.newInstance(aClass);
            } catch (Exception e) {
                throw new IllegalStateException("load dialect '" + aClass.getName() + "' failed, " + e.getMessage(), e);
            }
        } else {
            if (StringUtils.isNotBlank(lastMessage)) {
                throw new IllegalStateException(lastMessage);
            } else {
                throw new IllegalStateException("no dialect '" + dialectName + "' found.");
            }
        }
        //
        dialectCache.put(dialectName, dialect);
        return dialect;
    }
}
