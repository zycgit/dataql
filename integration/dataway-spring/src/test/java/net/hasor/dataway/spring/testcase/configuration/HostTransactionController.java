/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.testcase.configuration;
import java.sql.SQLException;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.dal.jdbc.JdbcExecutor;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** A host business action that changes metadata inside the host's transaction. */
@RestController
public class HostTransactionController {
    private final Dataway             dataway;
    private final JdbcExecutor        executor;
    private final TransactionTemplate transactions;

    public HostTransactionController(Dataway dataway, JdbcExecutor executor, PlatformTransactionManager manager) {
        this.dataway = dataway;
        this.executor = executor;
        this.transactions = new TransactionTemplate(manager);
    }

    @PostMapping("/host-transaction")
    public Map<String, ?> execute(HttpServletRequest request) {
        UserIdentity identity = (UserIdentity) request.getAttribute("host.identity");
        if (!identity.checkOperation(Operation.PUBLISH)) {
            throw new DatawayException(401, "Unauthorized");
        }
        return this.transactions.execute(status -> {
            this.saveAndPublish(this.dataway, request.getParameter("mode"));
            return Map.of("committed", true);
        });
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
