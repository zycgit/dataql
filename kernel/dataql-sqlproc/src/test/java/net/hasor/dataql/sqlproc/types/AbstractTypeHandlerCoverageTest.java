/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.types;

import java.lang.reflect.Proxy;
import java.sql.*;
import org.junit.Test;

public class AbstractTypeHandlerCoverageTest {

    private static class BrokenTypeHandler extends AbstractTypeHandler {
        @Override
        public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, Integer jdbcType) throws SQLException {
            throw new RuntimeException("Test Exception");
        }

        @Override
        public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
            throw new RuntimeException("Test Exception");
        }

        @Override
        public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            throw new RuntimeException("Test Exception");
        }

        @Override
        public Object getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
            throw new RuntimeException("Test Exception");
        }
    }

    private static class WorkingTypeHandler extends AbstractTypeHandler {
        @Override
        public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, Integer jdbcType) throws SQLException {
            // No-op
        }

        @Override
        public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
            return "result";
        }

        @Override
        public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            return "result";
        }

        @Override
        public Object getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
            return "result";
        }
    }

    @Test
    public void testExceptionWrapping() {
        BrokenTypeHandler broken = new BrokenTypeHandler();
        PreparedStatement ps = (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { PreparedStatement.class }, (proxy, method, args) -> null);
        ResultSet rs = (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { ResultSet.class }, (proxy, method, args) -> null);
        CallableStatement cs = (CallableStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { CallableStatement.class }, (proxy, method, args) -> null);

        // setParameter
        try {
            broken.setParameter(ps, 1, "val", null);
            assert false : "Should have thrown SQLException";
        } catch (SQLException e) {
            assert e.getMessage().contains("Error setting non null for parameter #1");
        }

        // getResult(rs, col)
        try {
            broken.getResult(rs, "col");
            assert false : "Should have thrown SQLException";
        } catch (SQLException e) {
            assert e.getMessage().contains("Error attempting to get column 'col' from result set");
        }

        // getResult(rs, idx)
        try {
            broken.getResult(rs, 1);
            assert false : "Should have thrown SQLException";
        } catch (SQLException e) {
            assert e.getMessage().contains("Error attempting to get column #1 from result set");
        }

        // getResult(cs, idx)
        try {
            broken.getResult(cs, 1);
            assert false : "Should have thrown SQLException";
        } catch (SQLException e) {
            assert e.getMessage().contains("Error attempting to get column #1 from callable statement");
        }
    }

    @Test
    public void testNullHandling() throws SQLException {
        WorkingTypeHandler working = new WorkingTypeHandler();
        final boolean[] setNullCalled = { false };

        PreparedStatement ps = (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { PreparedStatement.class }, (proxy, method, args) -> {
            if ("setNull".equals(method.getName())) {
                setNullCalled[0] = true;
                assert (Integer) args[0] == 1;
                assert (Integer) args[1] == Types.VARCHAR;
            }
            return null;
        });

        // Case 1: parameter null, jdbcType provided
        working.setParameter(ps, 1, null, Types.VARCHAR);
        assert setNullCalled[0];

        // Case 2: parameter null, jdbcType null
        try {
            working.setParameter(ps, 1, null, null);
            assert false : "Should have thrown SQLException for missing jdbcType";
        } catch (SQLException e) {
            assert e.getMessage().contains("JDBC requires that the JdbcType must be specified");
        }
    }

    @Test
    public void testNullHandlingSqlException() {
        WorkingTypeHandler working = new WorkingTypeHandler();

        PreparedStatement ps = (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { PreparedStatement.class }, (proxy, method, args) -> {
            if ("setNull".equals(method.getName())) {
                throw new SQLException("Simulated SQL Error");
            }
            return null;
        });

        try {
            working.setParameter(ps, 1, null, Types.VARCHAR);
            assert false;
        } catch (SQLException e) {
            assert e.getMessage().contains("Error setting null for parameter #1");
            assert e.getMessage().contains("Simulated SQL Error");
        }
    }
}
