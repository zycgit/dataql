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
package net.hasor.dataql.sqlproc.types;
import net.hasor.dataql.sqlproc.types.handler.SqlXmlTypeHandler;
import net.hasor.dataql.sqlproc.utils.DsUtils;
import net.hasor.dataql.sqlproc.utils.TypeHandlerBridge;
import net.hasor.dbvisitor.jdbc.SqlParameterUtils;
import net.hasor.dbvisitor.jdbc.core.JdbcTemplate;
import org.junit.Test;

import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SqlXmlTypeTest {
    protected void preTable(JdbcTemplate jdbcTemplate) throws SQLException {
        try {
            jdbcTemplate.executeUpdate("drop table tb_oracle_types_onlyxml");
        } catch (Exception e) {
            /**/
        }
        jdbcTemplate.executeUpdate("create table tb_oracle_types_onlyxml (c_xml xmltype)");
    }

    protected void preProc(JdbcTemplate jdbcTemplate) throws SQLException {
        try {
            jdbcTemplate.executeUpdate("drop procedure proc_xmltype");
        } catch (Exception e) {
            /**/
        }
        jdbcTemplate.execute(""//
                + "create or replace procedure proc_xmltype(p_out out xmltype) as " //
                + "begin " //
                + "  SELECT (XMLTYPE('<xml>abc</xml>')) into p_out FROM DUAL; " //
                + "end;");
    }

    @Test
    public void testSqlXmlTypeHandler_1() throws SQLException {
        try (Connection c = DsUtils.oracleConn()) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(c);
            preTable(jdbcTemplate);

            jdbcTemplate.executeUpdate("insert into tb_oracle_types_onlyxml (c_xml) values ('<xml>abc</xml>')");
            List<String> dat = jdbcTemplate.queryForList("select c_xml from tb_oracle_types_onlyxml where c_xml is not null", (rs, rowNum) -> {
                return new SqlXmlTypeHandler().getResult(rs, 1);
            });
            assert dat.get(0).trim().equals("<xml>abc</xml>");
        }
    }

    @Test
    public void testSqlXmlTypeHandler_2() throws SQLException {
        try (Connection c = DsUtils.oracleConn()) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(c);
            preTable(jdbcTemplate);

            jdbcTemplate.executeUpdate("insert into tb_oracle_types_onlyxml (c_xml) values ('<xml>abc</xml>')");
            List<String> dat = jdbcTemplate.queryForList("select c_xml from tb_oracle_types_onlyxml where c_xml is not null", (rs, rowNum) -> {
                return new SqlXmlTypeHandler().getResult(rs, "c_xml");
            });
            assert dat.get(0).trim().equals("<xml>abc</xml>");
        }
    }

    @Test
    public void testSqlXmlTypeHandler_3() throws SQLException {
        try (Connection c = DsUtils.oracleConn()) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(c);

            List<String> dat = jdbcTemplate.queryForList("select ? from dual", ps -> {
                new SqlXmlTypeHandler().setParameter(ps, 1, "<xml>abc</xml>", JDBCType.SQLXML.getVendorTypeNumber());
            }, (rs, rowNum) -> {
                return new SqlXmlTypeHandler().getNullableResult(rs, 1);
            });
            assert dat.get(0).equals("<xml>abc</xml>");
        }
    }

    @Test
    public void testSqlXmlTypeHandler_4() throws SQLException {
        try (Connection c = DsUtils.oracleConn()) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(c);
            preProc(jdbcTemplate);

            Map<String, Object> objectMap = jdbcTemplate.call("{call proc_xmltype(?)}",//
                    Collections.singletonList(SqlParameterUtils.withOutputName("out", JDBCType.SQLXML.getVendorTypeNumber(), new TypeHandlerBridge<>(new SqlXmlTypeHandler()))));

            assert objectMap.size() == 2;
            assert objectMap.get("out") instanceof String;
            assert objectMap.get("out").equals("<xml>abc</xml>");
            assert objectMap.get("#update-count-1").equals(-1);// in oracle ,no more result is -1
        }
    }
}
