/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.metadata;
import java.lang.reflect.Proxy;
import java.util.Properties;
import com.alibaba.nacos.api.config.ConfigService;
import net.hasor.core.Hasor;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.dal.nacos.NacosDataAccessLayer;
import net.hasor.dataway.hasor.DatawayModule;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NacosMetadataTest {
    private ConfigService client(boolean mapped) {
        return (ConfigService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { ConfigService.class }, (proxy, method, args) -> {
            if (method.getName().equals("getConfig")) {
                assertEquals("host-store", args[0]);
                assertEquals("HOST_GROUP", args[1]);
                assertEquals(1500L, args[2]);
                if (mapped) {
                    return """
                            {"format":1,"generation":"initial","records":{
                              "definitions":{"sample":{"api_id":"sample","REVISION":"1","METHOD":"GET",
                                "PATH":"/sample","STATUS":"0","TYPE":"DataQL"}},
                              "publications":{}}}
                            """;
                }
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

    @ParameterizedTest
    @CsvSource({ "false, false", "true, false", "false, true", "true, true" })
    void containerSuppliesNacosAccessLayer(boolean mapped, boolean documentsOnly) throws Throwable {
        DatawayConfig config = new DatawayConfig();
        if (mapped) {
            config.tableMapping(EntityType.INFO, "definitions").tableMapping(EntityType.RELEASE, "publications").fieldMapping(EntityType.INFO, FieldDef.ID, "api_id");
        }
        var settings = new Properties();
        settings.setProperty("dataway.admin-enabled", Boolean.toString(!documentsOnly));
        settings.setProperty("dataway.docs-enabled", Boolean.toString(documentsOnly));
        try (var context = Hasor.create().loadSettings(settings).build(new DatawayModule(config), binder -> {
            binder.bindType(ApiDataAccessLayer.class).toInstance(new NacosDataAccessLayer(this.client(mapped), "host-store", "HOST_GROUP", 1500));
        })) {
            var apis = context.getInstance(Dataway.class).getAdminService().list();
            if (mapped) {
                assertEquals("sample", apis.getFirst().getId());
            } else {
                assertTrue(apis.isEmpty());
            }
        }
    }
}
