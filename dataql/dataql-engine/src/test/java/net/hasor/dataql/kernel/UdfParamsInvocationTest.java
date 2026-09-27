/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.hasor.dataql.domain.*;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.Query;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.test.dataql.udfs.ParamsUdfSource;
import org.junit.Test;
import static org.junit.Assert.*;

public class UdfParamsInvocationTest {
    @Test
    public void importedUdfReceivesUnwrappedArguments() throws Exception {
        AtomicReference<Object[]> observed = new AtomicReference<>();
        HostConfiguration host = new HostConfiguration();
        host.addImport("echo", () -> (Udf) (hints, params) -> {
            observed.set(params.allParams());
            assertEquals("test", hints.getHint("tag"));
            return params.allParams();
        });
        Query query = new QueryManager(host).newBuilder().createQuery("""
                hint tag = "test"
                import 'echo' as echo;
                return echo(1, null, {'name': 'DataQL'}, [2, 3]);
                """);

        List<?> result = (List<?>) query.execute().getData().unwrap();

        Object[] values = observed.get();
        assertEquals(4, values.length);
        assertEquals(1, ((Number) values[0]).intValue());
        assertNull(values[1]);
        assertEquals(Map.of("name", "DataQL"), values[2]);
        assertEquals("[2,3]", JsonUtils.writeValueAsString(values[3]));
        assertEquals(4, result.size());
    }

    @Test
    public void emptyArgumentsRemainDistinctFromANullArgument() throws Exception {
        HostConfiguration host = new HostConfiguration();
        host.addImport("echo", () -> (Udf) (hints, params) -> {
            assertNotNull(params);
            return params.allParams();
        });
        Query query = new QueryManager(host).newBuilder().createQuery("""
                import 'echo' as echo;
                return [echo(), echo(null)];
                """);

        assertEquals("[[],[null]]", JsonUtils.writeValueAsString(query.execute().getData().unwrap()));
    }

    @Test
    public void injectedParametersDoNotConsumeScriptArguments() throws Exception {
        HostConfiguration host = new HostConfiguration();
        host.addImport("sample", ParamsUdfSource::new);
        Query query = new QueryManager(host).newBuilder().createQuery("""
                hint tag = "test"
                import 'sample' as sample;
                return [sample.leading('a', 2), sample.middle('a', 2),
                        sample.trailing('a', 2), sample.same('a')];
                """);

        assertEquals(List.of("a:2:test:2", "a:2:test:2", "a:2:test:2", true), query.execute().getData().unwrap());
    }

    @Test
    public void missingAndExtraArgumentsRemainDistinctFromInjectedParameters() throws Exception {
        HostConfiguration host = new HostConfiguration();
        host.addImport("sample", ParamsUdfSource::new);
        Query query = new QueryManager(host).newBuilder().createQuery("""
                hint tag = "test"
                import 'sample' as sample;
                return [sample.middle('a'), sample.middle('a', 2, 3), sample.middle(null, 2)];
                """);

        assertEquals(List.of("a:null:test:1", "a:2:test:3", "null:2:test:2"), query.execute().getData().unwrap());
    }

    @Test
    public void reflectionCallsPreserveTheSuppliedParameterObject() throws Throwable {
        HostConfiguration host = new HostConfiguration();
        Map<String, Udf> functions = new ParamsUdfSource().getUdfResource(host).get();
        Udf capture = functions.get("capture");
        Object[] values = { "value" };
        UdfParams params = () -> values;
        HintsSet hints = new HintsSet();
        hints.setHint("tag", "test");

        UdfParams supplied = (UdfParams) capture.call(hints, params);
        assertSame(params, supplied);
        assertSame(values, supplied.allParams());
        assertEquals("value:null:test:1", functions.get("leading").call(hints, params));
        assertEquals("value:null:test:1", functions.get("middle").call(hints, params));
        assertEquals(true, functions.get("same").call(hints, params));
    }

    @Test
    public void applicationUdfAndModelPreserveParameters() throws Throwable {
        Object[] values = { 1, "DataQL" };
        UdfParams params = () -> values;
        HintsSet hints = new HintsSet();
        Udf udf = (readOnly, supplied) -> {
            assertSame(params, supplied);
            return supplied.allParams();
        };

        assertSame(values, udf.call(hints, params));

        UdfModel model = (UdfModel) DomainHelper.convertTo(udf);
        assertEquals("[1,\"DataQL\"]", JsonUtils.writeValueAsString(model.call(hints, params).unwrap()));
    }

    @Test
    public void collectionCanCallAnImportedUdfWithItsOwnArguments() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        Object binding = new Object();
        HostConfiguration host = new HostConfiguration();
        host.addImport("keep", () -> (Udf) (hints, params) -> {
            calls.incrementAndGet();
            assertEquals(1, params.allParams().length);
            assertEquals("test", hints.getHint("tag"));
            assertSame(binding, hints.getHint("binding"));
            return ((Number) params.allParams()[0]).intValue() > 1;
        });
        Query query = new QueryManager(host).newBuilder().createQuery("""
                hint tag = "test"
                import 'net.hasor.dataql.host.function.basic.CollectionUdfSource' as collect;
                import 'keep' as keep;
                var callback = (value) -> { return keep(value); };
                return [collect.filter([1, 2, 3], keep), collect.filter([1, 2, 3], callback)];
                """);
        query.setHint("binding", binding);

        assertEquals("[[2,3],[2,3]]", JsonUtils.writeValueAsString(query.execute().getData().unwrap()));
        assertEquals(6, calls.get());
    }

    @Test
    public void returnedCallbacksRemainCallableThroughModels() throws Exception {
        HostConfiguration host = new HostConfiguration();
        host.addImport("identity", () -> (Udf) (hints, params) -> {
            assertTrue(params.allParams()[0] instanceof Udf);
            return params.allParams()[0];
        });
        Query query = new QueryManager(host).newBuilder().createQuery("""
                import 'identity' as identity;
                var callback = identity((value) -> { return value + 1; });
                var callbacks = {'nested': callback};
                return [callback(1), callbacks.nested(2)];
                """);

        assertEquals("[2,3]", JsonUtils.writeValueAsString(query.execute().getData().unwrap()));
    }

    @Test
    public void collectionCallbacksReceiveTheirOwnArguments() throws Exception {
        HostConfiguration host = new HostConfiguration();
        host.addImport("sample", ParamsUdfSource::new);
        Query query = new QueryManager(host).newBuilder().createQuery("""
                import 'net.hasor.dataql.host.function.basic.CollectionUdfSource' as collect;
                import 'sample' as sample;
                return {
                    'filter': collect.filter([1, 2, 3], (v) -> { return v > 1; }),
                    'filterMap': collect.filterMap({'a': 1, 'b': 2}, (k) -> { return k == 'b'; }),
                    'sort': collect.listSort([3, 1, 2], sample.compare),
                    'list2map': collect.list2map([{'id': 'a', 'v': 1}], (i, row) -> { return row.id; }, (i, row) -> { return row.v; }),
                    'map2list': collect.map2list({'a': 1}, (k, v) -> { return k + v; }),
                    'map2string': collect.map2string({'a': 1}, ',', (k, v) -> { return k + v; }),
                    'keys': collect.mapKeyReplace({'a': 1}, (k, v) -> { return k + 'x'; }),
                    'values': collect.mapValueReplace({'a': 1}, (k, v) -> { return v + 10; }),
                    'group': collect.groupBy([{'id': 'a', 'v': 1}, {'id': 'a', 'v': 2}], 'id'),
                    'unique': collect.uniqueBy([{'id': 'a', 'v': 1}, {'id': 'a', 'v': 2}], 'id')
                };
                """);

        Map<?, ?> result = JsonUtils.readValue(JsonUtils.writeValueAsString(query.execute().getData().unwrap()), Map.class);

        assertEquals(List.of(2, 3), result.get("filter"));
        assertEquals(Map.of("b", 2), result.get("filterMap"));
        assertEquals(List.of(1, 2, 3), result.get("sort"));
        assertEquals(Map.of("a", 1), result.get("list2map"));
        assertEquals(List.of("a1"), result.get("map2list"));
        assertEquals("a1", result.get("map2string"));
        assertEquals(Map.of("ax", 1), result.get("keys"));
        assertEquals(Map.of("a", 11), result.get("values"));
        assertEquals(Map.of("a", List.of(Map.of("id", "a", "v", 1), Map.of("id", "a", "v", 2))), result.get("group"));
        assertEquals(List.of(Map.of("id", "a", "v", 1)), result.get("unique"));
    }

    @Test
    public void fragmentsKeepTheirNamedArguments() throws Exception {
        Object binding = new Object();
        HostConfiguration host = new HostConfiguration();
        host.addFragment("echo", () -> (hints, params, script) -> {
            assertSame(binding, hints.getHint("binding"));
            return Map.of("params", params, "script", script.trim());
        });
        Query query = new QueryManager(host).newBuilder().createQuery("""
                var echo = @@echo(name)<% body %>;
                return echo('DataQL');
                """);
        query.setHint("binding", binding);

        assertEquals(Map.of("params", Map.of("name", "DataQL"), "script", "body"), query.execute().getData().unwrap());
    }
}
