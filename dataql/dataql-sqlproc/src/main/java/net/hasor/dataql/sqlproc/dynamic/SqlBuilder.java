/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.hasor.dataql.sqlproc.dialect.BoundSql;

/**
 * SQL Build
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-06-05
 */
public class SqlBuilder implements BoundSql {
    protected final StringBuilder queryString = new StringBuilder();
    protected final List<Object>  argList     = new ArrayList<>();

    public boolean lastSpaceCharacter() {
        if (this.queryString.length() == 0) {
            return true;
        } else {
            char charAt = this.queryString.charAt(this.queryString.length() - 1);
            return charAt == ' ' || charAt == '\n' || charAt == '\t';
        }
    }

    public void appendSql(String sql, Object... args) {
        this.queryString.append(sql);
        this.argList.addAll(Arrays.asList(args));
    }

    public void appendSql(String sql) {
        this.queryString.append(sql);
    }

    public void appendSql(BoundSql boundSql) {
        this.queryString.append(boundSql.getSqlString());
    }

    public void appendBuilder(BoundSql boundSql) {
        if (boundSql instanceof SqlBuilder) {
            this.queryString.append(((SqlBuilder) boundSql).queryString);
            this.argList.addAll(((SqlBuilder) boundSql).argList);
        } else {
            this.queryString.append(boundSql.getSqlString());
            this.argList.addAll(Arrays.asList(boundSql.getArgs()));
        }
    }

    public void appendArgs(BoundSql boundSql) {
        this.argList.addAll(Arrays.asList(boundSql.getArgs()));
    }

    @Override
    public String getSqlString() {
        return this.queryString.toString();
    }

    @Override
    public Object[] getArgs() {
        return this.argList.toArray();
    }
}
