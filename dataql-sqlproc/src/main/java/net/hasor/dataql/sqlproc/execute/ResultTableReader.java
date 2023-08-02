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
package net.hasor.dataql.sqlproc.execute;
import net.hasor.cobble.ResourcesUtils;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.ref.LinkedCaseInsensitiveMap;
import net.hasor.dataql.sqlproc.types.TypeHandler;
import net.hasor.dataql.sqlproc.types.TypeHandlerRegistry;

import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Map 化 TableReader
 * @version : 2021-07-20
 * @author 赵永春 (zyc@hasor.net)
 */
public class ResultTableReader {
    private final TypeHandlerRegistry handlerRegistry;
    private final boolean             caseInsensitive;

    public ResultTableReader(boolean caseInsensitive, TypeHandlerRegistry typeHandler) {
        this.handlerRegistry = Objects.requireNonNull(typeHandler, "typeHandler is null.");
        this.caseInsensitive = caseInsensitive;
    }

    public TypeHandlerRegistry getHandlerRegistry() {
        return this.handlerRegistry;
    }

    /** 获取读取列用到的那个 TypeHandler */
    public TypeHandler<?> getResultSetTypeHandler(ResultSet rs, int columnIndex, Class<?> targetType) throws SQLException {
        int jdbcType = rs.getMetaData().getColumnType(columnIndex);
        String columnTypeName = rs.getMetaData().getColumnTypeName(columnIndex);
        String columnClassName = rs.getMetaData().getColumnClassName(columnIndex);

        if ("YEAR".equalsIgnoreCase(columnTypeName)) {
            // TODO with mysql `YEAR` type, columnType is DATE. but getDate() throw Long cast Date failed.
            jdbcType = JDBCType.INTEGER.getVendorTypeNumber();
        } else if (StringUtils.isNotBlank(columnClassName) && columnClassName.startsWith("oracle.")) {
            // TODO with oracle columnClassName is specifically customizes standard types, it specializes process.
            jdbcType = TypeHandlerRegistry.toSqlType(columnClassName);
            if (targetType != null) {
                return this.handlerRegistry.getJavaTypeHandler(targetType, jdbcType);
            } else {
                return this.handlerRegistry.getJdbcTypeHandler(jdbcType);
            }
        }

        Class<?> columnTypeClass = targetType;
        if (columnTypeClass == null) {
            try {
                columnTypeClass = ResourcesUtils.classForName(columnClassName);
            } catch (ClassNotFoundException e) {
                /**/
            }
        }
        TypeHandler<?> typeHandler = this.handlerRegistry.getJavaTypeHandler(columnTypeClass, jdbcType);
        if (typeHandler == null) {
            String message = "jdbcType=" + jdbcType + " ,columnTypeClass=" + columnTypeClass;
            throw new SQLException("no typeHandler is matched to any available " + message);
        }
        return typeHandler;
    }

    /** 创建一个 Map 用于存放数据 */
    protected Map<String, Object> createColumnMap(final int columnCount) {
        if (this.caseInsensitive) {
            return new LinkedCaseInsensitiveMap<>(columnCount);
        } else {
            return new LinkedHashMap<>();
        }
    }

    public Map<String, Object> extractRow(List<String> columns, ResultSet rs, int rowNum) throws SQLException {
        List<TypeHandler<?>> handlers = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            handlers.add(this.getResultSetTypeHandler(rs, i + 1, null));
        }

        return this.extractRow(columns, handlers, rs, rowNum);
    }

    protected Map<String, Object> extractRow(List<String> columns, List<TypeHandler<?>> handlers, ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> target = this.createColumnMap(columns.size());

        for (int i = 0; i < columns.size(); i++) {
            String columnName = columns.get(i);
            TypeHandler<?> handler = handlers.get(i);
            if (handler == null) {
                handler = this.getHandlerRegistry().getDefaultTypeHandler();
            }

            Object result = handler.getResult(rs, i + 1);
            target.put(columnName, result);
        }

        return target;
    }
}