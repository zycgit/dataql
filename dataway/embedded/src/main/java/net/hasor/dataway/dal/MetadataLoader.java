/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal;
import java.util.*;

/** Shared selection rules for all containers and standalone embedding. */
public final class MetadataLoader {
    private MetadataLoader() {
    }

    public static ApiDataAccessLayer create(MetadataContext context) {
        Objects.requireNonNull(context, "context");
        String bean = context.getProperty("dataway.metadata.bean", "");
        ApiDataAccessLayer supplied = context.getBean(bean, ApiDataAccessLayer.class);
        if (supplied != null) {
            return supplied;
        }

        String selected = context.getProperty("dataway.metadata.type", "").trim();
        if (selected.isEmpty()) {
            throw new IllegalStateException("Supply an ApiDataAccessLayer or configure dataway.metadata.type");
        }

        Map<String, MetadataProvider> providers = new LinkedHashMap<>();
        try {
            for (MetadataProvider provider : ServiceLoader.load(MetadataProvider.class, context.getClassLoader())) {
                String name = Objects.requireNonNull(provider.getName(), "provider name");
                if (name.isBlank() || providers.putIfAbsent(name, provider) != null) {
                    throw new IllegalStateException("Invalid or duplicate metadata provider: " + name);
                }
            }
        } catch (ServiceConfigurationError error) {
            throw new IllegalStateException("Cannot load metadata providers; check installed storage extensions", error);
        }

        MetadataProvider provider = providers.get(selected);
        if (provider == null) {
            throw new IllegalStateException("No metadata provider '" + selected + "'; install its extension. Available: " + providers.keySet());
        }
        return Objects.requireNonNull(provider.create(context), "Metadata provider returned null");
    }
}
