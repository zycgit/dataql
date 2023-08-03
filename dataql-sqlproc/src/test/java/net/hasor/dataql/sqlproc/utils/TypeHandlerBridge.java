package net.hasor.dataql.sqlproc.utils;
import net.hasor.dbvisitor.types.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TypeHandlerBridge<T> implements TypeHandler<T> {
    private final net.hasor.dataql.sqlproc.types.TypeHandler<T> typeHandler;

    public TypeHandlerBridge(net.hasor.dataql.sqlproc.types.TypeHandler<T> typeHandler) {
        this.typeHandler = typeHandler;
    }

    @Override
    public void setParameter(PreparedStatement ps, int i, T parameter, Integer jdbcType) throws SQLException {
        this.typeHandler.setParameter(ps, i, parameter, jdbcType);
    }

    @Override
    public T getResult(ResultSet rs, String columnName) throws SQLException {
        return this.typeHandler.getResult(rs, columnName);
    }

    @Override
    public T getResult(ResultSet rs, int columnIndex) throws SQLException {
        return this.typeHandler.getResult(rs, columnIndex);
    }

    @Override
    public T getResult(CallableStatement cs, int columnIndex) throws SQLException {
        return this.typeHandler.getResult(cs, columnIndex);
    }
}
