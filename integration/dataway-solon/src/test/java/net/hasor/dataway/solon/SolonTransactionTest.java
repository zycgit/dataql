/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon;
import java.util.Map;
import net.hasor.dataway.Dataway;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.dataway.service.model.ApiDefinition;
import net.hasor.dataway.service.model.ScriptType;
import net.hasor.dataway.spi.CallContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.noear.solon.Solon;
import org.noear.solon.data.annotation.TransactionAnno;
import org.noear.solon.data.tran.TranUtils;
import static org.junit.jupiter.api.Assertions.*;

class SolonTransactionTest {
    @Test
    void namedAccessLayerDoesNotFallBackOnMissingOrWrongType() throws Throwable {
        for (String name : new String[] { "first", "missing", "wrong", "" }) {
            try {
                Solon.start(SolonTransactionTest.class, new String[] { "--server.port=0", "--dataway.admin-enabled=true", "--dataway.metadata.bean=" + name }, app -> {
                    app.context().wrapAndPut("first", new JdbcDataAccessLayer(TestDatabase.create(), ""));
                    app.context().wrapAndPut("second", new JdbcDataAccessLayer(TestDatabase.create(), ""));
                    app.context().wrapAndPut("wrong", "not a storage layer");
                });
                Executable start = () -> {
                    new DatawayPlugin().start(Solon.context());
                    Solon.context().getBean(Dataway.class).getService().list(CallContext.LOCAL);
                };
                if (name.equals("first")) {
                    start.execute();
                    assertTrue(Solon.context().getBean(Dataway.class).getService().list(CallContext.LOCAL).isEmpty());
                } else {
                    Throwable failure = assertThrows(Throwable.class, start);
                    while (failure.getCause() != null) {
                        failure = failure.getCause();
                    }
                    assertTrue(failure.getMessage().contains("ApiDataAccessLayer"));
                }
            } finally {
                Solon.stopBlock(false, 0);
            }
        }
    }

    @Test
    void hostTransactionRollsBackPluginAssembledDatawayAndThenCanCommit() throws Throwable {
        var source = TestDatabase.create();
        Solon.start(SolonTransactionTest.class, new String[] { "--server.port=0", "--dataway.admin-enabled=true" }, app -> {
            app.pluginAdd(0, new DatawayPlugin());
            app.context().wrapAndPut(ApiDataAccessLayer.class, new JdbcDataAccessLayer(new SolonJdbcExecutor(source), ""));
        });
        try {
            Dataway dataway = Solon.context().getBean(Dataway.class);
            var api = new ApiDefinition("one", "GET", "/one", ScriptType.DATAQL, "return 1;", "");
            assertThrows(IllegalStateException.class, () -> TranUtils.execute(new TransactionAnno(), () -> {
                dataway.getService().save(api, 0, CallContext.LOCAL);
                dataway.getService().publish("one", 1, CallContext.LOCAL);
                throw new IllegalStateException("host failure");
            }));
            assertTrue(dataway.getService().list(CallContext.LOCAL).isEmpty());
            try (var connection = source.getConnection(); var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT COUNT(*) FROM interface_release")) {
                rows.next();
                assertEquals(0, rows.getInt(1));
            }
            TranUtils.execute(new TransactionAnno(), () -> {
                dataway.getService().save(api, 0, CallContext.LOCAL);
                dataway.getService().publish("one", 1, CallContext.LOCAL);
            });
            assertEquals(1, ((Number) dataway.getService().invokeApi("/one", Map.of())).intValue());
        } finally {
            Solon.stopBlock(false, 0);
        }
    }
}
