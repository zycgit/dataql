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
import java.sql.ResultSetMetaData;
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

    /** 获取列的值 */
    protected Object getResultSetValue(ResultSet rs, int columnIndex) throws SQLException {
        return getResultSetTypeHandler(rs, columnIndex, null).getResult(rs, columnIndex);
    }

    /** 获取列的值 */
    protected Object getResultSetValue(ResultSet rs, int columnIndex, Class<?> targetType) throws SQLException {
        TypeHandler<?> typeHandler = getResultSetTypeHandler(rs, columnIndex, targetType);
        return typeHandler.getResult(rs, columnIndex);
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
                return this.handlerRegistry.getTypeHandler(targetType, jdbcType);
            } else {
                return this.handlerRegistry.getTypeHandler(jdbcType);
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
        TypeHandler<?> typeHandler = this.handlerRegistry.getTypeHandler(columnTypeClass, jdbcType);
        if (typeHandler == null) {
            String message = "jdbcType=" + jdbcType + " ,columnTypeClass=" + columnTypeClass;
            throw new SQLException("no typeHandler is matched to any available " + message);
        }
        return typeHandler;
    }

    public boolean isCaseInsensitive() {
        return this.caseInsensitive;
    }

    public final Map<String, Object> mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();
        Map<String, Object> mapOfColValues = this.createColumnMap(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            String key = this.getColumnKey(rsmd, i);
            Object obj = this.getColumnValue(rs, i);
            if (!mapOfColValues.containsKey(key)) {
                mapOfColValues.put(key, obj);
            }
        }
        return mapOfColValues;
    }

    protected String getColumnKey(final ResultSetMetaData rsmd, final int index) throws SQLException {
        String name = rsmd.getColumnLabel(index);
        if (name == null || name.length() < 1) {
            name = rsmd.getColumnName(index);
        }
        return name;
    }

    /** 取得指定列的值 */
    protected Object getColumnValue(final ResultSet rs, final int index) throws SQLException {
        return getResultSetValue(rs, index);
    }

    /** 创建一个 Map 用于存放数据 */
    protected Map<String, Object> createColumnMap(final int columnCount) {
        if (this.caseInsensitive) {
            return new LinkedCaseInsensitiveMap<>(columnCount);
        } else {
            return new LinkedHashMap<>();
        }
    }

    public List<Map<String, Object>> extractData(ResultSet rs) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();

        List<String> columns = new ArrayList<>();
        List<TypeHandler<?>> handlers = new ArrayList<>();
        for (int i = 0; i < rsmd.getColumnCount(); i++) {
            columns.add(this.getColumnKey(rsmd, i));
            handlers.add(this.getResultSetTypeHandler(rs, i + 1, null));
        }

        List<Map<String, Object>> results = new ArrayList<>();
        int rowNum = 0;
        while (rs.next()) {
            results.add(this.extractRow(columns, handlers, rs, rowNum++));
        }
        return results;
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