/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.args;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import net.hasor.dataql.sqlproc.types.SqlArgSource;

/**
 * Local argument bindings with fallback to a parent source.
 * @author 赵永春 (zyc@hasor.net)
 * @version 2014-3-31
 */
public class StackSqlArgSource extends BindSqlArgSource {
    private final SqlArgSource target;

    public StackSqlArgSource(SqlArgSource target) {
        this.target = Objects.requireNonNull(target);
    }

    @Override
    public boolean hasValue(final String paramName) {
        if (this.bindValues.containsKey(paramName)) {
            return true;
        } else {
            return this.target.hasValue(paramName);
        }
    }

    @Override
    public Object getValue(final String paramName) throws IllegalArgumentException {
        if (this.bindValues.containsKey(paramName)) {
            return super.getValue(paramName);
        } else {
            return this.target.getValue(paramName);
        }
    }

    @Override
    public void putValue(String paramName, Object value) {
        this.bindValues.put(paramName, value);
    }

    @Override
    public String[] getParameterNames() {
        Set<String> tmpKeys = new HashSet<>();
        tmpKeys.addAll(this.bindValues.keySet());
        tmpKeys.addAll(Arrays.asList(this.target.getParameterNames()));
        return tmpKeys.toArray(new String[0]);
    }

    @Override
    public void cleanupParameters() {
        if (this.target instanceof SqlArgDisposer) {
            ((SqlArgDisposer) this.target).cleanupParameters();
        }
        super.cleanupParameters();
    }
}
