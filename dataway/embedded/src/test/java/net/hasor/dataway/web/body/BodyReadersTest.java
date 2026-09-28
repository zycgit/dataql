/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web.body;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.hasor.dataway.model.WebRequest;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.config.MemoryRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BodyReadersTest {
    @ParameterizedTest
    @ValueSource(strings = { "UTF-8", "UTF-16LE", "GB18030" })
    void jsonUsesTheDeclaredCharsetAndCachesTheParsedObject(String encoding) throws Exception {
        Charset charset = Charset.forName(encoding);
        InputStream source = spy(new ByteArrayInputStream("{\"name\":\"中文\",\"nested\":{\"flag\":true},\"items\":[1,2]}".getBytes(charset)));
        WebRequest request = this.request("Application/JSON; CHARSET=\"" + encoding + "\"", source);
        try (request) {
            Map<String, Object> body = request.readBody();
            assertEquals(Map.of("name", "中文", "nested", Map.of("flag", true), "items", List.of(1, 2)), body);
            assertSame(body, request.readBody());
            verify(request).getBody();
        }
        verify(source, never()).close();
    }

    private WebRequest request(String contentType, InputStream source) throws IOException {
        MemoryRequest request = spy(new MemoryRequest());
        request.setMethod("POST");
        request.setHeaders(contentType == null ? Map.of() : Map.of("cOnTeNt-TyPe", contentType));
        doReturn(source).when(request).getBody();
        return request;
    }

    @ParameterizedTest
    @ValueSource(strings = { "application/json", "application/x-www-form-urlencoded", "" })
    void anEmptyBodyProducesAnEmptyMutableMap(String contentType) throws Exception {
        try (WebRequest request = this.request(contentType, InputStream.nullInputStream())) {
            Map<String, Object> body = request.readBody();
            assertTrue(body.isEmpty());
            body.put("added", true);
            assertSame(body, request.readBody());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "null", "[]", "1", "true", "\"text\"", "{broken", "   " })
    void jsonMustContainOneValidObject(String json) throws Exception {
        try (WebRequest request = this.request("application/json", new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
            assertEquals(400, assertThrows(DatawayException.class, request::readBody).status());
        }
    }

    @Test
    void aMissingContentTypeIsAllowedOnlyForAnEmptyBody() throws Exception {
        try (WebRequest request = this.request(null, new ByteArrayInputStream(new byte[] { 1 }))) {
            DatawayException error = assertThrows(DatawayException.class, request::readBody);
            assertEquals(415, error.status());
            assertTrue(error.getMessage().contains("Content-Type is required"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "text/plain", "application/xml", "application/octet-stream", "application/problem+json" })
    void unsupportedMediaTypesAreRejectedBeforeOpeningTheBody(String contentType) throws Exception {
        try (WebRequest request = this.request(contentType, InputStream.nullInputStream())) {
            assertEquals(415, assertThrows(DatawayException.class, request::readBody).status());
            verify(request, never()).getBody();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "not-a-charset", "", "UTF 8", "\"\"" })
    void invalidCharsetsAreRejectedBeforeOpeningTheBody(String charset) throws Exception {
        try (WebRequest request = this.request("application/json; charset=" + charset, InputStream.nullInputStream())) {
            assertEquals(415, assertThrows(DatawayException.class, request::readBody).status());
            verify(request, never()).getBody();
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "multipart/form-data; boundary=upload", "multipart/form-data; boundary=\"a;charset=GBK;b\"", "multipart/form-data; boundary=\"a\\\";charset=GBK;b\"; ignored", "application/json; charsetHint=GBK; flag" })
    void quotedBoundaryParametersDoNotOverrideTheCharset(String header) {
        assertEquals(StandardCharsets.UTF_8, BodyReaders.charset(header, StandardCharsets.UTF_8));
    }

    @Test
    void charsetAfterAQuotedBoundaryAndWithWhitespaceIsRecognized() {
        String header = "multipart/form-data; boundary=\"a;\\\"b\"; CHARSET = \"ISO-8859-1\" ; other=value";
        assertEquals(StandardCharsets.ISO_8859_1, BodyReaders.charset(header, StandardCharsets.UTF_8));
    }

    @Test
    void formDecodingPreservesRepeatedEmptyAndEncodedFields() throws Exception {
        String encoded = "name=%E4%B8%AD%E6%96%87&tag=a&tag=b&tag=c&empty=&flag&=unnamed&&formula=a%2Bb+c%3Dd%26e&same=x=y";
        InputStream source = spy(new ByteArrayInputStream(encoded.getBytes(StandardCharsets.UTF_8)));
        try (WebRequest request = this.request("Application/X-WWW-Form-Urlencoded; charset=UTF-8", source)) {
            assertEquals(Map.of("name", "中文", "tag", List.of("a", "b", "c"), "empty", "", "flag", "", "", "unnamed", "formula", "a+b c=d&e", "same", "x=y"), request.readBody());
        }
        verify(source, never()).close();
    }

    @ParameterizedTest
    @CsvSource({ "ISO-8859-1,caf%E9,café", "GB18030,%D6%D0%CE%C4,中文" })
    void formPercentEscapesUseTheDeclaredCharset(String encoding, String encoded, String expected) throws Exception {
        try (WebRequest request = this.request("application/x-www-form-urlencoded; charset=" + encoding, new ByteArrayInputStream(("name=" + encoded).getBytes(StandardCharsets.US_ASCII)))) {
            assertEquals(Map.of("name", expected), request.readBody());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "name=%", "name=%0", "name=%ZZ", "%GG=value" })
    void malformedFormEscapesAreClientErrors(String form) throws Exception {
        try (WebRequest request = this.request("application/x-www-form-urlencoded", new ByteArrayInputStream(form.getBytes(StandardCharsets.UTF_8)))) {
            DatawayException error = assertThrows(DatawayException.class, request::readBody);
            assertEquals(400, error.status());
            assertInstanceOf(IllegalArgumentException.class, error.getCause());
        }
    }

    @Test
    void alreadyParsedHostFormsNeverReopenAConsumedStream() throws Exception {
        Map<String, Object> parsed = new LinkedHashMap<>(Map.of("tag", List.of("a", "b")));
        try (WebRequest request = this.request("application/x-www-form-urlencoded", InputStream.nullInputStream())) {
            doReturn(parsed).when(request).getFormBody();
            assertSame(parsed, request.readBody());
            assertSame(parsed, request.readBody());
            verify(request).getFormBody();
            verify(request, never()).getBody();
        }
    }

    @Test
    void hostParametersExcludeOnlyMatchingQueryOccurrencesWithoutMutatingTheHost() {
        Map<String, List<String>> parameters = Map.of("tag", List.of("a", "a", "b", "c", "d"), "title", List.of("中文", "body"), "queryOnly", List.of("q"), "flag", List.of(""), "untouched", List.of("one", "two"));
        String query = "tag=a&tag=b&title=%E4%B8%AD%E6%96%87&queryOnly=q&flag&absent=x";
        assertEquals(Map.of("tag", List.of("a", "c", "d"), "title", "body", "untouched", List.of("one", "two")), FormBodyReader.fromParameters(parameters, query));
        assertEquals(List.of("a", "a", "b", "c", "d"), parameters.get("tag"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void hostFormsWithoutAQueryKeepAllValues(String query) {
        assertEquals(Map.of("field", List.of("one", "two", "three")), FormBodyReader.fromParameters(Map.of("field", List.of("one", "two", "three")), query));
    }

    @Test
    void multipartUsesTheHostAdapterAndItsParsedBodyIsCached() throws Exception {
        Map<String, Object> parsed = new LinkedHashMap<>(Map.of("title", "中文", "tag", List.of("one", "two")));
        try (WebRequest request = this.request("multipart/form-data; boundary=\"a;b\"; charset=GB18030", InputStream.nullInputStream())) {
            Charset charset = Charset.forName("GB18030");
            doReturn(parsed).when(request).getMultipartBody(charset);
            assertSame(parsed, request.readBody());
            assertSame(parsed, request.readBody());
            verify(request).getMultipartBody(charset);
            verify(request, never()).getBody();
        }
    }

    @Test
    void multipartRequiresAnAdapterThatProvidesAnUploadParser() throws Exception {
        try (WebRequest request = this.request("multipart/form-data; boundary=upload", InputStream.nullInputStream())) {
            DatawayException error = assertThrows(DatawayException.class, request::readBody);
            assertEquals(415, error.status());
            assertTrue(error.getMessage().contains("request adapter"));
            verify(request, never()).getBody();
        }
    }

    @Test
    void uploadParserFailuresRemainAvailableToTheHostExceptionHandler() throws Exception {
        IOException failure = new IOException("truncated multipart body");
        try (WebRequest request = this.request("multipart/form-data; boundary=upload", InputStream.nullInputStream())) {
            doThrow(failure).when(request).getMultipartBody(StandardCharsets.UTF_8);
            assertSame(failure, assertThrows(IOException.class, request::readBody));
            verify(request, never()).getBody();
        }
    }
}
