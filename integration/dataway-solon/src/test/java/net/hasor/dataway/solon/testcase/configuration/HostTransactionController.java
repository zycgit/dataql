/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.testcase.configuration;
import java.sql.SQLException;
import java.util.Map;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.dal.jdbc.JdbcExecutor;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayException;
import org.noear.solon.core.AppContext;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Handler;
import org.noear.solon.data.annotation.TransactionAnno;
import org.noear.solon.data.tran.TranUtils;

/** A host business action sharing Solon's transaction with Dataway metadata writes. */
public final class HostTransactionController implements Handler {
    private final AppContext   beans;
    private final JdbcExecutor executor;

    public HostTransactionController(AppContext beans, JdbcExecutor executor) {
        this.beans = beans;
        this.executor = executor;
    }

    @Override
    public void handle(Context context) throws Throwable {
        UserIdentity identity = context.attr("host.identity");
        if (!identity.checkOperation(Operation.PUBLISH)) {
            throw new DatawayException(401, "Unauthorized");
        }
        TranUtils.execute(new TransactionAnno(), () -> {
            this.saveAndPublish(this.beans.getBean(Dataway.class), context.param("mode"));
        });
        context.setHandled(true);
        context.outputAsJson(JsonUtils.writeValueAsString(Map.of("committed", true)));
    }

    private void saveAndPublish(Dataway dataway, String mode) {
        ApiDefinition definition = new ApiDefinition();
        definition.setId("transaction");
        definition.setMethod("GET");
        definition.setPath("/transaction");
        definition.setType(ApiScriptType.DATA_QL);
        definition.setScript("return 'committed';");
        definition.setDescription("Host transaction example");
        definition.setSchema("{}");
        definition.setSample("{}");
        definition.setOptions("{\"resultHandler\":\"raw\"}");
        dataway.getAdminService().save(definition, 0);
        dataway.getAdminService().publish("transaction", 1);
        if (mode.equals("rollback")) {
            throw new DatawayException(409, "Host transaction rolled back");
        }
        if (mode.equals("sql-failure")) {
            try {
                this.executor.execute(connection -> {
                    try (var statement = connection.createStatement()) {
                        statement.execute("INSERT INTO missing_business_table VALUES (1)");
                        return null;
                    }
                });
            } catch (SQLException error) {
                throw new DatawayException(503, "Business SQL failed", error);
            }
        }
    }
}
