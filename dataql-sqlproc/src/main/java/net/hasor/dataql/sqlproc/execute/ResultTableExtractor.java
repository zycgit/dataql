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
import net.hasor.cobble.logging.Logger;
import net.hasor.cobble.logging.LoggerFactory;
import net.hasor.dataql.sqlproc.ColumnCaseType;
import net.hasor.dataql.sqlproc.OpenPackageType;
import net.hasor.dataql.sqlproc.repository.MultipleResultsType;
import net.hasor.dataql.sqlproc.types.TypeHandler;
import net.hasor.dataql.sqlproc.types.TypeHandlerRegistry;

import java.sql.*;
import java.util.*;

/**
 * 该类会将结果集中的每一行进行处理，并返回一个 List 用以封装处理结果集。
 * @author 赵永春 (zyc@hasor.net)
 */
public class ResultTableExtractor {
    private static final Logger              logger = LoggerFactory.getLogger(ResultTableExtractor.class);
    private final        OpenPackageType     openPackage;
    private final        ColumnCaseType      caseModule;
    private final        MultipleResultsType processType;
    private final        TypeHandlerRegistry typeHandler;

    public ResultTableExtractor(OpenPackageType openPackage, ColumnCaseType caseModule, MultipleResultsType processType, TypeHandlerRegistry typeHandler) {
        this.openPackage = openPackage;
        this.caseModule = caseModule;
        this.processType = processType == null ? MultipleResultsType.ALL : processType;
        this.typeHandler = typeHandler;
    }

    public List<Object> doResult(boolean retVal, Statement stmt) throws SQLException {
        if (logger.isTraceEnabled()) {
            logger.trace("statement.execute() returned '" + retVal + "'");
        }

        List<Object> resultList = new ArrayList<>();
        if (retVal) {
            try (ResultSet resultSet = stmt.getResultSet()) {
                resultList.add(processResultSet(resultSet));
            }
        } else {
            resultList.add(stmt.getUpdateCount());
        }

        if (this.processType == MultipleResultsType.FIRST) {
            return resultList;
        }

        while ((stmt.getMoreResults()) || (stmt.getUpdateCount() != -1)) {
            int updateCount = stmt.getUpdateCount();
            Object last = null;
            try (ResultSet resultSet = stmt.getResultSet()) {
                if (resultSet != null) {
                    last = processResultSet(resultSet);
                } else {
                    last = updateCount;
                }
            }

            if (this.processType == MultipleResultsType.LAST) {
                resultList.set(0, last);
            } else {
                resultList.add(last);
            }
        }
        return resultList;
    }

    /**
     * Process the given ResultSet from a stored procedure.
     * @param rs the ResultSet to process
     * @return a Map that contains returned results
     */
    protected Object processResultSet(ResultSet rs) throws SQLException {
        if (rs == null) {
            return null;
        }

        ResultSetMetaData rsmd = rs.getMetaData();
        int nrOfColumns = rsmd.getColumnCount();

        Map<String, Integer> columns = new LinkedHashMap<>();
        Map<Integer, TypeHandler<?>> typeHandlers = new HashMap<>();

        for (int i = 1; i <= nrOfColumns; i++) {
            String name = rsmd.getColumnLabel(i);
            if (name == null || name.length() < 1) {
                name = rsmd.getColumnName(i);
            }

            String useColumn = convertColumn(name);
            if (columns.containsKey(useColumn)) {
                continue;
            }

            columns.put(useColumn, i);
            typeHandlers.put(i, this.getResultSetTypeHandler(rsmd, i, null));
        }

        List<Map<String, Object>> results = new ArrayList<>();
        int rowNum = 0;
        while (rs.next()) {
            results.add(this.extractRow(columns, typeHandlers, rs, rowNum++));
        }

        return convertResult(results);
    }

    protected Map<String, Object> extractRow(Map<String, Integer> columns, Map<Integer, TypeHandler<?>> typeHandlers, ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> target = new LinkedHashMap<>();

        for (Map.Entry<String, Integer> colEnt : columns.entrySet()) {
            String mapKey = colEnt.getKey();
            int colIndex = colEnt.getValue();
            TypeHandler<?> colHandler = typeHandlers.get(colIndex);

            if (colHandler == null) {
                colHandler = this.typeHandler.getDefaultTypeHandler();
            }

            Object result = colHandler.getResult(rs, colIndex);
            target.put(mapKey, result);
        }

        return target;
    }

    /** Key名转换 */
    private String convertColumn(String key) {
        switch (this.caseModule) {
            case ColumnCaseUpper:
                return key.toUpperCase();
            case ColumnCaseLower:
                return key.toLowerCase();
            case ColumnCaseHump:
                return StringUtils.lineToHump(key.toLowerCase());
            case ColumnCaseDefault:
            default:
                return key;
        }
    }

    /** 获取读取列用到的那个 TypeHandler */
    private TypeHandler<?> getResultSetTypeHandler(ResultSetMetaData rsmd, int columnIndex, Class<?> targetType) throws SQLException {
        int jdbcType = rsmd.getColumnType(columnIndex);
        String columnTypeName = rsmd.getColumnTypeName(columnIndex);
        String columnClassName = rsmd.getColumnClassName(columnIndex);

        if ("YEAR".equalsIgnoreCase(columnTypeName)) {
            // TODO with mysql `YEAR` type, columnType is DATE. but getDate() throw Long cast Date failed.
            jdbcType = JDBCType.INTEGER.getVendorTypeNumber();
        } else if (StringUtils.isNotBlank(columnClassName) && columnClassName.startsWith("oracle.")) {
            // TODO with oracle columnClassName is specifically customizes standard types, it specializes process.
            jdbcType = TypeHandlerRegistry.toSqlType(columnClassName);
            if (targetType != null) {
                return this.typeHandler.getJavaTypeHandler(targetType, jdbcType);
            } else {
                return this.typeHandler.getJdbcTypeHandler(jdbcType);
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
        TypeHandler<?> typeHandler = this.typeHandler.getJavaTypeHandler(columnTypeClass, jdbcType);
        if (typeHandler == null) {
            String message = "jdbcType=" + jdbcType + " ,columnTypeClass=" + columnTypeClass;
            throw new SQLException("no typeHandler is matched to any available " + message);
        }
        return typeHandler;
    }

    /** 结果转换 */
    protected Object convertResult(List<Map<String, Object>> mapList) {
        // .结果有多条记录,或者模式为 off，那么直接返回List
        if (OpenPackageType.Off == this.openPackage || (mapList != null && mapList.size() > 1)) {
            return mapList;
        }

        // .为空或者结果为空，那么看看是返回 null 或者 空对象
        if (mapList == null || mapList.isEmpty()) {
            if (OpenPackageType.Column == this.openPackage) {
                return null;
            } else {
                return Collections.emptyMap();
            }
        }

        // .只有1条记录
        Map<String, Object> rowObject = mapList.get(0);
        if (OpenPackageType.Column == openPackage) {
            if (rowObject == null) {
                return null;
            }
            if (rowObject.size() == 1) {
                Set<Map.Entry<String, Object>> entrySet = rowObject.entrySet();
                Map.Entry<String, Object> objectEntry = entrySet.iterator().next();
                return objectEntry.getValue();
            }
        }
        return rowObject;
    }
}
