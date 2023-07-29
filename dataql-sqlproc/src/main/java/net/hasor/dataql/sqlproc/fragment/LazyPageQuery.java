/*
 * Copyright 2008-2009 the original author or authors.
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
package net.hasor.dataql.sqlproc.fragment;
import net.hasor.cobble.convert.ConverterUtils;
import net.hasor.dataql.Hints;
import net.hasor.dataql.UdfSourceAssembly;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dialect.PageDialect;
import net.hasor.dataql.sqlproc.execute.ColumnCaseType;
import net.hasor.dataql.sqlproc.execute.ExecuteProxy;
import net.hasor.dataql.sqlproc.execute.OpenPackageType;
import net.hasor.dataql.sqlproc.execute.page.Page;
import net.hasor.dataql.sqlproc.execute.page.PageObject;
import net.hasor.dataql.sqlproc.execute.page.PageResult;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import static net.hasor.dataql.sqlproc.SqlHintNames.FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET;

/**
 * 翻页数据，同时负责调用分页的SQL执行分页查询
 * @version : 2014年10月25日
 * @author 赵永春 zyc@hasor.net
 */
class LazyPageQuery implements UdfSourceAssembly {

    private final Hints               hints;
    private final ExecuteProxy        execute;
    private final Map<String, Object> params;
    private       Page                pageInfo;
    private       PageResult<Object>  pageResult;
    private final SqlFragment         fragment;

    public LazyPageQuery(Hints hints, ExecuteProxy execute, Map<String, Object> params, SqlFragment fragment) {
        this.hints = hints;
        this.execute = execute;
        this.params = params;
        this.fragment = fragment;
        this.pageInfo = initPageFormHints(hints);
    }

    protected Page initPageFormHints(Hints hints) {
        Page pageInfo = new PageObject(1, -1);
        Object pageOffset = hints.getOrDefault(FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET.name(), FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET.getDefaultVal());
        pageInfo.setPageNumberOffset(Integer.parseInt(pageOffset.toString()));
        return pageInfo;
    }
    // ----------------------------------------------------

    /** 移动到第一页 */
    public long firstPage() {
        this.pageInfo.firstPage();
        return this.pageInfo.getCurrentPage();
    }

    /** 移动到上一页 */
    public long previousPage() {
        this.pageInfo.previousPage();
        return this.pageInfo.getCurrentPage();
    }

    /** 移动到下一页 */
    public long nextPage() {
        this.pageInfo.nextPage();
        return this.pageInfo.getCurrentPage();
    }

    /** 移动到最后一页 */
    public long lastPage() {
        this.pageInfo.lastPage();
        return this.pageInfo.getCurrentPage();
    }

    /** 获取分页的页大小 */
    public Map<String, Object> pageInfo() throws SQLException {
        final Page usingInfo;
        if (this.pageInfo.getTotalCount() > 0) {
            usingInfo = this.pageInfo;
        } else {
            usingInfo = this.fetchData(this.pageInfo, true);
            this.pageInfo.setTotalCount(usingInfo.getTotalCount());
        }

        return new LinkedHashMap<String, Object>() {{
            put("enable", usingInfo.getPageSize() > 0);
            put("pageSize", usingInfo.getPageSize());
            put("totalCount", usingInfo.getTotalCount());
            put("totalPage", usingInfo.getTotalPage());
            put("currentPage", usingInfo.getCurrentPage());
            put("recordPosition", usingInfo.getFirstRecordPosition());
        }};
    }
    // ----------------------------------------------------------------------------------

    /** 设置分页信息和总记录数 */
    public boolean setPageInfo(Map<String, Object> pageInfo) {
        if (pageInfo == null || pageInfo.isEmpty()) {
            return false;
        }
        Object currentPage = pageInfo.get("currentPage");
        Object pageSize = pageInfo.get("pageSize");
        Object totalCount = pageInfo.get("totalCount");
        if (currentPage == null && pageSize == null) {
            return false;
        }
        this.pageInfo.setCurrentPage(((Integer) ConverterUtils.convert(Integer.TYPE, currentPage)));
        this.pageInfo.setPageSize(((Integer) ConverterUtils.convert(Integer.TYPE, pageSize)));
        if (totalCount != null) {
            this.pageInfo.setTotalCount((Long) ConverterUtils.convert(Long.TYPE, totalCount));
        }
        return true;
    }

    /** 获取数据 */
    public Object data() throws SQLException {
        return fetchData(this.pageInfo, false).getData();
    }

    private PageResult<Object> fetchData(Page pageInfo, boolean onlyPageCount) throws SQLException {
        try (Connection con = this.fragment.fetchConnection(hints)) {
            PageDialect dialect = this.fragment.fetchDialect(hints, con);
            OpenPackageType resultType = OpenPackageType.valueOfCode(hints.getOrDefault(SqlHintNames.FRAGMENT_SQL_OPEN_PACKAGE.name(), SqlHintNames.FRAGMENT_SQL_OPEN_PACKAGE.getDefaultVal()).toString());
            ColumnCaseType columnCaseType = ColumnCaseType.valueOfCode(hints.getOrDefault(SqlHintNames.FRAGMENT_SQL_COLUMN_CASE.name(), SqlHintNames.FRAGMENT_SQL_COLUMN_CASE.getDefaultVal()).toString());

            boolean usePageCnt = pageInfo.getTotalCount() <= 0;
            Object oriResult = this.execute.execute(con, this.params, !onlyPageCount, usePageCnt, pageInfo, dialect, resultType, columnCaseType);
            this.pageResult = (PageResult<Object>) oriResult;
            return this.pageResult;
        }
    }
}
