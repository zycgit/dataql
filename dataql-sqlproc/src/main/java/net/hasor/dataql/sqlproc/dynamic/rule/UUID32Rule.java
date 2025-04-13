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
package net.hasor.dataql.sqlproc.dynamic.rule;
import net.hasor.dataql.sqlproc.dynamic.*;
import net.hasor.dataql.sqlproc.types.TypeHandler;
import net.hasor.dataql.sqlproc.types.TypeHandlerRegistry;
import net.hasor.dataql.sqlproc.types.string.StringTypeHandler;

import java.sql.Types;
import java.util.UUID;

/**
 * 产生一个 32 字符长度的 `UUID`，并加入到 SQL 参数中
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-10-31
 */
public class UUID32Rule implements SqlRule {
    private static final TypeHandler typeHandler = TypeHandlerRegistry.DEFAULT.getHandlerByHandlerType(StringTypeHandler.class);
    public static final  UUID32Rule  INSTANCE    = new UUID32Rule();

    @Override
    public boolean test(SqlArgSource data, QueryContext context, String activeExpr) {
        return true;
    }

    @Override
    public void executeRule(SqlArgSource data, QueryContext context, SqlBuilder sqlBuilder, String activeExpr, String ruleValue) {
        String uuidValue = UUID.randomUUID().toString().replace("-", "");
        SqlArg sqlArg = new SqlArg(ruleValue, uuidValue, SqlMode.In, Types.VARCHAR, typeHandler);
        sqlBuilder.appendSql("?", sqlArg);
    }

    @Override
    public String toString() {
        return "uuid32 [" + this.hashCode() + "]";
    }
}
