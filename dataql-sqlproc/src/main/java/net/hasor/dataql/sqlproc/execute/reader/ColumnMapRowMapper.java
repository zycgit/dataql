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
package net.hasor.dataql.sqlproc.execute.reader;
import net.hasor.cobble.ResourcesUtils;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.Hints;
import net.hasor.dataql.sqlproc.ColumnCaseType;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.types.TypeHandler;
import net.hasor.dataql.sqlproc.types.TypeHandlerRegistry;

import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 赵永春 (zyc@hasor.net)
 * @since 1.2
 */
public class ColumnMapRowMapper implements ResultSetExtractor {
    private final TypeHandlerRegistry typeRegistry;
    private final ColumnCaseType      caseType;

    public ColumnMapRowMapper(Hints hints, TypeHandlerRegistry typeRegistry) {
        String caseTypeStr = SqlHintNames.getValue(hints, SqlHintNames.FRAGMENT_SQL_COLUMN_CASE);

        this.typeRegistry = typeRegistry;
        this.caseType = ColumnCaseType.valueOfCode(caseTypeStr);
    }

    @Override
    public List<Map<String, Object>> extractData(ResultSet rs) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();

        // fetch columnNames
        List<String> colNames = new ArrayList<>();
        Map<String, Integer> colNameIndex = new LinkedHashMap<>();
        for (int i = 1; i <= columnCount; i++) {
            String key = this.getColumnKey(rsmd, i);
            if (colNames.contains(key)) {
                continue;
            }

            colNames.add(key);
            colNameIndex.put(key, i);
        }

        // fetch values
        List<Map<String, Object>> results = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (String colName : colNames) {
                row.put(colName, this.getColumnValue(rs, colNameIndex.get(colName)));
            }
            results.add(row);
        }
        return results;
    }

    protected String getColumnKey(final ResultSetMetaData rsmd, final int index) throws SQLException {
        String name = rsmd.getColumnLabel(index);
        if (name == null || name.isEmpty()) {
            name = rsmd.getColumnName(index);
        }

        switch (this.caseType) {
            case ColumnCaseHump:
                return StringUtils.lineToHump(name);
            case ColumnCaseLower:
                return name.toLowerCase();
            case ColumnCaseUpper:
                return name.toUpperCase();
            case ColumnCaseDefault:
            default:
                return name;
        }
    }

    /** 取得指定列的值 */
    protected Object getColumnValue(final ResultSet rs, final int index) throws SQLException {
        return getResultSetTypeHandler(rs, index).getResult(rs, index);
    }

    /** 获取读取列用到的那个 TypeHandler */
    public TypeHandler getResultSetTypeHandler(ResultSet rs, int columnIndex) throws SQLException {
        int jdbcType = rs.getMetaData().getColumnType(columnIndex);
        String columnTypeName = rs.getMetaData().getColumnTypeName(columnIndex);
        String columnClassName = rs.getMetaData().getColumnClassName(columnIndex);

        if ("YEAR".equalsIgnoreCase(columnTypeName)) {
            // TODO with mysql `YEAR` type, columnType is DATE. but getDate() throw Long cast Date failed.
            jdbcType = JDBCType.INTEGER.getVendorTypeNumber();
        } else if (StringUtils.isNotBlank(columnClassName) && columnClassName.startsWith("oracle.")) {
            // TODO with oracle columnClassName is specifically customizes standard types, it specializes process.
            jdbcType = TypeHandlerRegistry.toSqlType(columnClassName);
            return this.typeRegistry.getTypeHandler(jdbcType);
        }

        Class<?> columnTypeClass = null;
        try {
            columnTypeClass = ResourcesUtils.classForName(columnClassName);
        } catch (ClassNotFoundException e) {
            /**/
        }

        if (this.typeRegistry.hasTypeHandler(columnTypeClass)) {
            return this.typeRegistry.getTypeHandler(columnTypeClass);
        } else {
            TypeHandler typeHandler = this.typeRegistry.getTypeHandler(columnTypeClass);
            if (typeHandler == null) {
                String message = "jdbcType=" + jdbcType + " ,columnTypeClass=" + columnTypeClass;
                throw new SQLException("no typeHandler is matched to any available " + message);
            }
            return typeHandler;
        }
    }
}
