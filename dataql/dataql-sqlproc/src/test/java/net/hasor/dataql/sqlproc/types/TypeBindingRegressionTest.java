/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.types;

import java.io.StringReader;
import java.io.ByteArrayInputStream;
import java.sql.*;
import net.hasor.dataql.domain.BinaryModel;
import net.hasor.dataql.sqlproc.types.time.OffsetDateTimeTypeHandler;
import net.hasor.dataql.sqlproc.types.time.SqlTimestampTypeHandler;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Date;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.rule.TestQueryContext;
import net.hasor.dataql.sqlproc.dynamic.segment.DynamicParsed;
import net.hasor.dataql.sqlproc.execute.MapResultExtractor;
import net.hasor.dataql.sqlproc.ColumnCaseType;
import net.hasor.dataql.sqlproc.types.array.ArrayTypeHandler;
import net.hasor.dataql.sqlproc.types.json.JsonTypeHandler;
import net.hasor.dataql.sqlproc.types.string.StringTypeHandler;
import net.hasor.dataql.sqlproc.types.vector.PgVectorTypeHandler;
import net.hasor.dataql.sqlproc.types.vector.ChVectorTypeHandler;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TypeBindingRegressionTest {
    @Test
    public void handlerContractAcceptsObjectsAndPreservesTypedNullBinding() throws Exception {
        TypeHandler handler = new StringTypeHandler();
        PreparedStatement statement = mock(PreparedStatement.class);
        handler.setParameter(statement, 1, "dataql", null);
        handler.setParameter(statement, 2, null, null);
        verify(statement).setString(1, "dataql");
        verify(statement).setNull(2, Types.VARCHAR);
        assertThrows(SQLException.class, () -> handler.setParameter(statement, 3, List.of("invalid"), null));
        ResultSet result = mock(ResultSet.class);
        when(result.getString(1)).thenReturn("dataql");
        assertEquals("dataql", handler.getResult(result, 1));
    }

    @Test
    public void vectorHandlersRejectNonNumericObjectsBeforeBinding() {
        PreparedStatement statement = mock(PreparedStatement.class);
        for (TypeHandler handler : List.of(new PgVectorTypeHandler(), new ChVectorTypeHandler())) {
            assertThrows(SQLException.class, () -> handler.setParameter(statement, 1, "invalid", null));
            assertThrows(SQLException.class, () -> handler.setParameter(statement, 1, List.of(1, "invalid"), null));
            assertThrows(SQLException.class, () -> handler.setParameter(statement, 1, List.of(Double.NaN), null));
        }
        verifyNoInteractions(statement);
    }

    @Test
    public void postgresVectorsPreserveValuesEmptyVectorsAndNulls() throws Exception {
        PgVectorTypeHandler handler = new PgVectorTypeHandler();
        ResultSet result = mock(ResultSet.class);
        when(result.getString(1)).thenReturn("[1.5, -2.25]", "[ ]", null);
        assertEquals(List.of(1.5f, -2.25f), handler.getResult(result, 1));
        assertEquals(List.of(), handler.getResult(result, 1));
        assertNull(handler.getResult(result, 1));
        PreparedStatement statement = mock(PreparedStatement.class);
        handler.setParameter(statement, 1, List.of(1.5f, -2.25f), Types.OTHER);
        verify(statement).setObject(1, "[1.5,-2.25]", Types.OTHER);
    }

    @Test
    public void typedNullUsesItsExplicitJdbcType() throws Exception {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        PreparedStatement statement = mock(PreparedStatement.class);
        registry.setParameterValue(statement, 1, SqlArg.valueOf(null, Types.VARCHAR));
        new StringTypeHandler().setParameter(statement, 2, null, null);
        verify(statement).setNull(1, Types.VARCHAR);
        verify(statement).setNull(2, Types.VARCHAR);
    }

    @Test
    public void explicitJdbcTypeUsesTheRuntimeJavaType() throws Exception {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        TypeHandler handler = mock(TypeHandler.class);
        registry.register(Types.OTHER, Integer.class, handler);
        SqlArg arg = SqlArg.valueOf(7, Types.OTHER);
        PreparedStatement statement = mock(PreparedStatement.class);
        registry.setParameterValue(statement, 1, arg);
        verify(handler).setParameter(statement, 1, 7, Types.OTHER);
        verifyNoInteractions(statement);
    }

    @Test
    public void runtimeFallbackDoesNotHideLaterRegistrations() {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        assertSame(registry.getDefaultTypeHandler(), registry.getTypeHandler(StringBuilder.class));
        TypeHandler handler = mock(TypeHandler.class);
        registry.register(CharSequence.class, handler);
        assertSame(handler, registry.getTypeHandler(StringBuilder.class));
    }

    @Test
    public void primitiveArraysAndDoublePrecisionRoundTripThroughH2() throws Exception {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:array_regression")) {
            try (PreparedStatement statement = connection.prepareStatement("select cast(? as integer array), cast(? as double precision array)")) {
                registry.setParameterValue(statement, 1, new int[] {1,2,3});
                registry.setParameterValue(statement, 2, new double[] {1.123456789123,2.987654321987});
                try (ResultSet result = statement.executeQuery()) {
                    assertTrue(result.next());
                    ArrayTypeHandler arrays = new ArrayTypeHandler();
                    assertArrayEquals(new Integer[] {1,2,3}, (Integer[]) arrays.getResult(result,1));
                    assertArrayEquals(new Double[] {1.123456789123,2.987654321987}, (Double[]) arrays.getResult(result,2));
                }
            }
        }
    }

    @Test
    public void emptyAndNullArrayElementsRetainTheirSqlType() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:empty_arrays");
                PreparedStatement statement = connection.prepareStatement("select cast(array[] as double array), cast(array[null,null] as integer array)")) {
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                ArrayTypeHandler handler = new ArrayTypeHandler();
                assertArrayEquals(new Object[0], (Object[]) handler.getResult(result,1));
                assertArrayEquals(new Object[] {null,null}, (Object[]) handler.getResult(result,2));
            }
        }
    }

    @Test
    public void knownArrayBaseTypesProduceTypedEmptyAndNullArrays() throws Exception {
        Array array = mock(Array.class);
        ResultSet result = mock(ResultSet.class);
        when(result.getArray(1)).thenReturn(array);
        when(array.getBaseTypeName()).thenReturn("DOUBLE");
        when(array.getArray()).thenReturn((Object) new Object[0]).thenReturn((Object) new Object[] {null, null});
        ArrayTypeHandler handler = new ArrayTypeHandler();
        assertArrayEquals(new Double[0], (Double[]) handler.getResult(result, 1));
        assertArrayEquals(new Double[] {null, null}, (Double[]) handler.getResult(result, 1));
        verify(array, times(2)).free();
    }

    @Test
    public void jdbcArrayOwnershipAndFailureCleanupArePreserved() throws Exception {
        ArrayTypeHandler handler = new ArrayTypeHandler();
        PreparedStatement statement = mock(PreparedStatement.class);
        Array ownedByCaller = mock(Array.class);
        handler.setParameter(statement,1,ownedByCaller,Types.ARRAY);
        verify(ownedByCaller,never()).free();
        ResultSet result = mock(ResultSet.class);
        Array readArray = mock(Array.class);
        when(result.getArray(1)).thenReturn(readArray);
        when(readArray.getArray()).thenThrow(new SQLException("read failure"));
        assertThrows(SQLException.class, () -> handler.getResult(result,1));
        verify(readArray).free();
    }

    @Test
    public void timestampReadsKeepTimeOfDay() throws Exception {
        Instant instant = Instant.parse("2026-10-05T15:24:36.123Z");
        ResultSet result = mock(ResultSet.class);
        when(result.getTimestamp(1)).thenReturn(Timestamp.from(instant));
        Date date = (Date) new SqlTimestampTypeHandler().getResult(result,1);
        assertEquals(instant.toEpochMilli(), date.getTime());
        verify(result,never()).getDate(anyInt());
    }

    @Test
    public void missingColumnClassFallsBackToJdbcType() throws Exception {
        ResultSet result = mock(ResultSet.class);
        ResultSetMetaData metadata = mock(ResultSetMetaData.class);
        when(result.getMetaData()).thenReturn(metadata);
        when(metadata.getColumnCount()).thenReturn(1);
        when(metadata.getColumnLabel(1)).thenReturn("name");
        when(metadata.getColumnType(1)).thenReturn(Types.VARCHAR);
        when(result.getString(1)).thenReturn("hello");
        when(result.next()).thenReturn(true,false);
        MapResultExtractor extractor = new MapResultExtractor(new TypeHandlerRegistry());
        assertEquals("hello", extractor.extractData(ColumnCaseType.ColumnCaseDefault,result).get(0).get("name"));
    }

    @Test
    public void argRuleCarriesJdbcTypeToCallableOutParameters() throws Exception {
        SqlBuilder sql = DynamicParsed.getParsedSql("#{mode=OUT,jdbcType=VARCHAR,name=result}")
                .buildQuery(Map.of(), new TestQueryContext());
        SqlArg arg = (SqlArg) sql.getArgs()[0];
        assertEquals(Integer.valueOf(Types.VARCHAR),arg.getJdbcType());
        CallableStatement callable = mock(CallableStatement.class);
        when(callable.getString(1)).thenReturn("done");
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        registry.setParameterValue(callable,1,arg);
        verify(callable).registerOutParameter(1,Types.VARCHAR);
        assertEquals("done",registry.getParameterValue(callable,1,arg));
    }

    @Test
    public void jsonColumnsUseTheSharedJsonTool() throws Exception {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        JsonTypeHandler mapHandler = (JsonTypeHandler) registry.createTypeHandler(JsonTypeHandler.class, Map.class);
        JsonTypeHandler arrayHandler = (JsonTypeHandler) registry.createTypeHandler(JsonTypeHandler.class, Integer[].class);
        assertNotSame(mapHandler,arrayHandler);
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:json_types");
                PreparedStatement statement = connection.prepareStatement("select cast(? as varchar)")) {
            mapHandler.setParameter(statement,1,Map.of("id",7),Types.VARCHAR);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(Map.of("id",7),mapHandler.getResult(result,1));
            }
        }
    }

    @Test
    public void opaqueColumnClassesStillUseTheJdbcBinaryHandler() throws Exception {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        ResultSet result = mock(ResultSet.class);
        ResultSetMetaData metadata = mock(ResultSetMetaData.class);
        Blob blob = mock(Blob.class);
        when(result.getMetaData()).thenReturn(metadata);
        when(metadata.getColumnType(1)).thenReturn(Types.BLOB);
        when(metadata.getColumnClassName(1)).thenReturn(Object.class.getName());
        when(result.getBlob(1)).thenReturn(blob);
        when(blob.length()).thenReturn(2L);
        when(blob.getBytes(1, 2)).thenReturn(new byte[] {1, 2});
        BinaryModel value = (BinaryModel) registry.getResultSetTypeHandler(result, 1, null).getResult(result, 1);
        try (var input = value.openStream()) {
            assertArrayEquals(new byte[] {1, 2}, input.readAllBytes());
        }
        verify(blob).free();
        verify(result, never()).getObject(1);
    }

    @Test
    public void charOutParametersKeepTheWholeString() throws Exception {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        CallableStatement statement = mock(CallableStatement.class);
        when(statement.getString(1)).thenReturn("ABCD");
        when(statement.getNString(2)).thenReturn("中文测试");
        assertEquals("ABCD", registry.getParameterValue(statement, 1, SqlArg.valueOf(null, Types.CHAR)));
        assertEquals("中文测试", registry.getParameterValue(statement, 2, SqlArg.valueOf(null, Types.NCHAR)));
    }

    @Test
    public void legacyDriversCanReturnOffsetTextAndKeepReadFailures() throws Exception {
        ResultSet result = mock(ResultSet.class);
        when(result.getObject(1, OffsetDateTime.class)).thenThrow(new SQLFeatureNotSupportedException("typed read"));
        when(result.getString(1)).thenReturn("2026-10-05 12:34:56+0800", "invalid");
        OffsetDateTimeTypeHandler handler = new OffsetDateTimeTypeHandler();
        assertEquals("2026-10-05T12:34:56+08:00", handler.getResult(result, 1));
        SQLException failure = assertThrows(SQLException.class, () -> handler.getResult(result, 1));
        assertEquals(1, failure.getCause().getSuppressed().length);
    }
}
