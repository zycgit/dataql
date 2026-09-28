/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon;
import java.util.List;
import java.util.Map;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;
import org.junit.jupiter.api.Test;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InitializationTest {
    @Test
    void containerInitializesStorageBeforeBuildingDatawayAndItsConsumers() throws Throwable {
        String previousBean = System.getProperty("dataway.metadata.bean");
        var builder = new DatawayConfig();
        try {
            Solon.start(InitializationTest.class, new String[] { "--server.port=0", "--dataway.admin-enabled=true", "--dataway.metadata.bean=storage" }, app -> {
                app.pluginAdd(0, new DatawayPlugin(builder));
                app.pluginAdd(100, context -> {
                    context.beanMake(Consumer.class);
                    context.beanRegister(context.beanMake(InitializedAccess.class), "storage", true);
                });
            });
            var context = Solon.context();
            assertTrue(context.getBean(Consumer.class).initialized);
            assertSame(context.getBean(Dataway.class), context.getBean(Consumer.class).dataway);
        } finally {
            Solon.stopBlock(false, 0);
            if (previousBean == null) {
                System.clearProperty("dataway.metadata.bean");
            } else {
                System.setProperty("dataway.metadata.bean", previousBean);
            }
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

        @Init(index = DatawayPlugin.INITIALIZATION_INDEX + 1)
        public void init() {
            assertTrue(this.dataway.getAdminService().list().isEmpty());
            this.initialized = true;
        }
    }
}
