package net.hasor.test.dataql.sqlproc.types;

import net.hasor.dataql.sqlproc.types.MappedCross;
import net.hasor.dataql.sqlproc.types.MappedJavaTypes;
import net.hasor.dataql.sqlproc.types.MappedJdbcTypes;
import net.hasor.dataql.sqlproc.types.handler.AbstractTypeHandler;

import java.sql.*;

@MappedCross(javaTypes = @MappedJavaTypes(String.class), jdbcType = @MappedJdbcTypes(Types.DATALINK))
@MappedCross(javaTypes = @MappedJavaTypes(StringBuffer.class), jdbcType = @MappedJdbcTypes(Types.VARCHAR))
public class MyTypeHandler extends AbstractTypeHandler<String> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, Integer jdbcType) throws SQLException {

    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return null;
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return null;
    }
}
