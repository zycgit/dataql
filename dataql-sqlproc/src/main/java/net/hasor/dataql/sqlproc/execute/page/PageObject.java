/*
 * Copyright 2002-2005 the original author or authors.
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
package net.hasor.dataql.sqlproc.execute.page;
/**
 * 分页接口 Page 的实现类
 * @version : 2021-02-04
 * @author 赵永春 (zyc@hasor.net)
 */
public class PageObject implements Page {
    /** 满足条件的总记录数 */
    private long totalCount       = -1;
    /** 每页记录数（-1表示无限大）*/
    private long pageSize         = 0;
    /** 当前页号 */
    private long currentPage      = 0;
    /** 起始页码的偏移量 */
    private long pageNumberOffset = 0;

    public PageObject(long pageSize, long totalCount) {
        this.pageSize = pageSize;
        this.totalCount = totalCount;
    }

    public PageObject(Page pageInfo) {
        this.totalCount = pageInfo.getTotalCount();
        this.pageSize = pageInfo.getPageSize();
        this.currentPage = pageInfo.getCurrentPage();
        this.pageNumberOffset = pageInfo.getPageNumberOffset();
    }

    public long getPageSize() {
        return this.pageSize;
    }

    /** 设置分页的页大小 */
    public void setPageSize(long pageSize) {
        this.pageSize = Math.max(pageSize, 0);
    }

    /**取当前页号 */
    public long getCurrentPage() {
        if (this.pageSize > 0) {
            return this.currentPage + this.pageNumberOffset;
        } else {
            return this.pageNumberOffset;
        }
    }

    /** 设置前页号 */
    public void setCurrentPage(long currentPage) {
        if (currentPage <= this.pageNumberOffset) {
            this.currentPage = 0;
        } else {
            this.currentPage = currentPage - this.pageNumberOffset;
        }
    }

    public long getPageNumberOffset() {
        return this.pageNumberOffset;
    }

    public void setPageNumberOffset(long pageNumberOffset) {
        this.pageNumberOffset = Math.max(pageNumberOffset, 0);
    }

    /** 获取本页第一个记录的索引位置 */
    public long getFirstRecordPosition() {
        long pgSize = getPageSize();
        if (pgSize <= 0) {
            return 0;
        }
        return pgSize * this.currentPage;
    }

    /** 获取总页数 */
    public long getTotalPage() {
        long pgSize = getPageSize();
        if (pgSize > 0) {
            long totalCount = getTotalCount();
            if (totalCount == 0) {
                return this.pageNumberOffset;
            }
            long result = totalCount / pgSize;
            if ((totalCount % pgSize) != 0) {
                result++;
            }
            return result + this.pageNumberOffset;
        } else {
            long totalCount = getTotalCount();
            if (totalCount > 0) {
                return this.pageNumberOffset + 1;
            } else {
                return this.pageNumberOffset;
            }
        }
    }

    /** 获取记录总数 */
    public long getTotalCount() {
        return this.totalCount;
    }

    @Override
    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }
}
