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
package net.hasor.dataql.fx.db;
/**
 * SQL 执行前的检查。
 * @see net.hasor.dataql.sqlproc.spi.FxSqlCheckChainSpi
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-09-18
 */
@Deprecated
public interface FxSqlCheckChainSpi extends net.hasor.dataql.sqlproc.spi.FxSqlCheckChainSpi, java.util.EventListener {
    int NEXT = net.hasor.dataql.sqlproc.spi.FxSqlCheckChainSpi.NEXT;// 执行下一个 Spi
    int EXIT = net.hasor.dataql.sqlproc.spi.FxSqlCheckChainSpi.EXIT;// 退出执行

    int doCheck(FxSqlInfo infoObject) throws Throwable;

    @Override
    default int doCheck(net.hasor.dataql.sqlproc.spi.FxSqlCheckChainSpi.FxSqlInfo infoObject) throws Throwable {
        return this.doCheck(new FxSqlInfo(infoObject));
    }

    @Deprecated
    class FxSqlInfo extends net.hasor.dataql.sqlproc.spi.FxSqlCheckChainSpi.FxSqlInfo {

        public FxSqlInfo(net.hasor.dataql.sqlproc.spi.FxSqlCheckChainSpi.FxSqlInfo fxSqlInfo) {
            super(fxSqlInfo.isBatch(), fxSqlInfo.getSourceName(), fxSqlInfo.getQueryString(), fxSqlInfo.getQueryParams());
        }

        public FxSqlInfo(boolean batch, String sourceName, String queryString, Object[] queryParams) {
            super(batch, sourceName, queryString, queryParams);
        }

        /** 是否为批量操作 */
        public boolean isBatch() {
            return super.isBatch();
        }

        /** 使用的数据源 */
        public String getSourceName() {
            return super.getSourceName();
        }

        /** 计划执行的 SQL */
        public String getQueryString() {
            return super.getQueryString();
        }

        /** 执行 SQL 用到的参数 */
        public Object[] getQueryParams() {
            return super.getQueryParams();
        }
    }
}