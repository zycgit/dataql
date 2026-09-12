/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import net.hasor.dataql.sqlproc.ColumnCaseType;
import net.hasor.dataql.sqlproc.types.TypeHandlerRegistry;
import net.hasor.dbvisitor.jdbc.core.JdbcTemplate;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MapResultExtractorTest extends AbstractSqlProcTest {

    private Connection newExtractConnection() throws SQLException {
        Connection conn = newH2Connection();
        JdbcTemplate tpl = new JdbcTemplate(conn);
        tpl.execute("create table test_tbl (id bigint auto_increment, first_name varchar(50), last_name varchar(50), age int)");
        tpl.execute("insert into test_tbl (first_name, last_name, age) values ('John', 'Doe', 30)");
        tpl.execute("insert into test_tbl (first_name, last_name, age) values ('Jane', 'Smith', 25)");
        return conn;
    }

    @Test
    public void extractDefaultCase() throws SQLException {
        try (Connection conn = newExtractConnection()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM test_tbl");
            MapResultExtractor extractor = new MapResultExtractor(TypeHandlerRegistry.DEFAULT);
            List<Map<String, Object>> result = extractor.extractData(ColumnCaseType.ColumnCaseDefault, rs);
            assertEquals(2, result.size());
            assertEquals("John", result.get(0).get("FIRST_NAME"));
            assertEquals(30, ((Number) result.get(0).get("AGE")).intValue());
        }
    }

    @Test
    public void extractLowerCase() throws SQLException {
        try (Connection conn = newExtractConnection()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT first_name, last_name FROM test_tbl");
            MapResultExtractor extractor = new MapResultExtractor(TypeHandlerRegistry.DEFAULT);
            List<Map<String, Object>> result = extractor.extractData(ColumnCaseType.ColumnCaseLower, rs);
            assertEquals(2, result.size());
            assertTrue(result.get(0).containsKey("first_name"));
        }
    }

    @Test
    public void extractUpperCase() throws SQLException {
        try (Connection conn = newExtractConnection()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT first_name FROM test_tbl");
            MapResultExtractor extractor = new MapResultExtractor(TypeHandlerRegistry.DEFAULT);
            List<Map<String, Object>> result = extractor.extractData(ColumnCaseType.ColumnCaseUpper, rs);
            assertTrue(result.get(0).containsKey("FIRST_NAME"));
            assertEquals("John", result.get(0).get("FIRST_NAME"));
        }
    }

    @Test
    public void extractHumpCase() throws SQLException {
        try (Connection conn = newExtractConnection()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT first_name, last_name FROM test_tbl");
            MapResultExtractor extractor = new MapResultExtractor(TypeHandlerRegistry.DEFAULT);
            List<Map<String, Object>> result = extractor.extractData(ColumnCaseType.ColumnCaseHump, rs);
            assertTrue(result.get(0).containsKey("firstName"));
        }
    }

    @Test
    public void emptyResultSet() throws SQLException {
        try (Connection conn = newExtractConnection()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM test_tbl WHERE id = 999");
            MapResultExtractor extractor = new MapResultExtractor(TypeHandlerRegistry.DEFAULT);
            List<Map<String, Object>> result = extractor.extractData(ColumnCaseType.ColumnCaseDefault, rs);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void duplicateColumns() throws SQLException {
        try (Connection conn = newExtractConnection()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT id, id, first_name FROM test_tbl");
            MapResultExtractor extractor = new MapResultExtractor(TypeHandlerRegistry.DEFAULT);
            List<Map<String, Object>> result = extractor.extractData(ColumnCaseType.ColumnCaseDefault, rs);
            assertEquals(2, result.size());
            assertEquals(2, result.get(0).size());
        }
    }
}
