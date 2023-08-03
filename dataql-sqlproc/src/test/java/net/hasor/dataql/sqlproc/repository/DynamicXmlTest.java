/*
 * Copyright 2002-2005 the original author or authors.
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
package net.hasor.dataql.sqlproc.repository;
import net.hasor.cobble.ResourcesUtils;
import net.hasor.cobble.io.IOUtils;
import net.hasor.dataql.Hints;
import net.hasor.dataql.runtime.HintsSet;
import net.hasor.dataql.sqlproc.dialect.BoundSqlBuilder;
import net.hasor.dataql.sqlproc.dialect.SqlMode;
import net.hasor.dataql.sqlproc.repository.config.QueryProcSql;
import net.hasor.dataql.sqlproc.types.handler.BlobAsBytesTypeHandler;
import net.hasor.dataql.sqlproc.types.handler.StringTypeHandler;
import net.hasor.test.dataql.sqlproc.dto.TbUser;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DynamicXmlTest {
    private static final ProcSqlParser xmlParser = new ProcSqlParser();
    private static final Hints         hints     = new HintsSet();

    static {
        hints.setHint("hasXml", true);
    }

    private String loadString(String queryConfig) throws IOException {
        return IOUtils.readToString(ResourcesUtils.getResourceAsStream(queryConfig), "UTF-8");
    }

    @Test
    public void ifTest_01() throws Throwable {
        String queryConfig = loadString("/dataql_dynamic/if_01.xml");
        QueryProcSql parseXml = xmlParser.parseDynamicSql(queryConfig, hints);
        //
        String querySql1 = loadString("/dataql_dynamic/if_01.xml.sql_1");
        Map<String, Object> data1 = new HashMap<>();
        data1.put("ownerID", "123");
        data1.put("ownerType", "SYSTEM");

        BoundSqlBuilder sqlBuilder = new BoundSqlBuilder();
        parseXml.buildQuery(hints, data1, new TextBuilderContext(), sqlBuilder);
        assert sqlBuilder.getSqlString().trim().equals(querySql1.trim());
        assert sqlBuilder.getArgs()[0].getValue().equals("123");
        assert sqlBuilder.getArgs()[1].getValue().equals("SYSTEM");
        //
        String querySql2 = loadString("/dataql_dynamic/if_01.xml.sql_2");
        Map<String, Object> data2 = new HashMap<>();
        data1.put("ownerID", "123");
        data1.put("ownerType", null);
        sqlBuilder = new BoundSqlBuilder();
        parseXml.buildQuery(hints, data2, new TextBuilderContext(), sqlBuilder);
        assert sqlBuilder.getSqlString().trim().equals(querySql2.trim());
        assert sqlBuilder.getArgs().length == 0;
    }

    @Test
    public void includeTest_01() throws Throwable {
        String queryConfig = loadString("/dataql_dynamic/include_01.xml");
        QueryProcSql parseXml = xmlParser.parseDynamicSql(queryConfig, hints);
        //
        String querySql1 = loadString("/dataql_dynamic/include_01.xml.sql_1");
        Map<String, Object> data1 = new HashMap<>();
        data1.put("eventType", "123");

        BoundSqlBuilder sqlBuilder = new BoundSqlBuilder();
        parseXml.buildQuery(hints, data1, new TextBuilderContext(), sqlBuilder);
        assert sqlBuilder.getSqlString().trim().equals(querySql1.trim());
        assert sqlBuilder.getArgs()[0].getValue().equals("123");
    }

    @Test
    public void foreachTest_01() throws Throwable {
        String queryConfig = loadString("/dataql_dynamic/foreach_03.xml");
        QueryProcSql parseXml = xmlParser.parseDynamicSql(queryConfig, hints);
        //
        String querySql1 = loadString("/dataql_dynamic/foreach_03.xml.sql_1");
        Map<String, Object> data1 = new HashMap<>();
        data1.put("eventTypes", Arrays.asList("a", "b", "c", "d", "e"));

        BoundSqlBuilder sqlBuilder = new BoundSqlBuilder();
        parseXml.buildQuery(hints, data1, new TextBuilderContext(), sqlBuilder);
        assert sqlBuilder.getSqlString().trim().equals(querySql1.trim());
        assert sqlBuilder.getArgs()[0].getValue().equals("a");
        assert sqlBuilder.getArgs()[1].getValue().equals("b");
        assert sqlBuilder.getArgs()[2].getValue().equals("c");
        assert sqlBuilder.getArgs()[3].getValue().equals("d");
        assert sqlBuilder.getArgs()[4].getValue().equals("e");
    }

    @Test
    public void setTest_01() throws Throwable {
        String queryConfig = loadString("/dataql_dynamic/set_04.xml");
        QueryProcSql parseXml = xmlParser.parseDynamicSql(queryConfig, hints);
        //
        String querySql1 = loadString("/dataql_dynamic/set_04.xml.sql_1");
        Map<String, Object> data1 = new HashMap<>();
        data1.put("phone", "1234");
        data1.put("email", "zyc@zyc");
        data1.put("expression", "ddd");
        data1.put("id", "~~~");
        data1.put("uid", "1111");

        BoundSqlBuilder sqlBuilder = new BoundSqlBuilder();
        parseXml.buildQuery(hints, data1, new TextBuilderContext(), sqlBuilder);
        assert sqlBuilder.getSqlString().trim().equals(querySql1.trim());
        assert sqlBuilder.getArgs()[0].getValue().equals("1234");
        assert sqlBuilder.getArgs()[1].getValue().equals("zyc@zyc");
        assert sqlBuilder.getArgs()[2].getValue().equals("ddd");
        assert sqlBuilder.getArgs()[3].getValue().equals("~~~");
        assert sqlBuilder.getArgs()[4].getValue().equals("1111");
        //
        String querySql2 = loadString("/dataql_dynamic/set_04.xml.sql_2");
        Map<String, Object> data2 = new HashMap<>();
        data2.put("id", "~~~");
        data2.put("uid", "1111");

        sqlBuilder = new BoundSqlBuilder();
        parseXml.buildQuery(hints, data2, new TextBuilderContext(), sqlBuilder);
        assert sqlBuilder.getSqlString().trim().equals(querySql2.trim());
        assert sqlBuilder.getArgs()[0].getValue().equals("~~~");
        assert sqlBuilder.getArgs()[1].getValue().equals("1111");
    }

    @Test
    public void bindTest_01() throws Throwable {
        String queryConfig = loadString("/dataql_dynamic/bind_01.xml");
        QueryProcSql parseXml = xmlParser.parseDynamicSql(queryConfig, hints);
        //
        String querySql1 = loadString("/dataql_dynamic/bind_01.xml.sql_1");
        Map<String, Object> data1 = new HashMap<>();
        data1.put("sellerId", "123");

        BoundSqlBuilder sqlBuilder = new BoundSqlBuilder();
        parseXml.buildQuery(hints, data1, new TextBuilderContext(), sqlBuilder);
        assert sqlBuilder.getSqlString().trim().equals(querySql1.trim());
        assert sqlBuilder.getArgs()[0].getValue().equals("123abc");
    }

    @Test
    public void whereTest_01() throws Throwable {
        String queryConfig = loadString("/dataql_dynamic/where_01.xml");
        QueryProcSql parseXml = xmlParser.parseDynamicSql(queryConfig, hints);
        //
        String querySql1 = loadString("/dataql_dynamic/where_01.xml.sql_1");
        Map<String, Object> data1 = new HashMap<>();
        data1.put("sellerId", "123");

        BoundSqlBuilder sqlBuilder = new BoundSqlBuilder();
        parseXml.buildQuery(hints, data1, new TextBuilderContext(), sqlBuilder);
        assert sqlBuilder.getSqlString().trim().equals(querySql1.trim());
        //
        String querySql2 = loadString("/dataql_dynamic/where_01.xml.sql_2");
        Map<String, Object> data2 = new HashMap<>();
        data2.put("state", "123");
        data2.put("title", "aaa");

        sqlBuilder = new BoundSqlBuilder();
        parseXml.buildQuery(hints, data2, new TextBuilderContext(), sqlBuilder);
        assert sqlBuilder.getSqlString().trim().equals(querySql2.trim());
        assert sqlBuilder.getArgs()[0].getValue().equals("123");
        assert sqlBuilder.getArgs()[1].getValue().equals("aaa");
    }

    @Test
    public void chooseTest_01() throws Throwable {
        String queryConfig = loadString("/dataql_dynamic/choose_01.xml");
        QueryProcSql parseXml = xmlParser.parseDynamicSql(queryConfig, hints);
        //
        String querySql1 = loadString("/dataql_dynamic/choose_01.xml.sql_1");
        Map<String, Object> data1 = new HashMap<>();
        data1.put("title", "123");
        data1.put("content", "aaa");

        BoundSqlBuilder sqlBuilder = new BoundSqlBuilder();
        parseXml.buildQuery(hints, data1, new TextBuilderContext(), sqlBuilder);
        assert sqlBuilder.getSqlString().trim().equals(querySql1.trim());
        assert sqlBuilder.getArgs()[0].getValue().equals("123");
    }

    @Test
    public void chooseTest_02() throws Throwable {
        String queryConfig = loadString("/dataql_dynamic/choose_01.xml");
        QueryProcSql parseXml = xmlParser.parseDynamicSql(queryConfig, hints);
        //
        String querySql1 = loadString("/dataql_dynamic/choose_01.xml.sql_2");
        Map<String, Object> data1 = new HashMap<>();

        BoundSqlBuilder sqlBuilder = new BoundSqlBuilder();
        parseXml.buildQuery(hints, data1, new TextBuilderContext(), sqlBuilder);
        assert sqlBuilder.getSqlString().trim().equals(querySql1.trim());
    }

    @Test
    public void tokenTest_01() throws Throwable {
        String queryConfig = loadString("/dataql_dynamic/token_01.xml");
        QueryProcSql parseXml = xmlParser.parseDynamicSql(queryConfig, hints);
        //
        String querySql1 = loadString("/dataql_dynamic/token_01.xml.sql_1");
        Map<String, Object> data1 = new HashMap<>();
        data1.put("abc", "123");
        data1.put("futures", "11");
        data1.put("orderBy", "user_name asc");
        data1.put("info", new HashMap<String, Object>() {{
            put("status", true);
        }});

        BoundSqlBuilder sqlBuilder = new BoundSqlBuilder();
        parseXml.buildQuery(hints, data1, new TextBuilderContext(), sqlBuilder);
        assert sqlBuilder.getSqlString().trim().equals(querySql1.trim());
        assert sqlBuilder.getArgs()[0].getValue().equals("123");
        assert sqlBuilder.getArgs()[1].getValue() == null;// mode = out not eval value.
        assert sqlBuilder.getArgs()[0].getJavaType() == String.class;
        assert sqlBuilder.getArgs()[1].getJavaType() == TbUser.class;
        assert sqlBuilder.getArgs()[0].getTypeHandler() instanceof StringTypeHandler;
        assert sqlBuilder.getArgs()[1].getTypeHandler() instanceof BlobAsBytesTypeHandler;
        assert sqlBuilder.getArgs()[0].getSqlMode() == SqlMode.In;
        assert sqlBuilder.getArgs()[1].getSqlMode() == SqlMode.Out;
    }

    @Test
    public void selectKeyTest_01() throws Throwable {
        String queryConfig = loadString("/dataql_dynamic/selectkey_01.xml");
        QueryProcSql parseXml = xmlParser.parseDynamicSql(queryConfig, hints);

        String querySql1 = loadString("/dataql_dynamic/selectkey_01.xml.sql_1");
        Map<String, Object> data1 = new HashMap<>();
        data1.put("uid", "zyc_uid");
        data1.put("name", "zyc_name");

        BoundSqlBuilder sqlBuilder = new BoundSqlBuilder();
        parseXml.buildQuery(hints, data1, new TextBuilderContext(), sqlBuilder);

        assert sqlBuilder.getSqlString().trim().equals(querySql1.trim());

        assert parseXml.getSelectKey() != null;

        String querySql2 = loadString("/dataql_dynamic/selectkey_01.xml.sql_2");

        sqlBuilder = new BoundSqlBuilder();
        parseXml.getSelectKey().buildQuery(hints, data1, new TextBuilderContext(), sqlBuilder);
        assert sqlBuilder.getSqlString().trim().equals(querySql2.trim());
    }
}
