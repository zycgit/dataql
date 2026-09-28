/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor;
import java.util.Properties;
import net.hasor.core.Hasor;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.dataway.dal.jdbc.LocalJdbcExecutor;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.*;

class ExplicitDataAccessLayerTest {
    @Test
    void explicitStorageOrCoreDoesNotRequireAContainerMetadataBean() throws Throwable {
        var settings = new Properties();
        settings.setProperty("dataway.admin-enabled", "true");
        settings.setProperty("dataway.metadata.bean", "missing");
        var prepared = new Dataway(new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()));
        for (boolean suppliedCore : new boolean[] { false, true }) {
            DatawayModule module = suppliedCore ? new DatawayModule(prepared) : new DatawayModule(new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()));
            try (var context = Hasor.create().loadSettings(settings).build(module)) {
                assertTrue(context.getInstance(Dataway.class).getAdminService().list().isEmpty());
            }
        }
    }

    @Test
    void namedAccessLayerDoesNotFallBackOnMissingOrWrongType() throws Throwable {
        for (String name : new String[] { "first", "missing", "wrong", "" }) {
            var settings = new Properties();
            settings.setProperty("dataway.admin-enabled", "true");
            settings.setProperty("dataway.metadata.bean", name);
            Executable start = () -> {
                try (var context = Hasor.create().loadSettings(settings).build(new DatawayModule(), binder -> {
                    binder.bindType(ApiDataAccessLayer.class).nameWith("first").toInstance(TestDatabase.dataAccessLayer());
                    binder.bindType(ApiDataAccessLayer.class).nameWith("second").toInstance(TestDatabase.dataAccessLayer());
                    binder.bindType(String.class).nameWith("wrong").toInstance("not a storage layer");
                })) {
                    assertTrue(context.getInstance(Dataway.class).getAdminService().list().isEmpty());
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
            binder.bindType(ApiDataAccessLayer.class).toInstance(new JdbcDataAccessLayer(host, ""));
        })) {
            Dataway dataway = context.getInstance(Dataway.class);
            var api = new ApiDefinition();
            api.setId("one");
            api.setMethod("GET");
            api.setPath("/one");
            api.setType(ApiScriptType.DATA_QL);
            api.setScript("return 1;");
            api.setDescription("");
            assertThrows(IllegalStateException.class, () -> host.execute(connection -> {
                dataway.getAdminService().save(api, 0);
                dataway.getAdminService().publish("one", 1);
                throw new IllegalStateException("host failure");
            }));
            assertTrue(dataway.getAdminService().list().isEmpty());
            host.execute(connection -> {
                dataway.getAdminService().save(api, 0);
                dataway.getAdminService().publish("one", 1);
                return null;
            });
            String result = DatawayModuleTest.get(new DatawayController("/api", dataway.getApiHandler()), "/api/one");
            assertEquals(1, JsonUtils.readTree(result).get("value").intValue());
        }
    }
}
