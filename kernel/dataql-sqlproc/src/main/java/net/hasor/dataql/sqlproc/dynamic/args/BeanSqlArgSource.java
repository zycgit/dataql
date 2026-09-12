/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.args;
import java.util.*;
import net.hasor.cobble.BeanUtils;
import net.hasor.cobble.function.Property;
import net.hasor.dataql.sqlproc.types.SqlArgSource;

/**
 * @author 赵永春 (zyc@hasor.net)
 * @version 2014-3-31
 */
public class BeanSqlArgSource extends BindSqlArgSource implements SqlArgSource, SqlArgDisposer {
    private final Object                dataBean;
    private final Map<String, Property> dataProperty;

    public BeanSqlArgSource(Object dataBean) {
        this.dataBean = Objects.requireNonNull(dataBean);
        this.dataProperty = BeanUtils.getPropertyFunc(dataBean.getClass());
    }

    @Override
    public boolean hasValue(final String paramName) {
        if (this.bindValues.containsKey(paramName)) {
            return true;
        } else {
            return this.dataProperty.containsKey(paramName);
        }
    }

    @Override
    public Object getValue(final String paramName) throws IllegalArgumentException {
        if (this.bindValues.containsKey(paramName)) {
            return this.bindValues.get(paramName);
        } else if (this.dataProperty.containsKey(paramName)) {
            Property property = this.dataProperty.get(paramName);
            return property.get(this.dataBean);
        } else {
            return null;
        }
    }

    @Override
    public String[] getParameterNames() {
        Set<String> names = new HashSet<>();
        names.addAll(Arrays.asList(super.getParameterNames()));
        names.addAll(this.dataProperty.keySet());
        return names.toArray(new String[0]);
    }

    @Override
    public void cleanupParameters() {
        super.cleanupParameters();
        if (this.dataBean instanceof SqlArgDisposer) {
            ((SqlArgDisposer) this.dataBean).cleanupParameters();
        }
    }
}
