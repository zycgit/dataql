/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.document;
import java.util.ArrayList;
import java.util.List;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.config.MemoryResponse;
import net.hasor.dataway.service.config.ServiceTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class DocumentHandlerTest extends ServiceTestSupport {
    @ParameterizedTest
    @ValueSource(strings = { "/swagger2.json", "/openapi.json" })
    void documentRoutesAuthorizeTheResolvedIdentityAndSupportHead(String path) throws Exception {
        UserIdentity identity = UserIdentity.authenticated("reader");
        List<Operation> operations = new ArrayList<>();
        this.config.identityProvider(request -> identity).authorizationCheck((user, operation) -> {
            assertSame(identity, user);
            operations.add(operation);
            return true;
        }).adminInterceptor((context, chain) -> {
            fail("Documents must not use management interceptors");
            return null;
        }).apiInterceptor((context, chain) -> {
            fail("Documents must not execute scripts");
            return null;
        });
        Dataway dataway = this.config.createDataway();
        assertEquals(List.of("/swagger2.json", "/openapi.json"), dataway.getDocumentHandler().paths());
        MemoryResponse response = this.handle(dataway.getDocumentHandler(), "GET", path);
        assertEquals(200, response.getStatus());
        assertTrue(response.text().contains(path.equals("/swagger2.json") ? "\"swagger\":\"2.0\"" : "\"openapi\":\"3.2.1\""));
        assertArrayEquals(new byte[0], this.handle(dataway.getDocumentHandler(), "head", path).bytes());
        assertEquals(List.of(Operation.DOCUMENT, Operation.DOCUMENT), operations);
    }

    @Test
    void unknownPathsWrongMethodsAndUnauthorizedReadsFailBeforeExport() {
        Dataway dataway = this.config.authorizationCheck((identity, operation) -> false).createDataway();
        assertEquals(404, assertThrows(DatawayException.class, () -> this.handle(dataway.getDocumentHandler(), "GET", "/")).status());
        assertEquals(405, assertThrows(DatawayException.class, () -> this.handle(dataway.getDocumentHandler(), "POST", "/openapi.json")).status());
        assertEquals(401, assertThrows(DatawayException.class, () -> this.handle(dataway.getDocumentHandler(), "GET", "/openapi.json")).status());
    }
}
