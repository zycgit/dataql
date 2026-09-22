/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.metadata;
import java.lang.reflect.Proxy;
import java.util.Map;
import com.alibaba.nacos.api.config.ConfigService;
import net.hasor.dataway.Dataway;
import net.hasor.dataway.spi.CallContext;
import net.hasor.dataway.spring.DatawayAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import static org.junit.jupiter.api.Assertions.*;

class NacosMetadataTest {
    private ConfigService client() {
        return (ConfigService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { ConfigService.class }, (proxy, method, args) -> {
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
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("metadata", Map.of("dataway.admin-enabled", "true", "dataway.metadata.type", "nacos", "dataway.metadata.nacos.config-service", "hostClient", "dataway.metadata.nacos.data-id", "host-store", "dataway.metadata.nacos.group", "HOST_GROUP", "dataway.metadata.nacos.timeout-millis", "1500")));
            context.registerBean("hostClient", ConfigService.class, this::client);
            context.register(DatawayAutoConfiguration.class);
            context.refresh();
            assertTrue(context.getBean(Dataway.class).getService().list(CallContext.LOCAL).isEmpty());
        }
    }
}
