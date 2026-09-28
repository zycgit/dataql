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
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.nacos.NacosDataAccessLayer;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.spring.DatawayAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void containerSuppliesNacosAccessLayer() throws Throwable {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("metadata", Map.of("dataway.admin-enabled", "true", "dataway.metadata.bean", "metadataStore")));
            context.registerBean("metadataStore", ApiDataAccessLayer.class, () -> new NacosDataAccessLayer(this.client(), "host-store", "HOST_GROUP", 1500));
            context.register(DatawayAutoConfiguration.class);
            context.refresh();
            assertTrue(context.getBean(Dataway.class).getAdminService().list().isEmpty());
        }
    }
}
