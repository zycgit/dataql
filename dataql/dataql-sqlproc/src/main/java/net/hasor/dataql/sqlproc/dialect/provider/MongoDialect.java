/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dialect.provider;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.hasor.dataql.sqlproc.dialect.BoundSql;

/** Pagination for Mongo JDBC commands. */
public class MongoDialect extends AbstractDialect {
    private static final Pattern AGGREGATE = Pattern.compile("\\.aggregate\\s*\\(\\s*\\[");

    @Override
    public BoundSql countSql(BoundSql boundSql) {
        if (isAggregate(boundSql)) {
            return appendAggregateStage(boundSql, "{$facet: {rows: [{$count: 'value'}]}}, {$project: {_id: 0, count: {$ifNull: [{$arrayElemAt: ['$rows.value', 0]}, 0]}}}");
        }
        return new BoundSql.BoundSqlObj("/*+overwrite_find_as_count*/" + boundSql.getSqlString(), boundSql.getArgs());
    }

    @Override
    public BoundSql pageSql(BoundSql boundSql, long start, long limit) {
        if (isAggregate(boundSql)) {
            return appendAggregateStage(boundSql, (start > 0 ? "{$skip: " + start + "}, " : "") + "{$limit: " + limit + "}");
        }
        StringBuilder sb = new StringBuilder("/*+");

        if (start <= 0) {
            sb.append("overwrite_find_limit=" + limit);
        } else {
            sb.append("overwrite_find_skip=" + start + ",overwrite_find_limit=" + limit);
        }

        sb.append("*/");
        return new BoundSql.BoundSqlObj(sb + boundSql.getSqlString(), boundSql.getArgs());
    }

    private boolean isAggregate(BoundSql boundSql) {
        return this.pipelineStart(boundSql.getSqlString()) >= 0;
    }

    private int pipelineStart(String command) {
        Matcher matcher = AGGREGATE.matcher(command);
        char quote = 0;
        for (int i = 0; i < command.length(); i++) {
            char ch = command.charAt(i);
            if (quote != 0) {
                if (ch == '\\') {
                    i++;
                } else if (ch == quote) {
                    quote = 0;
                }
            } else if (ch == '\'' || ch == '"') {
                quote = ch;
            } else if (ch == '/' && i + 1 < command.length() && command.charAt(i + 1) == '*') {
                int end = command.indexOf("*/", i + 2);
                if (end < 0) {
                    return -1;
                }
                i = end + 1;
            } else if (ch == '.' && matcher.region(i, command.length()).lookingAt()) {
                return matcher.end() - 1;
            }
        }
        return -1;
    }

    private BoundSql appendAggregateStage(BoundSql boundSql, String stage) {
        String command = boundSql.getSqlString();
        int start = this.pipelineStart(command);
        int depth = 1;
        char quote = 0;
        // Find the pipeline boundary, preserving nested arrays, options and trailing whitespace.
        for (int i = start + 1; i < command.length(); i++) {
            char ch = command.charAt(i);
            if (quote != 0) {
                if (ch == '\\') {
                    i++;
                } else if (ch == quote) {
                    quote = 0;
                }
            } else if (ch == '\'' || ch == '"') {
                quote = ch;
            } else if (ch == '[') {
                depth++;
            } else if (ch == ']' && --depth == 0) {
                String separator = command.substring(start + 1, i).isBlank() ? "" : ", ";
                return new BoundSql.BoundSqlObj(command.substring(0, i) + separator + stage + command.substring(i), boundSql.getArgs());
            }
        }
        throw new IllegalArgumentException("Unclosed MongoDB aggregation pipeline");
    }
}
