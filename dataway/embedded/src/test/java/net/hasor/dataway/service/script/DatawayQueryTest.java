/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.script;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.kernel.QueryResult;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.service.ResultInfoUtils;
import net.hasor.dataway.service.config.MemoryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatawayQueryTest extends ScriptTestSupport {
    @Test
    void scopeDefaultsMergeWithRequestParametersAndOtherScopesRemainDelegated() throws Exception {
        this.config.resultHandler("raw");
        this.scope = symbol -> switch (symbol) {
            case "$" -> Map.of("name", "default", "fallback", "host");
            case "@" -> Map.of("name", "context");
            case "#" -> Map.of("name", "environment");
            default -> throw new AssertionError(symbol);
        };
        DatawayQuery query = this.engine().newQuery(this.definition("api", "return [${name}, ${fallback}, @{name}, #{name}];"), List.of(), null);
        Map<String, String> input = Map.of("name", "request");
        assertEquals(List.of("request", "host", "context", "environment"), this.execute(query, input));
        assertEquals(Map.of("name", "request"), input);
    }

    @Test
    void interceptorsSeePreparedParametersAndExecuteInRegistrationOrder() throws Exception {
        this.config.resultHandler("raw");
        this.scope = symbol -> null;
        List<String> events = new ArrayList<>();
        UserIdentity identity = UserIdentity.authenticated("caller", Map.of());
        this.interceptors.add((context, chain) -> {
            assertEquals("api", context.definition().getId());
            assertEquals(Operation.INVOKE, context.operation());
            assertSame(identity, context.identity());
            assertEquals(Map.of("name", "input"), context.parameters());
            events.add("first-before");
            Object result = chain.proceed(context);
            events.add("first-after");
            return result;
        });
        this.interceptors.add((context, chain) -> {
            events.add("second-before");
            Object result = chain.proceed(context);
            events.add("second-after");
            return result;
        });
        DatawayQuery query = this.engine().newQuery(this.definition("api", "return ${name};"), List.of(), null);
        assertEquals("input", query.execute(Operation.INVOKE, identity, Map.of("name", "input"), Map.of(), new MemoryResponse()).getData());
        assertEquals(List.of("first-before", "second-before", "second-after", "first-after"), events);
    }

    @Test
    void shortCircuitResultsRemainUnwrappedAndMissingIdentityBecomesAnonymous() throws Exception {
        ResultInfo response = ResultInfoUtils.json(202, Map.of("queued", true));
        this.interceptors.add((context, chain) -> {
            assertNull(context.identity().identityId());
            assertFalse(context.identity().authenticated());
            assertTrue(context.identity().attributes().isEmpty());
            assertTrue(context.identity().operations().isEmpty());
            return response;
        });
        DatawayQuery query = this.engine().newQuery(this.definition("api", "throw 500, 'must not execute';"), List.of(), null);
        assertSame(response, query.execute(Operation.INVOKE, null, Map.of(), Map.of(), new MemoryResponse()));
    }

    @ParameterizedTest
    @MethodSource("binaryResults")
    void queryResultsContainingExplicitOrBinaryResponsesBypassTheTemplate(Object value) throws Exception {
        DataModel model = mock(DataModel.class);
        when(model.unwrap()).thenReturn(value);
        QueryResult result = mock(QueryResult.class);
        when(result.getData()).thenReturn(model);
        this.interceptors.add((context, chain) -> result);
        DatawayQuery query = this.engine().newQuery(this.definition("api", "return 1;"), List.of(), null);
        ResultInfo response = query.execute(Operation.INVOKE, null, Map.of(), Map.of(), new MemoryResponse());
        if (value instanceof ResultInfo) {
            assertSame(value, response);
        } else {
            assertSame(value, response.getData());
            assertFalse(response.isJson());
        }
    }

    private static Stream<Object> binaryResults() {
        return Stream.of(new byte[] { 1, 2 }, new ByteArrayInputStream(new byte[] { 3 }), ResultInfoUtils.json(201, "created"));
    }

    @Test
    void scriptThrowsRetainTheirCodeValueAndLocation() throws Exception {
        DatawayQuery query = this.engine().newQuery(this.definition("api", "throw 409, {'reason':'conflict'};"), List.of(), null);
        Map<?, ?> result = (Map<?, ?>) this.execute(query, Map.of());
        assertEquals(false, result.get("success"));
        assertEquals(409, ((Number) result.get("code")).intValue());
        assertEquals(Map.of("reason", "conflict"), result.get("value"));
        assertNotEquals("Unknown", result.get("location"));
        assertNotNull(result.get("message"));
        assertTrue(((Number) result.get("executionTime")).longValue() >= 0);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void executionFailuresUseTheSelectedResponseStructure(boolean structured) throws Exception {
        this.config.resultHandler(structured ? "structure" : "raw");
        this.interceptors.add((context, chain) -> {
            throw new ExecutionException(new IllegalStateException("storage offline"));
        });
        DatawayQuery query = this.engine().newQuery(this.definition("api", "return 1;"), List.of(), null);
        Object result = this.execute(query, Map.of());
        if (structured) {
            Map<?, ?> failure = (Map<?, ?>) result;
            assertEquals(false, failure.get("success"));
            assertEquals(500, failure.get("code"));
            assertEquals("storage offline", failure.get("value"));
            assertEquals("Unknown", failure.get("location"));
            assertEquals(-1, ((Number) failure.get("executionTime")).intValue());
        } else {
            assertEquals("storage offline", result);
        }
    }

    @Test
    void nullErrorMessagesStillProduceAnErrorEnvelopeWhenStructureIsDisabled() throws Exception {
        this.config.resultHandler("raw");
        this.interceptors.add((context, chain) -> {
            throw new IllegalStateException();
        });
        DatawayQuery query = this.engine().newQuery(this.definition("api", "return 1;"), List.of(), null);
        Map<?, ?> result = (Map<?, ?>) this.execute(query, Map.of());
        assertEquals(false, result.get("success"));
        assertNull(result.get("value"));
        assertEquals(500, result.get("code"));
    }

    @Test
    void responseTemplateResolvesPlaceholdersAndCopiesNestedLiteralsBetweenCalls() throws Exception {
        this.config.responseFormat("""
                {"data":"@resultData","status":"@resultStatus","code":"@resultCode","message":"@resultMessage",
                 "where":"@codeLocation","lifecycle":"@timeLifeCycle","execution":"@timeExecution",
                 "literal":{"list":[{"name":"original"},null]},"text":"@unknown"}
                """);
        DatawayQuery query = this.engine().newQuery(this.definition("api", "return 'value';"), List.of(), null);
        Map<?, ?> first = (Map<?, ?>) this.execute(query, Map.of());
        assertEquals("value", first.get("data"));
        assertEquals(true, first.get("status"));
        assertEquals("OK", first.get("message"));
        assertEquals("@unknown", first.get("text"));
        Map<?, ?> literal = (Map<?, ?>) first.get("literal");
        ((List<?>) literal.get("list")).clear();
        Map<?, ?> second = (Map<?, ?>) this.execute(query, Map.of());
        assertEquals(2, ((List<?>) ((Map<?, ?>) second.get("literal")).get("list")).size());
        assertNotSame(first.get("literal"), second.get("literal"));
    }
}
