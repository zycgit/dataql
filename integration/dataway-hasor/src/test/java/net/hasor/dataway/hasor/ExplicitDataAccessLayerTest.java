/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor;
import java.util.Map;
import java.util.Properties;
import net.hasor.core.Hasor;
import net.hasor.dataway.Dataway;
import net.hasor.dataway.dal.jdbc.LocalJdbcExecutor;
import net.hasor.dataway.service.model.ApiDefinition;
import net.hasor.dataway.service.model.ScriptType;
import net.hasor.dataway.spi.CallContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExplicitDataAccessLayerTest {
    @Test
    void namedAccessLayerDoesNotFallBackOnMissingOrWrongType() throws Throwable {
        for (String name : new String[] { "first", "missing", "wrong", "" }) {
            var settings = new Properties();
            settings.setProperty("dataway.admin-enabled", "true");
            settings.setProperty("dataway.metadata.bean", name);
            org.junit.jupiter.api.function.Executable start = () -> {
                try (var context = Hasor.create().loadSettings(settings).build(new DatawayModule(), binder -> {
                    binder.bindType(net.hasor.dataway.dal.ApiDataAccessLayer.class).nameWith("first").toInstance(TestDatabase.dataAccessLayer());
                    binder.bindType(net.hasor.dataway.dal.ApiDataAccessLayer.class).nameWith("second").toInstance(TestDatabase.dataAccessLayer());
                    binder.bindType(String.class).nameWith("wrong").toInstance("not a storage layer");
                })) {
                    assertTrue(context.getInstance(Dataway.class).getService().list(CallContext.LOCAL).isEmpty());
                }
            };
            if (name.equals("first")) {
                start.execute();
            } else {
                Throwable failure = assertThrows(Throwable.class, start);
                while (failure.getCause() != null) {
                    failure = failure.getCause();
                }
                assertTrue(failure.getMessage().contains("ApiDataAccessLayer"));
            }
        }
    }

    @Test
    void missingAccessLayerFailsDuringContainerStartup() {
        var settings = new Properties();
        settings.setProperty("dataway.admin-enabled", "true");
        var failure = assertThrows(Throwable.class, () -> {
            try (var ignored = Hasor.create().loadSettings(settings).build(new DatawayModule())) {
                fail("Startup must reject a missing access layer");
            }
        });
        assertFalse(failure instanceof AssertionError);
        while (failure.getCause() != null) {
            failure = failure.getCause();
        }
        assertTrue(failure.getMessage().contains("ApiDataAccessLayer"));
    }

    @Test
    void moduleUsesTheHostBoundTransactionComponent() throws Throwable {
        var source = TestDatabase.create();
        var host = new LocalJdbcExecutor(source);
        var settings = new Properties();
        settings.setProperty("dataway.admin-enabled", "true");
        try (var context = Hasor.create().loadSettings(settings).build(new DatawayModule(), binder -> {
            binder.bindType(net.hasor.dataway.dal.ApiDataAccessLayer.class).toInstance(new net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer(host, ""));
        })) {
            Dataway dataway = context.getInstance(Dataway.class);
            var api = new ApiDefinition("one", "GET", "/one", ScriptType.DATAQL, "return 1;", "");
            assertThrows(IllegalStateException.class, () -> host.execute(connection -> {
                dataway.getService().save(api, 0, CallContext.LOCAL);
                dataway.getService().publish("one", 1, CallContext.LOCAL);
                throw new IllegalStateException("host failure");
            }));
            assertTrue(dataway.getService().list(CallContext.LOCAL).isEmpty());
            host.execute(connection -> {
                dataway.getService().save(api, 0, CallContext.LOCAL);
                dataway.getService().publish("one", 1, CallContext.LOCAL);
                return null;
            });
            assertEquals(1, ((Number) dataway.getService().invokeApi("/one", Map.of())).intValue());
        }
    }
}
