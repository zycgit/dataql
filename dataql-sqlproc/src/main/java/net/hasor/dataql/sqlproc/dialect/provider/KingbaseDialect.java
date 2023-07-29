/*
 * Copyright 2008-2009 the original author or authors.
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
package net.hasor.dataql.sqlproc.dialect.provider;

import net.hasor.dataql.sqlproc.dialect.BoundSql;
import net.hasor.dataql.sqlproc.dialect.BoundSqlObject;
import net.hasor.dataql.sqlproc.dialect.PageDialect;
import net.hasor.dataql.sqlproc.dialect.SqlArg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 人大金仓数据库 的 SqlDialect 实现
 * @version : 2020-10-31
 * @author 赵永春 (zyc@hasor.net)
 */
public class KingbaseDialect extends AbstractDialect implements PageDialect {

    @Override
    public BoundSql pageSql(BoundSql boundSql, long start, long limit) {
        StringBuilder sqlBuilder = new StringBuilder(boundSql.getSqlString());
        List<SqlArg> paramArrays = new ArrayList<>(Arrays.asList(boundSql.getArgs()));

        if (limit > 0) {
            sqlBuilder.append(" LIMIT ?");
            paramArrays.add(buildNumber(limit));
        }
        if (start > 0) {
            sqlBuilder.append(" OFFSET ?");
            paramArrays.add(buildNumber(start));
        }

        return new BoundSqlObject(sqlBuilder.toString(), paramArrays.toArray(new SqlArg[0]));
    }
}