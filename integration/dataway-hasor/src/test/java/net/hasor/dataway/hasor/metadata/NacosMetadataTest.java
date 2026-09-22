/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.metadata;

import java.lang.reflect.Proxy;
import java.util.Map;
import com.alibaba.nacos.api.config.ConfigService;
import net.hasor.dataway.Dataway;
import net.hasor.dataway.spi.CallContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import net.hasor.core.Hasor;
import net.hasor.dataway.hasor.DatawayModule;

class NacosMetadataTest {
    private ConfigService client() {
        return (ConfigService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {ConfigService.class}, (proxy, method, args) -> {
            if (method.getName().equals("getConfig")) {
                assertEquals("host-store", args[0]);
                assertEquals("HOST_GROUP", args[1]);
                assertEquals(1500L, args[2]);
                return "{\"format\":1,\"generation\":\"initial\",\"records\":{\"INFO\":{},\"RELEASE\":{}}}";
            }
            if (method.getName().equals("equals")) {
                return proxy == args[0];
            }
            if (method.getName().equals("hashCode")) {
                return System.identityHashCode(proxy);
            }
            if (method.getName().equals("toString")) {
                return "hostConfigService";
            }
            throw new AssertionError("Unexpected call: " + method);
        });
    }

    @Test
    void containerSuppliesClientToTheServiceLoaderProvider() throws Throwable {
        var settings = new java.util.Properties();
        settings.setProperty("dataway.admin-enabled", "true");
        settings.setProperty("dataway.metadata.type", "nacos");
        settings.setProperty("dataway.metadata.nacos.config-service", "hostClient");
        settings.setProperty("dataway.metadata.nacos.data-id", "host-store");
        settings.setProperty("dataway.metadata.nacos.group", "HOST_GROUP");
        settings.setProperty("dataway.metadata.nacos.timeout-millis", "1500");
        try (var context = Hasor.create().loadSettings(settings).build(new DatawayModule(), binder -> {
            binder.bindType(ConfigService.class).nameWith("hostClient").toInstance(client());
        })) {
            assertTrue(context.getInstance(Dataway.class).getService().list(CallContext.LOCAL).isEmpty());
        }
    }
}
