/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.metadata;

import java.lang.reflect.Proxy;
import java.util.Map;
import com.alibaba.nacos.api.config.ConfigService;
import net.hasor.dataway.Dataway;
import net.hasor.dataway.spi.CallContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.noear.solon.Solon;
import net.hasor.dataway.solon.DatawayPlugin;

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
        try {
            Solon.start(NacosMetadataTest.class, new String[] {"--server.port=0",
                    "--dataway.admin-enabled=true",
                    "--dataway.metadata.type=nacos",
                    "--dataway.metadata.nacos.config-service=hostClient",
                    "--dataway.metadata.nacos.data-id=host-store",
                    "--dataway.metadata.nacos.group=HOST_GROUP",
                    "--dataway.metadata.nacos.timeout-millis=1500"}, app -> {
                app.pluginAdd(0, new DatawayPlugin());
                app.pluginAdd(100, context -> context.wrapAndPut("hostClient", client()));
            });
            assertTrue(Solon.context().getBean(Dataway.class).getService().list(CallContext.LOCAL).isEmpty());
        } finally {
            Solon.stopBlock(false, 0);
        }
    }
}
