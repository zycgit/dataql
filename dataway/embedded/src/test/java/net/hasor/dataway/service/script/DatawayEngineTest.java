/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.script;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import net.hasor.dataql.kernel.FragmentProcess;
import net.hasor.dataql.parser.QueryParseException;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.DatawayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class DatawayEngineTest extends ScriptTestSupport {
    @Test
    void dataqlUsesTheRealCompilerAndQueryCustomizers() throws Exception {
        this.config.resultHandler("raw");
        DatawayQuery query = this.engine().newQuery(this.definition("api", "return [customized, ${name}];"), List.of("name"), null);
        assertEquals(List.of("query", "value"), this.execute(query, Map.of("name", "value")));
        assertThrows(QueryParseException.class, () -> this.engine().newQuery(this.definition("api", "invalid script !!!"), List.of(), null));
    }

    @Test
    void queryOptionsOverrideDefaultsWithoutChangingOtherQueries() throws Exception {
        DatawayEngine engine = this.engine();
        ApiDefinition definition = this.definition("api", "return ${name};");
        DatawayQuery raw = engine.newQuery(definition, List.of("name"), Map.of("resultHandler", "raw"));
        assertEquals("value", this.execute(raw, Map.of("name", "value")));
        Map<?, ?> structured = (Map<?, ?>) this.execute(engine.newQuery(definition, List.of("name"), null), Map.of("name", "value"));
        assertEquals(true, structured.get("success"));
        assertEquals("value", structured.get("value"));
        assertEquals(0, ((Number) structured.get("code")).intValue());
        assertNull(structured.get("location"));
        assertTrue(((Number) structured.get("lifeCycleTime")).longValue() >= 0);
        assertTrue(((Number) structured.get("executionTime")).longValue() >= 0);
    }

    @Test
    void savedStructureOptionsResolveToHandlersAndExplicitNamesTakePrecedence() throws Exception {
        DatawayEngine engine = this.engine();
        ApiDefinition definition = this.definition("api", "return 'saved';");
        for (Map<String, Object> options : List.<Map<String, Object>>of(Map.of("resultStructure", false), Map.of("resultHandler", "default", "resultStructure", false))) {
            assertEquals("saved", this.execute(engine.newQuery(definition, List.of(), options), Map.of()));
        }
        Map<?, ?> structured = (Map<?, ?>) this.execute(engine.newQuery(definition, List.of(), Map.of("resultStructure", true)), Map.of());
        assertEquals("saved", structured.get("value"));
        assertEquals("saved", this.execute(engine.newQuery(definition, List.of(), Map.of("resultHandler", "raw", "resultStructure", true)), Map.of()));
        Map<String, Object> invalid = new LinkedHashMap<>();
        invalid.put("resultStructure", null);
        assertEquals(400, assertThrows(DatawayException.class, () -> engine.newQuery(definition, List.of(), invalid)).status());
    }

    @ParameterizedTest
    @MethodSource("invalidOptions")
    void invalidPerQueryOptionsFailBeforeExecution(String name, Object value) {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put(name, value);
        DatawayException error = assertThrows(DatawayException.class, () -> this.engine().newQuery(this.definition("api", "return 1;"), List.of(), options));
        assertEquals(400, error.status());
        assertTrue(error.getMessage().contains(name) || name.equals("wrapParameterName"), error.getMessage());
    }

    private static Stream<Arguments> invalidOptions() {
        return Stream.of(Arguments.of("responseFormat", null), Arguments.of("responseFormat", Map.of()), Arguments.of("responseFormat", "[]"), Arguments.of("responseFormat", "null"), Arguments.of("responseFormat", "{invalid"), Arguments.of("resultHandler", "missing"), Arguments.of("resultHandler", null), Arguments.of("wrapAllParameters", 1), Arguments.of("wrapAllParameters", null), Arguments.of("wrapParameterName", null), Arguments.of("wrapParameterName", 1), Arguments.of("wrapParameterName", "bad-name"), Arguments.of("wrapParameterName", " "), Arguments.of("wrapParameterName", "1name"));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void sqlIsDeliveredUnchangedToTheRegisteredFragmentWithConsistentParameterWrapping(boolean wrap) throws Exception {
        AtomicReference<String> scriptSeen = new AtomicReference<>();
        AtomicReference<Map<String, Object>> parametersSeen = new AtomicReference<>();
        FragmentProcess fragment = (hints, parameters, script) -> {
            scriptSeen.set(script);
            parametersSeen.set(parameters);
            return parameters;
        };
        this.host.addFragment(ApiScriptType.SQL.getTypeName(), () -> fragment);
        this.config.resultHandler("raw").wrapAllParameters(wrap).wrapParameterName("args");
        ApiDefinition definition = this.definition("api", "select :id, '<% unchanged %>'");
        definition.setType(ApiScriptType.SQL);
        DatawayQuery query = this.engine().newQuery(definition, List.of("id"), null);
        Map<String, ?> parameters = Map.of("id", 7, "extra", "not declared");
        Object result = this.execute(query, parameters);
        assertEquals(definition.getScript(), scriptSeen.get());
        if (wrap) {
            assertEquals(Map.of("args", parameters), parametersSeen.get());
            assertEquals(Map.of("args", parameters), result);
        } else {
            assertEquals(Map.of("id", 7), parametersSeen.get());
            assertEquals(Map.of("id", 7), result);
        }
    }

    @Test
    void aFragmentWithNoDeclaredParametersReceivesAnEmptyMap() throws Exception {
        this.host.addFragment(ApiScriptType.SQL.getTypeName(), () -> (hints, parameters, script) -> parameters);
        this.config.resultHandler("raw");
        ApiDefinition definition = this.definition("api", "select 1");
        definition.setType(ApiScriptType.SQL);
        DatawayQuery query = this.engine().newQuery(definition, null, Map.of());
        assertEquals(Map.of(), this.execute(query, Map.of("undeclared", "value")));
    }

    @Test
    void dataqlWrappingUsesThePerQueryWrapperAndIncludesScopeDefaults() throws Exception {
        this.scope = symbol -> Map.of("default", "host", "name", "host-name");
        DatawayQuery query = this.engine().newQuery(this.definition("api", "return ${args};"), List.of("name"), Map.of("resultHandler", "raw", "wrapAllParameters", true, "wrapParameterName", " args "));
        assertEquals(Map.of("default", "host", "name", "request"), this.execute(query, Map.of("name", "request")));
    }
}
