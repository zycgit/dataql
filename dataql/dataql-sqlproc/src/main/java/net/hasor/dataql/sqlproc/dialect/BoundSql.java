/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dialect;
import java.util.Arrays;

/**
 * SQL
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-10-31
 */
public interface BoundSql {
    String getSqlString();

    Object[] getArgs();

    class BoundSqlObj implements BoundSql {
        /** SQL */
        private final String   sqlString;
        private final Object[] paramArray;

        public BoundSqlObj(String sqlString) {
            this.sqlString = sqlString;
            this.paramArray = new Object[0];
        }

        public BoundSqlObj(String sqlString, Object[] paramArray) {
            this.sqlString = sqlString;
            this.paramArray = paramArray;
        }

        public String getSqlString() {
            return this.sqlString;
        }

        @Override
        public Object[] getArgs() {
            return this.paramArray;
        }

        @Override
        public String toString() {
            return "BoundSqlObj{'" + sqlString + '\'' + ", args=" + Arrays.toString(paramArray) + '}';
        }
    }
}
