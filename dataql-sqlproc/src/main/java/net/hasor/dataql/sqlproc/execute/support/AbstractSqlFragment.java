/*
 * Copyright 2015-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * ...
 */
package net.hasor.dataql.sqlproc.execute.support;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.domain.HintNames;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.kernel.FragmentProcess;
import net.hasor.dataql.sqlproc.ConfigFormatType;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dialect.PageObject;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import net.hasor.dataql.sqlproc.dynamic.resolve.ConfigResolve;
import net.hasor.dataql.sqlproc.dynamic.resolve.ConfigResolveRoot;
import net.hasor.dataql.sqlproc.execute.RootStatement;
import net.hasor.dataql.sqlproc.execute.interceptor.SqlExecutionInterceptor;
import static net.hasor.dataql.sqlproc.SqlHintNames.FRAGMENT_SQL_DATA_SOURCE;
import static net.hasor.dataql.sqlproc.SqlHintNames.FRAGMENT_SQL_QUERY_BY_PAGE;
import static net.hasor.dataql.sqlproc.SqlHintNames.FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET;
import static net.hasor.dataql.sqlproc.SqlHintValue.FRAGMENT_SQL_QUERY_BY_PAGE_ENABLE;

/**
 * SQL FragmentProcess 抽象父类。fragmentType 从 {@link FragmentProcess#runFragment(Hints, Map, String)}
 * 注入 Hints，子类覆写 {@link #queryType(String, Hints)} 读取即可。
 */
public abstract class AbstractSqlFragment implements FragmentProcess {
    private final ConfigResolveRoot           configResolve = new ConfigResolveRoot();
    private final Map<String, SqlConfig>      configCache   = new ConcurrentHashMap<>();
    private final RootStatement               rootStatement;
    private final QueryContext                queryContext;

    protected AbstractSqlFragment(QueryContext queryContext) {
        this.queryContext = Objects.requireNonNull(queryContext, "queryContext is null");
        this.rootStatement = new RootStatement(this.queryContext);
    }

    protected abstract QueryType queryType(String fragmentString, Hints hints);

    protected Hints resolveHints(Hints hints) {
        return hints;
    }

    protected ConfigFormatType resolveFormatType(String fragmentType) {
        if (StringUtils.isBlank(fragmentType)) {
            return ConfigFormatType.Text;
        }
        if (StringUtils.endsWithIgnoreCase(fragmentType, "Sql")) {
            return ConfigFormatType.Text;
        } else if (StringUtils.endsWithIgnoreCase(fragmentType, "Xml")) {
            return ConfigFormatType.Xml;
        } else {
            throw new UnsupportedOperationException("fragment type '" + fragmentType + "' Unsupported.");
        }
    }

    String buildCacheKey(String fragmentString, QueryType type, ConfigFormatType formatType, Hints hints) {
        StringBuilder cacheKey = new StringBuilder();
        cacheKey.append(fragmentString).append('|').append(type.name()).append('|').append(formatType.name());
        for (SqlHintNames hintName : ConfigResolve.CONFIG_HINTS) {
            cacheKey.append('|').append(hintName.name()).append('=').append(SqlHintNames.getValue(hints, hintName));
        }
        return cacheKey.toString();
    }

    protected SqlConfig buildConfig(String fragmentString, Hints hints) {
        Object hintValue = hints.getHint(HintNames.FRAGMENT_TYPE.name());
        String fragmentType = hintValue == null ? null : hintValue.toString();
        QueryType type = QueryType.valueOfTag(fragmentType);
        if (type == null) {
            type = this.queryType(fragmentString, hints);
        }
        QueryType queryType = type;

        ConfigFormatType formatType = this.resolveFormatType(fragmentType);
        String cacheKey = buildCacheKey(fragmentString, queryType, formatType, hints);

        String fragmentName = queryType.getTagString();
        return this.configCache.computeIfAbsent(cacheKey, k -> {
            SqlConfig config = switch (formatType) {
                case Text -> this.configResolve.parsePlainConfig(fragmentName, hints, fragmentString);
                case Xml -> this.configResolve.parseXmlConfig(fragmentName, hints, fragmentString);
                default -> throw new UnsupportedOperationException("fragment type '" + fragmentType + "' Unsupported.");
            };
            return config;
        });
    }

    // ----------------------------------------------------------------
    // Execution
    // ----------------------------------------------------------------

    @Override
    public Object runFragment(Hints hints, Map<String, Object> params, String fragmentString) throws Throwable {
        return this.executeFragment(hints, params, fragmentString);
    }

    protected Object executeFragment(Hints hints, Map<String, Object> params, String fragmentString) throws Throwable {
        Hints fragmentHints = this.resolveHints(hints);
        SqlConfig config = buildConfig(fragmentString, fragmentHints);
        List<SqlExecutionInterceptor> interceptors = this.queryContext.filterInterceptors(config.getType(), fragmentString, fragmentHints);
        FragmentConfig fragmentConfig = new FragmentConfig(config, interceptors);

        String byPage = SqlHintNames.getValue(fragmentHints, FRAGMENT_SQL_QUERY_BY_PAGE);
        if (config.getType() == QueryType.Select && StringUtils.equalsIgnoreCase(FRAGMENT_SQL_QUERY_BY_PAGE_ENABLE, byPage)) {
            return this.usePageFragment(fragmentHints, params, fragmentConfig);
        } else {
            return this.noPageFragment(fragmentHints, params, config, interceptors);
        }
    }

    protected Object noPageFragment(Hints hints, Map<String, Object> params, SqlConfig config, List<SqlExecutionInterceptor> interceptors) throws SQLException {
        String sourceName = SqlHintNames.getValue(hints, FRAGMENT_SQL_DATA_SOURCE);
        try (Connection conn = this.queryContext.findConnection(sourceName, hints)) {
            return this.rootStatement.execute(conn, hints, config, params, null, false, interceptors);
        }
    }

    protected Object usePageFragment(Hints hints, Map<String, Object> params, FragmentConfig fragmentConfig) {
        PageObject page = new PageObject(1, -1);
        String offsetStr = SqlHintNames.getValue(hints, FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET);
        if (StringUtils.isNotBlank(offsetStr)) {
            page.setPageNumberOffset(Integer.parseInt(offsetStr));
        }

        return new PageQuery(this.queryContext, fragmentConfig, hints, params, this.rootStatement, page);
    }
}
