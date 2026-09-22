/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import net.hasor.core.Hasor;
import net.hasor.core.Init;
import net.hasor.core.Inject;
import net.hasor.dataway.Dataway;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.dataway.spi.CallContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InitializationTest {
    @Test
    void containerInitializesStorageBeforeBuildingDatawayAndItsConsumers() throws Throwable {
        var settings = new Properties();
        settings.setProperty("dataway.admin-enabled", "true");
        var builder = Dataway.builder().adminHandler(dataway -> {
            assertTrue(dataway.getService().list(CallContext.LOCAL).isEmpty());
            return (request, response) -> response.write(200, Map.of());
        });
        try (var context = Hasor.create().loadSettings(settings).build(binder -> binder.bindType(Consumer.class).asEagerSingleton(), new DatawayModule(builder), binder -> binder.bindType(net.hasor.dataway.dal.ApiDataAccessLayer.class).to(InitializedAccess.class).asEagerSingleton())) {
            assertTrue(context.getInstance(Consumer.class).initialized);
            assertSame(context.getInstance(Dataway.class), context.getInstance(Consumer.class).dataway);
        }
    }

    public static class InitializedAccess extends JdbcDataAccessLayer {
        boolean initialized;

        public InitializedAccess() {
            super(TestDatabase.create(), "");
        }

        @Init
        public void init() {
            this.initialized = true;
        }

        @Override
        public List<Map<FieldDef, String>> listObjects(EntityType type, Map<FieldDef, String> conditions) {
            assertTrue(this.initialized, "Storage init must run before Dataway uses it");
            return super.listObjects(type, conditions);
        }
    }

    public static class Consumer {
        @Inject
        private Dataway dataway;
        boolean initialized;

        @Init
        public void init() {
            assertTrue(this.dataway.getService().list(CallContext.LOCAL).isEmpty());
            this.initialized = true;
        }
    }
}
