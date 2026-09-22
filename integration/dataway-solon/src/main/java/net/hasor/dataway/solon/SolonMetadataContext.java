/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;
import net.hasor.dataway.dal.MetadataContext;
import org.noear.solon.core.AppContext;

public class SolonMetadataContext implements MetadataContext {
    private final AppContext context;

    public SolonMetadataContext(AppContext context) {
        this.context = Objects.requireNonNull(context);
    }

    @Override
    public ClassLoader getClassLoader() {
        ClassLoader loader = this.context.getClassLoader();
        return loader == null ? SolonMetadataContext.class.getClassLoader() : loader;
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return this.context.app().cfg().getProperty(key, defaultValue);
    }

    @Override
    public <T> T getBean(String name, Class<T> type) {
        if (name != null && !name.isBlank()) {
            Object value = this.context.getBean(name);
            if (value == null) {
                throw new IllegalStateException("Missing metadata resource: " + name);
            }
            return type.cast(value);
        }

        List<T> values = this.context.getBeansOfType(type).stream().distinct().toList();
        if (values.size() > 1) {
            throw new IllegalStateException("Multiple metadata resources of type " + type.getName() + "; configure a bean name");
        }

        if (!values.isEmpty()) {
            return values.getFirst();
        }

        if (type.getName().equals("net.hasor.dataway.dal.jdbc.JdbcExecutor")) {
            String sourceName = this.getProperty("dataway.metadata.jdbc.data-source", "");
            DataSource source = this.getBean(sourceName, DataSource.class);
            if (source == null) {
                throw new IllegalStateException("JDBC metadata requires a DataSource");
            }
            return type.cast(new SolonJdbcExecutor(source));
        }
        return null;
    }
}
