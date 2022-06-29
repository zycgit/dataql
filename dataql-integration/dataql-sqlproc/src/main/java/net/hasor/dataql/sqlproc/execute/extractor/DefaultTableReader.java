package net.hasor.dataql.sqlproc.execute.extractor;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.ref.LinkedCaseInsensitiveMap;
import net.hasor.dataql.sqlproc.dynamic.DynamicContext;
import net.hasor.dataql.sqlproc.types.TypeHandler;
import net.hasor.dataql.sqlproc.types.TypeHandlerRegistry;

import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultTableReader implements TableReader {
    private final boolean        caseInsensitive;
    private final DynamicContext dynamicContext;
    private final TypeHandler<?> defaultTypeHandler;

    public DefaultTableReader(boolean caseInsensitive, DynamicContext dynamicContext) {
        this.caseInsensitive = caseInsensitive;
        this.dynamicContext = dynamicContext;
        this.defaultTypeHandler = this.dynamicContext.defaultTypeHandler();
    }

    @Override
    public Map<String, Object> extractRow(List<String> columns, ResultSet rs, int rowNum) throws SQLException {
        List<TypeHandler<?>> handlers = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            handlers.add(this.getResultSetTypeHandler(rs, i + 1, null));
        }

        return this.extractRow(columns, handlers, rs, rowNum);
    }

    protected Map<String, Object> extractRow(List<String> columns, List<TypeHandler<?>> handlers, ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> target = (this.caseInsensitive) ? new LinkedCaseInsensitiveMap<>(columns.size()) : new LinkedHashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            String columnName = columns.get(i);
            TypeHandler<?> handler = handlers.get(i);
            if (handler == null) {
                handler = this.defaultTypeHandler;
            }

            Object result = handler.getResult(rs, i + 1);
            target.put(columnName, result);
        }
        return target;
    }

    /** 获取读取列用到的那个 TypeHandler */
    public TypeHandler<?> getResultSetTypeHandler(ResultSet rs, int columnIndex, Class<?> targetType) throws SQLException {
        int jdbcType = rs.getMetaData().getColumnType(columnIndex);
        String columnTypeName = rs.getMetaData().getColumnTypeName(columnIndex);
        String columnClassName = rs.getMetaData().getColumnClassName(columnIndex);

        if ("YEAR".equalsIgnoreCase(columnTypeName)) {
            // TODO with mysql `YEAR` type, columnType is DATE. but getDate() throw Long cast Date failed.
            jdbcType = JDBCType.INTEGER.getVendorTypeNumber();
        } else if (StringUtils.isNotBlank(columnClassName) && StringUtils.startsWithIgnoreCase(columnClassName, "oracle.")) {
            // TODO with oracle columnClassName is specifically customizes standard types, it specializes process.
            jdbcType = TypeHandlerRegistry.toSqlType(columnClassName);
            if (targetType != null) {
                return this.dynamicContext.findTypeHandler(targetType, jdbcType);
            } else {
                return this.dynamicContext.findTypeHandler(jdbcType);
            }
        }

        Class<?> columnTypeClass = targetType;
        if (columnTypeClass == null) {
            try {
                columnTypeClass = this.dynamicContext.loadClass(columnClassName);
            } catch (ClassNotFoundException e) {
                /**/
            }
        }
        TypeHandler<?> typeHandler = this.dynamicContext.findTypeHandler(columnTypeClass, jdbcType);
        if (typeHandler == null) {
            String message = "jdbcType=" + jdbcType + " ,columnTypeClass=" + columnTypeClass;
            throw new SQLException("no typeHandler is matched to any available " + message);
        }
        return typeHandler;
    }
}

