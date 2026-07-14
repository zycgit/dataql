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
package net.hasor.dataql.sqlproc.execute.support;
import java.sql.Connection;
import java.sql.SQLException;
import net.hasor.dataql.domain.Hints;

/**
 * 为 DataQL SQL 执行提供指定数据源名称对应的数据库连接。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-06-03
 */
@FunctionalInterface
public interface ConnectionProvider {
    /**
     * @param sourceName 数据源名称
     * @param hints 当前执行携带的参数
     * @return 数据库连接，返回 {@code null} 表示当前 Provider 无法提供
     * @throws SQLException 获取连接失败
     */
    Connection findConnection(String sourceName, Hints hints) throws SQLException;
}
