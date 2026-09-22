/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;

import java.util.Objects;
import net.hasor.dataway.dal.MetadataContext;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.env.Environment;

/** Passes Spring resources to independently constructed SPI providers. */
public class SpringMetadataContext implements MetadataContext {
    private final ListableBeanFactory beans;
    private final Environment         environment;

    public SpringMetadataContext(ListableBeanFactory beans, Environment environment) {
        this.beans = Objects.requireNonNull(beans);
        this.environment = Objects.requireNonNull(environment);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return org.springframework.boot.context.properties.bind.Binder.get(environment).bind(key, String.class).orElse(defaultValue);
    }

    @Override
    public <T> T getBean(String name, Class<T> type) {
        if (name != null && !name.isBlank()) {
            return beans.getBean(name, type);
        }
        T value = beans.getBeanProvider(type).getIfAvailable();
        if (value == null && type.getName().equals("net.hasor.dataway.dal.jdbc.JdbcExecutor")) {
            return type.cast(SpringJdbcSupport.create(this));
        }
        return value;
    }
}
