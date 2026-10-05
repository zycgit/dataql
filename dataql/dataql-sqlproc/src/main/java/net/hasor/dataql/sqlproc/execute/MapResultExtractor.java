/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.sqlproc.ColumnCaseType;
import net.hasor.dataql.sqlproc.types.TypeHandler;
import net.hasor.dataql.sqlproc.types.TypeHandlerRegistry;

/**
 * @author 赵永春 (zyc@hasor.net)
 * @since 1.2
 */
public class MapResultExtractor {
    private final TypeHandlerRegistry typeRegistry;

    public MapResultExtractor(TypeHandlerRegistry typeRegistry) {
        this.typeRegistry = typeRegistry;
    }

    public List<Map<String, Object>> extractData(ColumnCaseType caseType, ResultSet rs) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();

        // fetch columnNames
        List<String> colNames = new ArrayList<>();
        Map<String, Integer> colNameIndex = new LinkedHashMap<>();
        for (int i = 1; i <= columnCount; i++) {
            String key = this.getColumnKey(caseType, rsmd, i);
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

    protected String getColumnKey(ColumnCaseType caseType, ResultSetMetaData rsmd, final int index) throws SQLException {
        String name = rsmd.getColumnLabel(index);
        if (name == null || name.isEmpty()) {
            name = rsmd.getColumnName(index);
        }

        switch (caseType) {
            case ColumnCaseHump: {
                return StringUtils.lineToHump(name);
            }
            case ColumnCaseLower: {
                return name.toLowerCase();
            }
            case ColumnCaseUpper: {
                return name.toUpperCase();
            }
            case ColumnCaseDefault:
            default: {
                return name;
            }
        }
    }

    /** 取得指定列的值 */
    protected Object getColumnValue(final ResultSet rs, final int index) throws SQLException {
        return getResultSetTypeHandler(rs, index).getResult(rs, index);
    }

    /** 获取读取列用到的那个 TypeHandler */
    public TypeHandler getResultSetTypeHandler(ResultSet rs, int columnIndex) throws SQLException {
        return this.typeRegistry.getResultSetTypeHandler(rs, columnIndex, null);
    }
}
