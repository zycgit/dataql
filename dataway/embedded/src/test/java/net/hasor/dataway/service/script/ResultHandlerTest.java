/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.script;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import net.hasor.dataql.domain.BinaryValue;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.ResultInfoUtils;
import net.hasor.dataway.service.config.MemoryResponse;
import net.hasor.dataway.service.config.ServiceTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static net.hasor.dataway.dal.FieldDef.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResultHandlerTest extends ServiceTestSupport {
    @BeforeEach
    void identity() {
        this.config.identityProvider(request -> UserIdentity.consoleAdmin("tester", Map.of()));
    }

    private void publish(String script, String options, String method) {
        Map<FieldDef, String> info = this.info("result", "1", 1);
        info.put(SCRIPT, script);
        info.put(OPTION, options);
        info.put(METHOD, method);
        this.publishRoute(this.release(info, "release", "1", 1));
    }

    @Test
    void csvEscapesFieldsAndBypassesStructure() throws Exception {
        this.config.function("rows", (hints, params) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", "Alice, \"A\"");
            row.put("note", "line1\r\nline2");
            row.put("balance", 12);
            return List.of(row, Map.of("name", "Bob"));
        });
        this.publish("return rows();", "{\"resultHandler\":\"csv\"}", "GET");
        MemoryResponse response = this.handle(this.config.createDataway().getApiHandler(), "GET", "/result");
        assertEquals(200, response.getStatus());
        assertEquals(List.of("text/csv; charset=UTF-8"), response.getHeaders().get("Content-Type"));
        assertEquals("name,note,balance\r\n\"Alice, \"\"A\"\"\",\"line1\r\nline2\",12\r\nBob,,\r\n", response.text());
        assertEquals(List.of("attachment; filename=results.csv"), response.getHeaders().get("Content-Disposition"));
    }

    @Test
    void registeredHandlerAppearsInConsoleAndReceivesOriginalResult() throws Exception {
        AtomicReference<ResultContext> received = new AtomicReference<>();
        this.config.resultHandler("created", context -> {
            received.set(context);
            return ResultInfoUtils.json(201, Map.of("data", context.getValue()));
        });
        this.publish("return {'name': 'Ada'};", "{\"resultHandler\":\"created\"}", "GET");
        Dataway dataway = this.config.createDataway();
        MemoryResponse response = this.handle(dataway.getApiHandler(), "GET", "/result");
        assertEquals(201, response.getStatus());
        assertEquals(Map.of("data", Map.of("name", "Ada")), response.json());
        assertTrue(received.get().isSuccess());
        assertEquals(0, received.get().getCode());
        assertNull(received.get().getError());
        assertNull(received.get().getLocation());
        assertEquals("OK", received.get().getMessage());
        assertTrue(received.get().getLifeCycleTime() >= 0);
        assertTrue(received.get().getExecutionTime() >= 0);
        Map<?, ?> names = (Map<?, ?>) this.handle(dataway.getAdminHandler(), "GET", "/result-handlers").json();
        assertEquals(List.of("structure", "raw", "csv", "text", "created"), names.get("result"));
    }

    @Test
    void selectedHandlerReceivesFailureAndCanSetItsHttpStatus() throws Exception {
        AtomicReference<ResultContext> received = new AtomicReference<>();
        this.config.resultHandler("file", context -> {
            received.set(context);
            return ResultInfoUtils.json(context.getCode(), Map.of("error", context.getValue()));
        });
        this.publish("throw 409, 'failed';", "{\"resultHandler\":\"file\"}", "GET");
        MemoryResponse response = this.handle(this.config.createDataway().getApiHandler(), "GET", "/result");
        assertEquals(409, response.getStatus());
        assertEquals(Map.of("error", "failed"), response.json());
        assertFalse(received.get().isSuccess());
        assertNotNull(received.get().getError());
        assertNotEquals("Unknown", received.get().getLocation());
        assertTrue(received.get().getExecutionTime() >= 0);
    }

    @Test
    void structureAndRawUseTheSameRegistrationAndDefaultSelection() throws Exception {
        List<String> calls = new ArrayList<>();
        this.config.resultHandler("structure", context -> {
            calls.add("structure");
            return ResultInfoUtils.json(200, Map.of("wrapped", context.getValue()));
        });
        this.config.resultHandler("raw", context -> {
            calls.add("raw");
            return ResultInfoUtils.json(200, context.getValue());
        });
        this.publish("return 'value';", "{}", "GET");
        assertEquals(Map.of("wrapped", "value"), this.handle(this.config.createDataway().getApiHandler(), "GET", "/result").json());
        this.config.resultHandler("raw");
        assertEquals("value", this.handle(this.config.createDataway().getApiHandler(), "GET", "/result").json());
        this.publish("return 'override';", "{\"resultHandler\":\"structure\"}", "GET");
        assertEquals(Map.of("wrapped", "override"), this.handle(this.config.createDataway().getApiHandler(), "GET", "/result").json());
        assertEquals(List.of("structure", "raw", "structure"), calls);
    }

    @Test
    void textAndCsvKeepFailuresStructuredUsingTheApiTemplate() throws Exception {
        for (String handler : List.of("csv", "text")) {
            this.publish("throw 422, 'invalid';", "{\"resultHandler\":\"" + handler + "\",\"responseFormat\":\"{\\\"ok\\\":\\\"@resultStatus\\\",\\\"error\\\":\\\"@resultData\\\"}\"}", "GET");
            MemoryResponse response = this.handle(this.config.createDataway().getApiHandler(), "GET", "/result");
            assertEquals(Map.of("ok", false, "error", "invalid"), response.json());
            assertEquals(200, response.getStatus());
        }
    }

    @Test
    void handlerFailuresAndNullResponsesDoNotReenterTheSelectedHandler() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        this.config.resultHandler("broken", context -> {
            calls.incrementAndGet();
            throw new IllegalStateException("handler failed");
        });
        this.publish("throw 409, 'original';", "{\"resultHandler\":\"broken\"}", "GET");
        Map<?, ?> failure = (Map<?, ?>) this.handle(this.config.createDataway().getApiHandler(), "GET", "/result").json();
        assertEquals("handler failed", failure.get("value"));
        assertEquals(1, calls.get());
        this.config.resultHandler("broken", context -> null);
        failure = (Map<?, ?>) this.handle(this.config.createDataway().getApiHandler(), "GET", "/result").json();
        assertEquals("Result handler returned null", failure.get("value"));
    }

    @Test
    void interceptorResponsesKeepTheirStatusWithoutRunningTheHandler() throws Exception {
        ResultInfo response = ResultInfoUtils.json(202, Map.of("queued", true));
        this.config.apiInterceptor((context, chain) -> response);
        this.config.resultHandler("structure", context -> {
            throw new AssertionError("The interceptor already provided its response");
        });
        this.publish("throw 500, 'not executed';", "{}", "GET");
        MemoryResponse actual = this.handle(this.config.createDataway().getApiHandler(), "GET", "/result");
        assertEquals(202, actual.getStatus());
        assertEquals(Map.of("queued", true), actual.json());
    }

    @Test
    void invalidHandlersAreRejectedBeforeExecutionAndHandlerErrorsUseFailureFormat() throws Exception {
        for (String option : List.of("null", "3", "\"missing\"")) {
            this.publish("return 1;", "{\"resultHandler\":" + option + "}", "GET");
            DatawayException error = assertThrows(DatawayException.class, () -> this.handle(this.config.createDataway().getApiHandler(), "GET", "/result"));
            assertEquals(400, error.status());
        }
        this.publish("return 1;", "{\"resultHandler\":\"csv\"}", "GET");
        Map<?, ?> result = (Map<?, ?>) this.handle(this.config.createDataway().getApiHandler(), "GET", "/result").json();
        assertEquals(false, result.get("success"));
    }

    @Test
    void binaryUdfReturnsOriginalBytesThroughLambdaAndSetsHeaders() throws Exception {
        this.config.function("file", (hints, params) -> new BinaryValue(new byte[] { 0, 127, -1 }));
        this.publish("var read = () -> { return file(); }; return read();", "{}", "GET");
        MemoryResponse response = this.handle(this.config.createDataway().getApiHandler(), "GET", "/result");
        assertArrayEquals(new byte[] { 0, 127, -1 }, response.bytes());
        assertEquals(List.of("3"), response.getHeaders().get("Content-Length"));
    }

    @Test
    void webBinaryAndTextHandlerProduceUnquotedUtf8() throws Exception {
        this.publish("import 'net.hasor.dataway.function.WebUdfSource' as web; return web.binary('你好');", "{}", "GET");
        assertArrayEquals("你好".getBytes(StandardCharsets.UTF_8), this.handle(this.config.createDataway().getApiHandler(), "GET", "/result").bytes());
        this.publish("return '你好';", "{\"resultHandler\":\"text\"}", "GET");
        MemoryResponse response = this.handle(this.config.createDataway().getApiHandler(), "GET", "/result");
        assertEquals("你好", response.text());
        assertEquals(List.of("text/plain; charset=UTF-8"), response.getHeaders().get("Content-Type"));
    }

    @ParameterizedTest
    @MethodSource("textResults")
    void textHandlerAcceptsScriptValuesWithoutAddingAnEnvelope(String script, String expected) throws Exception {
        this.publish(script, "{\"resultHandler\":\"text\"}", "GET");
        MemoryResponse response = this.handle(this.config.createDataway().getApiHandler(), "GET", "/result");
        assertEquals(200, response.getStatus());
        assertEquals(List.of("text/plain; charset=UTF-8"), response.getHeaders().get("Content-Type"));
        assertEquals(expected, response.text());
        assertArrayEquals(expected.getBytes(StandardCharsets.UTF_8), response.bytes());
    }

    private static Stream<Arguments> textResults() {
        return Stream.of(Arguments.of("return '';", ""), Arguments.of("return '你好';", "你好"), Arguments.of("return 42;", "42"), Arguments.of("return 1.25;", "1.25"), Arguments.of("return true;", "true"), Arguments.of("return false;", "false"), Arguments.of("return null;", "null"), Arguments.of("return {\"name\": \"Ada\"};", "{\"name\":\"Ada\"}"), Arguments.of("return [{\"id\": 1}, {\"id\": 2}];", "[{\"id\":1},{\"id\":2}]"), Arguments.of("return {\"items\": [1, true, null], \"empty\": null};", "{\"items\":[1,true,null]}"));
    }

    @Test
    void streamsCloseAfterSuccessHeadAndFailedOutput() throws Exception {
        for (String outcome : List.of("GET", "HEAD", "disconnect")) {
            InputStream stream = mock(InputStream.class);
            when(stream.transferTo(any())).thenAnswer(call -> {
                call.getArgument(0, OutputStream.class).write(new byte[] { 1, 2 });
                return 2L;
            });
            this.config.function("file", (hints, params) -> new BinaryValue(stream));
            String method = outcome.equals("HEAD") ? "HEAD" : "GET";
            this.publish("return file();", "{}", method);
            MemoryResponse response = new MemoryResponse();
            if (outcome.equals("disconnect")) {
                response.failWith(new IOException("disconnected"));
                assertThrows(IOException.class, () -> this.config.createDataway().getApiHandler().handle(this.request(method, "/result"), response));
            } else {
                this.config.createDataway().getApiHandler().handle(this.request(method, "/result"), response);
                assertEquals(method.equals("HEAD") ? 0 : 2, response.bytes().length);
            }
            verify(stream).close();
        }
    }
}
