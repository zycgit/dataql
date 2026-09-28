/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.Dataway;
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
                    Solon.context().getBean(Dataway.class).getAdminService().list();
                };
                if (name.equals("first")) {
                    start.execute();
                    assertTrue(Solon.context().getBean(Dataway.class).getAdminService().list().isEmpty());
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
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        Solon.start(SolonTransactionTest.class, new String[] { "--server.port=" + port, "--dataway.api-enabled=true", "--dataway.admin-enabled=true" }, app -> {
            app.pluginAdd(0, new DatawayPlugin());
            app.context().wrapAndPut(ApiDataAccessLayer.class, new JdbcDataAccessLayer(new SolonJdbcExecutor(source), ""));
        });
        try {
            Dataway dataway = Solon.context().getBean(Dataway.class);
            var api = new ApiDefinition();
            api.setId("one");
            api.setMethod("GET");
            api.setPath("/one");
            api.setType(ApiScriptType.DATA_QL);
            api.setScript("return 1;");
            api.setDescription("");
            assertThrows(IllegalStateException.class, () -> TranUtils.execute(new TransactionAnno(), () -> {
                dataway.getAdminService().save(api, 0);
                dataway.getAdminService().publish("one", 1);
                throw new IllegalStateException("host failure");
            }));
            assertTrue(dataway.getAdminService().list().isEmpty());
            try (var connection = source.getConnection(); var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT COUNT(*) FROM interface_release")) {
                rows.next();
                assertEquals(0, rows.getInt(1));
            }
            TranUtils.execute(new TransactionAnno(), () -> {
                dataway.getAdminService().save(api, 0);
                dataway.getAdminService().publish("one", 1);
            });
            try (var client = HttpClient.newHttpClient()) {
                var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/one")).GET().build();
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode());
                assertEquals(1, JsonUtils.readTree(response.body()).get("value").intValue());
            }
        } finally {
            Solon.stopBlock(false, 0);
        }
    }
}
