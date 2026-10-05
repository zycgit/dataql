/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.types;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import net.hasor.dataql.domain.BinaryModel;
import net.hasor.dataql.domain.BinaryValue;
import net.hasor.dataql.domain.Udf;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.Query;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataql.sqlproc.types.json.JsonTypeHandler;
import net.hasor.dataql.sqlproc.types.vector.ChVectorTypeHandler;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Exercises type conversion through the real compiler, script model and JDBC execution. */
public class ScriptTypeBindingTest {
    private JdbcDataSource source;
    private Connection keeper;
    private HostConfiguration host;

    @Before
    public void initialize() throws Exception {
        this.source = new JdbcDataSource();
        this.source.setURL("jdbc:h2:mem:script_types_" + UUID.randomUUID());
        this.keeper = this.source.getConnection();
        this.host = new HostConfiguration();
        this.host.addAttachment(ConnectionProvider.class, (name, hints) -> this.source.getConnection());
    }

    @After
    public void close() throws Exception {
        if (this.keeper != null) {
            this.keeper.close();
        }
    }

    @Test
    public void scriptListsBindAsJdbcArraysIncludingMixedNumbersAndNulls() throws Exception {
        assertEquals(List.of(1, 2, 3), this.select("INTEGER ARRAY", "jdbcType=ARRAY", List.of(1, 2, 3)));
        assertEquals(List.of(1.25, 2.0), this.select("DOUBLE ARRAY", "jdbcType=ARRAY", List.of(1.25, 2)));
        assertEquals(Arrays.asList(1, null, 3), this.select("INTEGER ARRAY", "jdbcType=ARRAY", Arrays.asList(1, null, 3)));
        assertEquals(List.of(), this.select("INTEGER ARRAY", "jdbcType=ARRAY", List.of()));
        assertEquals(Arrays.asList(null, null), this.select("INTEGER ARRAY", "jdbcType=ARRAY", Arrays.asList(null, null)));
        assertEquals(List.of("a", "b"), this.select("VARCHAR ARRAY", "", List.of("a", "b")));
    }

    private Object select(String type, String options, Object input) throws Exception {
        String binding = "#{v" + (options.isEmpty() ? "" : "," + options) + "}";
        String script = "var q = @@selectSql(v)<% SELECT CAST(" + binding + " AS " + type + ") AS \"v\" %>; return q(${input});";
        Query query = new QueryManager(this.host).newBuilder().createQuery(script);
        return query.execute(symbol -> Collections.singletonMap("input", input)).getData().unwrap();
    }

    @Test
    public void incompatibleArrayElementsFailInsteadOfSilentlyStringifying() {
        assertThrows(Exception.class, () -> this.select("VARCHAR ARRAY", "jdbcType=ARRAY", List.of(1, "x")));
    }

    @Test
    public void characterColumnsKeepAllCharacters() throws Exception {
        assertEquals("ABCD", this.select("CHAR(4)", "jdbcType=CHAR", "ABCD"));
        assertEquals("中文测试", this.select("NCHAR(4)", "jdbcType=NCHAR", "中文测试"));
        assertNull(this.select("CHAR(4)", "jdbcType=CHAR", null));
    }

    @Test
    public void epochAndTextParametersBindToTemporalColumns() throws Exception {
        Timestamp instant = Timestamp.valueOf("2026-10-05 12:34:56.123");
        assertEquals(instant.getTime(), this.select("TIMESTAMP", "jdbcType=TIMESTAMP", instant.getTime()));
        assertEquals(instant.getTime(), this.select("TIMESTAMP", "jdbcType=TIMESTAMP", "2026-10-05T12:34:56.123"));
        assertEquals(Date.valueOf("2026-10-05").getTime(), this.select("DATE", "jdbcType=DATE", "2026-10-05"));
        assertEquals(Time.valueOf("12:34:56").getTime(), this.select("TIME", "jdbcType=TIME", "12:34:56"));
        assertNull(this.select("TIMESTAMP", "jdbcType=TIMESTAMP", null));
        assertThrows(Exception.class, () -> this.select("DATE", "jdbcType=DATE", "not-a-date"));
    }

    @Test
    public void offsetValuesReturnTextWithTheOriginalOffset() throws Exception {
        assertEquals("2026-10-05T12:34:56+08:00", this.select("TIMESTAMP WITH TIME ZONE", "jdbcType=TIMESTAMP_WITH_TIMEZONE", "2026-10-05T12:34:56+08:00"));
        assertEquals("12:34:56+08:00", this.select("TIME WITH TIME ZONE", "jdbcType=TIME_WITH_TIMEZONE", "12:34:56+08:00"));
        assertNull(this.select("TIME WITH TIME ZONE", "jdbcType=TIME_WITH_TIMEZONE", null));
        long epoch = OffsetDateTime.parse("2026-10-05T12:34:56Z").toInstant().toEpochMilli();
        assertEquals("2026-10-05T12:34:56Z", this.select("TIMESTAMP WITH TIME ZONE", "jdbcType=TIMESTAMP_WITH_TIMEZONE", epoch));
    }

    @Test
    public void temporalArraysAlsoReturnScriptValues() throws Exception {
        String script = "var q = @@selectSql()<% SELECT ARRAY[TIMESTAMP WITH TIME ZONE '2026-10-05 12:34:56+08:00', NULL] %>; return q();";
        Object result = new QueryManager(this.host).newBuilder().createQuery(script).execute().getData().unwrap();
        assertEquals(Arrays.asList("2026-10-05T12:34:56+08:00", null), result);
    }

    @Test
    public void jsonParametersAcceptScriptObjectsAndLists() throws Exception {
        String option = "typeHandler=" + JsonTypeHandler.class.getName();
        assertEquals("{\"name\":\"Alice\"}", this.select("VARCHAR", option, Map.of("name", "Alice")));
        assertEquals("[1,2,3]", this.select("VARCHAR", option, List.of(1, 2, 3)));
        assertNull(this.select("VARCHAR", option + ",jdbcType=VARCHAR", null));
    }

    @Test
    public void numbersPreserveBigIntegerAndDecimalPrecision() throws Exception {
        assertEquals(123L, this.select("BIGINT", "", new AtomicLong(123)));
        BigDecimal amount = new BigDecimal("12345678901234567890.123456789");
        assertEquals(amount, this.select("DECIMAL(30,9)", "", amount));
        BigInteger count = new BigInteger("1234567890123456789012345");
        assertEquals(new BigDecimal(count), this.select("DECIMAL(30,0)", "", count));
    }

    @Test
    public void jdbcBinaryResultsRemainReadableAfterTheQueryCloses() throws Exception {
        byte[] bytes = {0, 1, 2, -1};
        for (String type : List.of("VARBINARY", "BLOB")) {
            BinaryModel result = (BinaryModel) this.select(type, "jdbcType=" + type, new BinaryValue(bytes));
            try (InputStream input = result.openStream()) {
                assertArrayEquals(bytes, input.readAllBytes());
            }
        }
        assertNull(this.select("BLOB", "jdbcType=BLOB", null));
    }

    @Test
    public void binaryUdfValuesAreBoundAndTheirInputStreamIsClosed() throws Exception {
        ByteArrayInputStream input = spy(new ByteArrayInputStream(new byte[] {3, 4, 5}));
        Query query = new QueryManager(this.host).newBuilder()
                .addShareVar("file", () -> (Udf) (hints, params) -> new BinaryValue(input))
                .createQuery("var q = @@selectSql(v)<% SELECT CAST(#{v,jdbcType=BLOB} AS BLOB) AS \"v\" %>; return q(file());");
        BinaryModel result = (BinaryModel) query.execute().getData().unwrap();
        try (InputStream output = result.openStream()) {
            assertArrayEquals(new byte[] {3, 4, 5}, output.readAllBytes());
        }
        verify(input).close();
    }

    @Test
    public void clickHouseVectorConvertsScriptNumbersBeforeJdbcBinding() throws Exception {
        assertEquals(List.of(1.25f, 2.0f), this.select("REAL ARRAY", "typeHandler=" + ChVectorTypeHandler.class.getName(), List.of(1.25, 2)));
    }
}
