/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dialect;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.hasor.cobble.ClassUtils;
import net.hasor.cobble.ResourcesUtils;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dialect.provider.*;

/**
 * 方言管理器
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-10-31
 */
public class SqlDialectRegister {
    private static final Map<String, Class<?>>      dialectAliasMap = new ConcurrentHashMap<>();
    private static final Map<Class<?>, PageDialect> dialectCache    = new ConcurrentHashMap<>();

    static {
        registerDialectAlias(JdbcHelper.DB2, Db2Dialect.class);
        registerDialectAlias(JdbcHelper.DERBY, DerbyDialect.class);// Apache Derby
        registerDialectAlias(JdbcHelper.DM, DmDialect.class);
        registerDialectAlias(JdbcHelper.H2, H2Dialect.class);
        registerDialectAlias(JdbcHelper.HIVE, HiveDialect.class);
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
        registerDialectAlias(JdbcHelper.CLICKHOUSE, ClickHouseDialect.class);
        registerDialectAlias(JdbcHelper.MONGO, MongoDialect.class);
        registerDialectAlias(JdbcHelper.ELASTIC6, ElasticDialect.class);
        registerDialectAlias(JdbcHelper.ELASTIC7, ElasticDialect.class);
        registerDialectAlias(JdbcHelper.ELASTIC8, ElasticDialect.class);
        registerDialectAlias(JdbcHelper.MILVUS, MilvusDialect.class);
    }

    public static void clearDialectCache() {
        dialectCache.clear();
    }

    public static void registerDialectAlias(String dialectName, Class<? extends PageDialect> dialectClass) {
        dialectAliasMap.put(dialectName.toLowerCase(Locale.ROOT), dialectClass);
        dialectAliasMap.put(dialectClass.getName().toLowerCase(Locale.ROOT), dialectClass);
    }

    public static PageDialect findDialect(Connection conn, Hints hints, ClassLoader loader) throws SQLException {
        // .优先从 hint 中取方言，取不到在自动推断
        String dialectName = hints.getOrDefault(SqlHintNames.FRAGMENT_SQL_PAGE_DIALECT.name(), "").toString();
        if (StringUtils.isBlank(dialectName)) {
            String jdbcUrl = conn.getMetaData().getURL();
            String jdbcDriverName = conn.getMetaData().getDriverName();
            dialectName = JdbcHelper.getDbType(jdbcUrl, jdbcDriverName, conn.getMetaData().getDatabaseProductVersion());

            if (StringUtils.isBlank(dialectName)) {
                throw new IllegalArgumentException("Query dialect missing.");
            }
        }

        if (StringUtils.isBlank(dialectName)) {
            return DefaultPageDialect.DEFAULT;
        }
        PageDialect dialect;
        //
        loader = (loader == null) ? Thread.currentThread().getContextClassLoader() : loader;
        String lastMessage = null;
        Class<?> aClass = dialectAliasMap.get(dialectName.toLowerCase(Locale.ROOT));
        if (aClass == null) {
            try {
                aClass = ResourcesUtils.classForName(loader, dialectName);
            } catch (ClassNotFoundException e) {
                lastMessage = "load dialect '" + dialectName + "' class not found";
            }
        }
        if (aClass != null) {
            try {
                // Cache by resolved class, so aliases and class loaders keep their identity.
                dialect = dialectCache.computeIfAbsent(aClass, type -> (PageDialect) ClassUtils.newInstance(type));
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
        return dialect;
    }
}
