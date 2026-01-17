/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.dataql.sqlproc.spi;
/**
 * SQL 执行前的检查。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-09-18
 */
public interface FxSqlCheckChainSpi extends java.util.EventListener {
    int NEXT = 1;// 执行下一个 Spi
    int EXIT = 2;// 退出执行

    int doCheck(FxSqlInfo infoObject) throws Throwable;

    class FxSqlInfo {
        private final boolean  batch;
        private final String   sourceName;
        private final String   queryString;
        private final Object[] queryParams;

        public FxSqlInfo(boolean batch, String sourceName, String queryString, Object[] queryParams) {
            this.batch = batch;
            this.sourceName = sourceName;
            this.queryString = queryString;
            this.queryParams = queryParams;
        }

        /** 是否为批量操作 */
        public boolean isBatch() {
            return this.batch;
        }

        /** 使用的数据源 */
        public String getSourceName() {
            return this.sourceName;
        }

        /** 计划执行的 SQL */
        public String getQueryString() {
            return this.queryString;
        }

        /** 执行 SQL 用到的参数 */
        public Object[] getQueryParams() {
            return this.queryParams.clone();
        }
    }
}