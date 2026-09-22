/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor;
import java.util.Objects;
import net.hasor.core.AppContext;
import net.hasor.core.spi.AppContextAware;
import net.hasor.dataway.dal.MetadataContext;

public class HasorMetadataContext implements MetadataContext, AppContextAware {
    private AppContext context;

    @Override
    public void setAppContext(AppContext context) {
        this.context = Objects.requireNonNull(context);
    }

    @Override
    public ClassLoader getClassLoader() {
        ClassLoader loader = this.context.getClassLoader();
        return loader == null ? HasorMetadataContext.class.getClassLoader() : loader;
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return this.context.getSettings().getString(key, defaultValue);
    }

    @Override
    public <T> T getBean(String name, Class<T> type) {
        if (name != null && !name.isBlank()) {
            var binding = this.context.findBindingRegister(name, type);
            if (binding == null) {
                throw new IllegalStateException("Missing metadata resource: " + name);
            }
            return this.context.getInstance(binding);
        }

        var bindings = this.context.findBindingRegister(type);
        if (bindings.size() > 1) {
            throw new IllegalStateException("Multiple metadata resources of type " + type.getName() + "; configure a bean name");
        }

        return bindings.isEmpty() ? null : this.context.getInstance(bindings.getFirst());
    }
}
