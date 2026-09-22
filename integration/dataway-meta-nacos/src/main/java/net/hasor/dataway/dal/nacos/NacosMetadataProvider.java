/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.nacos;
import com.alibaba.nacos.api.config.ConfigService;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.MetadataContext;
import net.hasor.dataway.dal.MetadataProvider;

/** Nacos SPI; the application owns ConfigService creation and shutdown. */
public class NacosMetadataProvider implements MetadataProvider {
    @Override
    public String getName() {
        return "nacos";
    }

    @Override
    public ApiDataAccessLayer create(MetadataContext context) {
        String name = context.getProperty("dataway.metadata.nacos.config-service", "");
        ConfigService service = context.getBean(name, ConfigService.class);
        if (service == null) {
            throw new IllegalStateException("Nacos metadata requires a ConfigService");
        }

        String dataId = context.getProperty("dataway.metadata.nacos.data-id", "dataway-store.json");
        String group = context.getProperty("dataway.metadata.nacos.group", "HASOR_DATAWAY");
        long timeout = Long.parseLong(context.getProperty("dataway.metadata.nacos.timeout-millis", "3000"));
        return new NacosDataAccessLayer(service, dataId, group, timeout);
    }
}
