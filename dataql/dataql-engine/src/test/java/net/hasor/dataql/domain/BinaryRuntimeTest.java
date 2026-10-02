/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.domain;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.QueryBuilder;
import net.hasor.dataql.host.QueryManager;
import org.junit.Test;
import static org.junit.Assert.*;

public class BinaryRuntimeTest {
    @Test
    public void binarySurvivesUdfLambdaAndNestedCollections() throws Exception {
        BinaryValue content = new BinaryValue(new byte[] { 0, -1, 127 });
        QueryBuilder builder = new QueryManager(new HostConfiguration()).newBuilder();
        builder.addShareVar("file", () -> (Udf) (hints, params) -> content);
        builder.addShareVar("identity", () -> (Udf) (hints, params) -> {
            assertSame(content, params.allParams()[0]);
            return params.allParams()[0];
        });
        for (String script : List.of("return file();", "var read = () -> { return file(); }; return read();", "var read = (value) -> { return value; }; return identity(read(file()));", "var box = {'content': [file()]}; return box.content[0];")) {
            DataModel result = builder.createQuery(script).execute().getData();
            assertSame(script, content, result);
            assertTrue(result.isBinary());
            assertSame(content, result.unwrap());
            try (var input = content.openStream()) {
                assertArrayEquals(new byte[] { 0, -1, 127 }, input.readAllBytes());
            }
        }
    }

    @Test
    public void requestParametersKeepBinaryReferencesAndRawByteArraysKeepListSemantics() throws Exception {
        BinaryValue content = new BinaryValue(new byte[0]);
        QueryBuilder builder = new QueryManager(new HostConfiguration()).newBuilder();
        var result = builder.createQuery("var f = (x) -> { return x; }; return f(${files}[0]);").execute(symbol -> Map.of("files", List.of(content))).getData();
        assertSame(content, result.unwrap());
        assertTrue(DomainHelper.convertTo(new byte[] { 1, 2 }).isList());
        assertEquals(List.of((byte) 1, (byte) 2), DomainHelper.convertTo(new byte[] { 1, 2 }).unwrap());
    }

    @Test
    public void streamIsOpaqueAndSingleUse() throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[] { 3, 4 });
        BinaryValue value = new BinaryValue(input);
        assertSame(value, DomainHelper.convertTo(value));
        assertSame(value, value.asOri());
        assertSame(value, value.unwrap());
        assertEquals(2, input.available());
        assertEquals(-1, value.getSize());
        assertSame(input, value.openStream());
        assertThrows(IOException.class, value::openStream);
        value.close();
        value.close();
        assertThrows(IOException.class, value::openStream);
    }
}
