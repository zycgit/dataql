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
import net.hasor.dataql.sqlproc.repository.config.QueryProcSql;
import net.hasor.test.dataql.sqlproc.dto.CharacterSensitiveEnum;
import net.hasor.test.dataql.sqlproc.dto.LicenseOfCodeEnum;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

public class CaseXmlTest {
    private static final ProcSqlParser xmlParser = new ProcSqlParser();
    private static final Hints         hints     = new HintsSet();

    static {
        hints.setHint("hasXml", true);
    }

    private String loadString(String queryConfig) throws IOException {
        return IOUtils.readToString(ResourcesUtils.getResourceAsStream(queryConfig), "UTF-8");
    }

    @Test
    public void caseTest_01() throws Throwable {
        String queryConfig = loadString("/dataql_dynamic/testcase/case_01.xml");
        QueryProcSql parseXml = xmlParser.parseDynamicSql(queryConfig, hints);
        //
        String querySql1 = loadString("/dataql_dynamic/testcase/case_01.xml.sql_1");
        Map<String, Object> data1 = new HashMap<>();
        data1.put("startId", "33322");
        data1.put("label", new ArrayList<>(Arrays.asList(LicenseOfCodeEnum.Private, LicenseOfCodeEnum.GPLv3)));
        data1.put("state", new ArrayList<>(Collections.singletonList(CharacterSensitiveEnum.A)));
        data1.put("consoleJobId", "123");

        BoundSqlBuilder sqlBuilder = new BoundSqlBuilder();
        parseXml.buildQuery(hints, data1, new TextBuilderContext(), sqlBuilder);
        assert sqlBuilder.getSqlString().trim().equals(querySql1.trim());
        assert sqlBuilder.getArgs()[0].getValue().equals("33322");
        assert sqlBuilder.getArgs()[1].getValue().equals(LicenseOfCodeEnum.Private);
        assert sqlBuilder.getArgs()[2].getValue().equals(LicenseOfCodeEnum.GPLv3);
        assert sqlBuilder.getArgs()[3].getValue().equals(CharacterSensitiveEnum.A);
        assert sqlBuilder.getArgs()[4].getValue().equals("123");
        assert sqlBuilder.getArgs()[5].getValue() == null;
    }
}
