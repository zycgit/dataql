/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute.fragment;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import net.hasor.cobble.convert.ConverterUtils;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.host.function.AbstractUdfSource;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dialect.PageObject;
import net.hasor.dataql.sqlproc.dialect.PageResult;
import net.hasor.dataql.sqlproc.execute.RootStatement;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContext;

/**
 * 延迟分页查询对象，继承 {@link AbstractUdfSource} 使其可在 DataQL 脚本中调用。
 *
 * <pre>{@code
 *   // DataQL 脚本中：
 *   var pageQuery = sqlFragment.runFragment(hints, params, "SELECT * FROM users");
 *   pageQuery.setPageInfo({ "pageSize": 10, "currentPage": 1 });
 *   return pageQuery.data();
 * }</pre>
 */
public class PageQuery extends AbstractUdfSource {
    private final ExecuteContext      context;
    private final Hints               hints;
    private final FragmentConfig      sqlConfig;
    private final Map<String, Object> params;
    private final RootStatement       rootStatement;
    private final PageObject          pageInfo;
    private       PageResult<Object>  pageResult;

    PageQuery(ExecuteContext context, FragmentConfig sqlConfig, Hints hints, Map<String, Object> params, RootStatement rootStatement, PageObject pageInfo) {
        this.context = context;
        this.hints = hints;
        this.params = params;
        this.rootStatement = rootStatement;
        this.pageInfo = pageInfo;
        this.sqlConfig = sqlConfig;
    }

    @Override
    public <T> T get(Class<? extends T> targetType) {
        return targetType.cast(this);
    }

    // ----------------------------------------------------------------
    // Page navigation
    // ----------------------------------------------------------------

    public long firstPage() {
        this.pageInfo.firstPage();
        return this.pageInfo.getCurrentPage();
    }

    public long previousPage() {
        this.pageInfo.previousPage();
        return this.pageInfo.getCurrentPage();
    }

    public long nextPage() {
        this.pageInfo.nextPage();
        return this.pageInfo.getCurrentPage();
    }

    public long lastPage() {
        this.pageInfo.lastPage();
        return this.pageInfo.getCurrentPage();
    }

    // ----------------------------------------------------------------
    // Page info
    // ----------------------------------------------------------------

    /** 获取分页信息（首次调用时自动执行 count 查询获取总记录数） */
    public Map<String, Object> pageInfo() throws SQLException {
        if (this.pageResult == null && this.pageInfo.getTotalCount() <= 0) {
            this.fetchData();
        }
        return this.pageInfo.toPageInfo();
    }

    /** 设置分页参数，支持 pageSize / currentPage / totalCount */
    public boolean setPageInfo(Map<String, Object> info) {
        if (info == null || info.isEmpty()) {
            return false;
        }
        Object currentPage = info.get("currentPage");
        Object pageSize = info.get("pageSize");
        Object totalCount = info.get("totalCount");
        if (currentPage == null && pageSize == null) {
            return false;
        }
        if (currentPage != null) {
            this.pageInfo.setCurrentPage((Integer) ConverterUtils.convert(Integer.TYPE, currentPage));
        }
        if (pageSize != null) {
            this.pageInfo.setPageSize((Integer) ConverterUtils.convert(Integer.TYPE, pageSize));
        }
        if (totalCount != null) {
            this.pageInfo.setTotalCount((Long) ConverterUtils.convert(Long.TYPE, totalCount));
        }
        return true;
    }

    // ----------------------------------------------------------------
    // Data fetch
    // ----------------------------------------------------------------

    /** 获取当前页数据（触发实际 SQL 执行） */
    public Object data() throws SQLException {
        this.fetchData();
        return this.pageResult.getData();
    }

    private void fetchData() throws SQLException {
        String sourceName = SqlHintNames.getValue(hints, SqlHintNames.FRAGMENT_SQL_DATA_SOURCE);
        try (Connection conn = this.context.findConnection(sourceName, this.hints)) {
            this.pageResult = (PageResult<Object>) this.rootStatement.execute(conn, this.hints, this.sqlConfig.config(), this.params, this.pageInfo, true, this.sqlConfig.interceptors());
            // Keep metadata in sync so pageInfo() does not execute the page again.
            this.pageInfo.setTotalCount(this.pageResult.getTotalCount());
        }
    }
}
