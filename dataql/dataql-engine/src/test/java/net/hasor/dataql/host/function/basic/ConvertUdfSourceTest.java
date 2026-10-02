/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.host.function.basic;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.hasor.dataql.domain.BinaryModel;
import net.hasor.dataql.domain.BinaryValue;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.QueryManager;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConvertUdfSourceTest {
    @Test
    public void textToByteUsesUtf8WithoutACharsetOrWebContext() throws Exception {
        DataModel result = this.execute("return convert.textToByte('你好🙂');");
        assertTrue(result.isBinary());
        assertSame(result, result.unwrap());
        assertArrayEquals("你好🙂".getBytes(StandardCharsets.UTF_8), this.read((BinaryModel) result));
    }

    @Test
    public void textAndHexConversionsRoundTripThroughUdfAndLambdaCalls() throws Exception {
        Object result = this.execute("""
                var identity = (value) -> { return value; };
                var bytes = identity(convert.textToByte('你好', 'UTF-16LE'));
                return {
                    'text': convert.byteToString(bytes, 'UTF-16LE'),
                    'hex': convert.byteToHex(bytes),
                    'restored': convert.byteToString(convert.hexToByte('E4BDA0E5A5BD')),
                    'existing': convert.byteToString(convert.stringToByte('Hello', 'UTF-8'))
                };
                """).unwrap();
        assertEquals(Map.of("text", "你好", "hex", "604F7D59", "restored", "你好", "existing", "Hello"), result);
    }

    @Test
    public void hexAndExistingStringConversionsReturnBinaryModels() throws Exception {
        DataModel hex = this.execute("return convert.hexToByte('007fFf');");
        assertTrue(hex.isBinary());
        assertArrayEquals(new byte[] { 0, 127, -1 }, this.read((BinaryModel) hex));
        DataModel text = this.execute("return convert.stringToByte('A', 'UTF-16BE');");
        assertTrue(text.isBinary());
        assertArrayEquals(new byte[] { 0, 65 }, this.read((BinaryModel) text));
    }

    @Test
    public void byteListsAndRawArraysStillSupportConversions() throws Exception {
        assertEquals("007FFFFF", this.execute("return convert.byteToHex([0, 127, 255, -1]);").unwrap());
        assertEquals("AB", this.execute("return convert.byteToString([65, 66]);").unwrap());
        assertEquals("AB", this.execute("return convert.byteToString([0, 65, 0, 66], 'UTF-16BE');").unwrap());
        assertEquals("00FF", ConvertUdfSource.byteToHex(new byte[] { 0, -1 }));
        assertEquals("AB", ConvertUdfSource.byteToString(new byte[] { 65, 66 }, null));
        assertEquals("00FF", ConvertUdfSource.byteToHex(Arrays.asList((byte) 0, (byte) -1)));
    }

    @Test
    public void nullAndEmptyValuesHaveExplicitResults() throws Exception {
        assertNull(ConvertUdfSource.hexToByte(null));
        assertNull(ConvertUdfSource.byteToHex(null));
        assertNull(ConvertUdfSource.byteToString(null, null));
        assertEquals(0, ConvertUdfSource.textToByte(null, null).getSize());
        assertEquals(0, ConvertUdfSource.stringToByte(null, null).getSize());
        assertEquals(0, ((BinaryModel) this.execute("return convert.textToByte('');")).getSize());
        assertEquals(0, ((BinaryModel) this.execute("return convert.hexToByte('');")).getSize());
        assertEquals("", this.execute("return convert.byteToString(convert.textToByte(''));").unwrap());
        assertEquals("", this.execute("return convert.byteToHex(convert.hexToByte(''));").unwrap());
        assertEquals("", ConvertUdfSource.byteToString(List.of(), null));
    }

    @Test
    public void bytesCanBeReadRepeatedlyWithoutClosingTheBinaryValue() throws Exception {
        BinaryValue value = ConvertUdfSource.textToByte("hello", null);
        assertEquals("68656C6C6F", ConvertUdfSource.byteToHex(value));
        assertEquals("hello", ConvertUdfSource.byteToString(value, null));
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), this.read(value));
    }

    @Test
    public void aStreamIsConsumedOnceAndClosedAfterConversion() throws Exception {
        TrackingInput input = new TrackingInput("hello".getBytes(StandardCharsets.UTF_8));
        BinaryValue value = new BinaryValue(input);
        assertEquals("hello", ConvertUdfSource.byteToString(value, null));
        assertTrue(input.closed);
        assertThrows(IOException.class, () -> ConvertUdfSource.byteToHex(value));
    }

    @Test
    public void aReadFailureStillClosesTheOpenedStream() {
        AtomicBoolean closed = new AtomicBoolean();
        IOException failure = new IOException("read failed");
        BinaryModel value = new BinaryValue(new InputStream() {
            @Override
            public int read() throws IOException {
                throw failure;
            }

            @Override
            public void close() {
                closed.set(true);
            }
        });
        assertSame(failure, assertThrows(IOException.class, () -> ConvertUdfSource.byteToHex(value)));
        assertTrue(closed.get());
    }

    @Test
    public void anInvalidCharsetDoesNotConsumeTheBinaryStream() throws Exception {
        TrackingInput input = new TrackingInput(new byte[] { 65 });
        BinaryValue value = new BinaryValue(input);
        assertThrows(UnsupportedCharsetException.class, () -> ConvertUdfSource.byteToString(value, "missing-charset"));
        assertFalse(input.closed);
        assertEquals("A", ConvertUdfSource.byteToString(value, null));
        assertTrue(input.closed);
        assertThrows(UnsupportedCharsetException.class, () -> ConvertUdfSource.textToByte("A", "missing-charset"));
    }

    @Test
    public void unsupportedValuesAreRejectedInsteadOfBeingStringified() {
        assertThrows(IllegalArgumentException.class, () -> ConvertUdfSource.byteToHex("hello"));
        assertThrows(IllegalArgumentException.class, () -> ConvertUdfSource.byteToString(List.of("hello"), null));
    }

    private DataModel execute(String script) throws Exception {
        return new QueryManager(new HostConfiguration()).newBuilder().createQuery("import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;\n" + script).execute().getData();
    }

    private byte[] read(BinaryModel value) throws IOException {
        try (InputStream input = value.openStream()) {
            return input.readAllBytes();
        }
    }

    private static class TrackingInput extends ByteArrayInputStream {
        private boolean closed;

        private TrackingInput(byte[] content) {
            super(content);
        }

        @Override
        public void close() throws IOException {
            this.closed = true;
            super.close();
        }
    }
}
