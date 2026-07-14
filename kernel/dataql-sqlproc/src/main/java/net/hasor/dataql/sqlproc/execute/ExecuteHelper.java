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
package net.hasor.dataql.sqlproc.execute;
import java.io.StringReader;
import java.util.List;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.io.IOUtils;
import net.hasor.dataql.sqlproc.dialect.BoundSql;
import net.hasor.dataql.sqlproc.dialect.Page;
import net.hasor.dataql.sqlproc.types.SqlArg;

class ExecuteHelper {
    public static boolean usingPage(Page pageInfo) {
        return pageInfo != null && pageInfo.getPageSize() > 0;
    }

    public static StringBuilder fmtBoundSql(BoundSql sqlBuilder) {
        StringBuilder builder = new StringBuilder("querySQL: ");

        try {
            if (sqlBuilder == null) {
                builder.append("(Empty)");
                return builder;
            } else {
                List<String> lines = IOUtils.readLines(new StringReader(sqlBuilder.getSqlString()));
                for (String line : lines) {
                    if (StringUtils.isNotBlank(line)) {
                        builder.append(line.trim()).append(" ");
                    }
                }
            }
        } catch (Exception e) {
            builder.append(sqlBuilder.getSqlString().replace("\n", ""));
        }

        builder.append(", parameter: [");
        int i = 0;
        for (Object arg : sqlBuilder.getArgs()) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(fmtValue(arg));
            i++;
        }
        builder.append("] ");

        return builder;
    }

    private static String fmtValue(Object value) {
        Object object = value instanceof SqlArg ? ((SqlArg) value).getValue() : value;
        if (object == null) {
            return "null";
        } else if (object instanceof String) {
            if (((String) object).length() > 2048) {
                return "'" + ((String) object).substring(0, 2048) + "...'";
            } else {
                return "'" + ((String) object).replace("'", "\\'") + "'";
            }
        } else if (object instanceof Page) {
            return "page[pageSize=" + ((Page) object).getPageSize()//
                    + ", currentPage=" + ((Page) object).getCurrentPage()//
                    + ", pageNumberOffset=" + ((Page) object).getPageNumberOffset() + "]";
        }
        return object.toString();
    }
}
