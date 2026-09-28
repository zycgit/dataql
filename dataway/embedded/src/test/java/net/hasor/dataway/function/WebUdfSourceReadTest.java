/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.function;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.hasor.dataql.domain.HintsSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class WebUdfSourceReadTest {
    private final WebUdfSource source = new WebUdfSource();

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "not a request map" })
    void missingRequestContextHasNoHeadersCookiesOrBody(Object metadata) {
        HintsSet hints = this.hints(metadata);

        assertNull(this.source.header("X-Request", hints));
        assertTrue(this.source.headerArray("X-Request", hints).isEmpty());
        assertTrue(this.source.headerMap(hints).isEmpty());
        assertTrue(this.source.headerArrayMap(hints).isEmpty());
        assertNull(this.source.cookie("session", hints));
        assertTrue(this.source.cookieArray("session", hints).isEmpty());
        assertTrue(this.source.cookieMap(hints).isEmpty());
        assertTrue(this.source.cookieArrayMap(hints).isEmpty());
        assertNull(this.source.jsonBody(hints));
    }

    private HintsSet hints(Object metadata) {
        HintsSet hints = new HintsSet();
        hints.setHint(WebUdfSource.HINT_REQUEST, metadata);
        return hints;
    }

    @Test
    void headersMergeCaseVariantsAndPreserveValueOrderWithoutChangingInput() {
        List<String> original = new ArrayList<>(List.of("first", "second"));
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("X-Trace", original);
        headers.put("x-trace", "third");
        headers.put("X-Empty", List.of());
        HintsSet hints = this.hints(Map.of("headerValues", headers));

        assertEquals("first", this.source.header("X-TRACE", hints));
        assertEquals(List.of("first", "second", "third"), this.source.headerArray("x-TrAcE", hints));
        assertEquals(Map.of("X-Trace", "first"), this.source.headerMap(hints));
        assertEquals(Map.of("X-Trace", List.of("first", "second", "third"), "X-Empty", List.of()), this.source.headerArrayMap(hints));
        assertNull(this.source.header("X-Empty", hints));
        assertEquals(List.of("first", "second"), original);
        assertEquals(3, headers.size());
    }

    @Test
    void multipleHeaderValuesTakePrecedenceOverSingleValueMetadata() {
        HintsSet hints = this.hints(Map.of("headers", Map.of("x-name", "fallback"), "headerValues", Map.of("x-name", List.of("actual", "other"))));
        assertEquals(List.of("actual", "other"), this.source.headerArray("x-name", hints));

        hints.setHint(WebUdfSource.HINT_REQUEST, Map.of("headers", Map.of("x-name", "fallback"), "headerValues", Map.of()));
        assertTrue(this.source.headerArrayMap(hints).isEmpty());
    }

    @Test
    void headersFallBackToTheSingleValueMapWhenMultipleValuesAreUnavailable() {
        HintsSet hints = this.hints(Map.of("headers", Map.of("X-Count", 2), "headerValues", "invalid"));

        assertEquals(2, this.source.header("x-count", hints));
        assertEquals(List.of(2), this.source.headerArray("X-COUNT", hints));
        assertEquals(Map.of("X-Count", 2), this.source.headerMap(hints));
    }

    @Test
    void headersIgnoreNullEntriesAndAcceptTextualKeys() {
        Map<Object, Object> headers = new LinkedHashMap<>();
        headers.put(null, "ignored");
        headers.put("X-Null", null);
        headers.put(42, "answer");
        HintsSet hints = this.hints(Map.of("headers", headers));

        assertEquals(Map.of("42", List.of("answer")), this.source.headerArrayMap(hints));
        assertNull(this.source.header("X-Null", hints));
        assertEquals("answer", this.source.header("42", hints));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "\t", "missing" })
    void blankOrMissingNamesReturnEmptyReadResults(String name) {
        HintsSet hints = this.hints(Map.of("headers", Map.of("X-Name", "value"), "cookies", Map.of("session", "value")));

        assertNull(this.source.header(name, hints));
        assertTrue(this.source.headerArray(name, hints).isEmpty());
        assertNull(this.source.cookie(name, hints));
        assertTrue(this.source.cookieArray(name, hints).isEmpty());
    }

    @Test
    void cookieLookupPrefersAnExactNameAndNeverMergesDifferentSpellings() {
        Map<String, Object> cookies = new LinkedHashMap<>();
        cookies.put("Session", List.of("upper-first", "upper-second"));
        cookies.put("session", "lower");
        cookies.put("empty", List.of());
        HintsSet hints = this.hints(Map.of("cookies", cookies));

        assertEquals("lower", this.source.cookie("session", hints));
        assertEquals(List.of("lower"), this.source.cookieArray("session", hints));
        assertEquals("upper-first", this.source.cookie("SESSION", hints));
        assertEquals(List.of("upper-first", "upper-second"), this.source.cookieArray("SESSION", hints));
        assertNull(this.source.cookie("empty", hints));
        assertEquals(Map.of("Session", "upper-first", "session", "lower"), this.source.cookieMap(hints));
        assertEquals(cookies, this.source.cookieArrayMap(hints));
    }

    @Test
    void scalarCookieFallbackDoesNotDecodeOrNormalizeTheValue() {
        HintsSet hints = this.hints(Map.of("cookies", Map.of("Token", "a%2Bb+c")));

        assertEquals("a%2Bb+c", this.source.cookie("TOKEN", hints));
        assertEquals(List.of("a%2Bb+c"), this.source.cookieArray("TOKEN", hints));
        assertEquals(Map.of("Token", "a%2Bb+c"), this.source.cookieMap(hints));
    }

    @Test
    void requestMetadataKeysIgnoreCaseButExactKeysWin() {
        Map<String, Object> body = Map.of("MixedCaseParameter", "body");
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(null, "ignored");
        metadata.put("HEADERS", Map.of("X-Name", "fallback"));
        metadata.put("headers", Map.of("X-Name", "exact"));
        metadata.put("CoOkIeS", Map.of("session", "cookie"));
        metadata.put("BoDy", body);
        HintsSet hints = this.hints(metadata);

        assertEquals("exact", this.source.header("x-name", hints));
        assertEquals("cookie", this.source.cookie("SESSION", hints));
        assertSame(body, this.source.jsonBody(hints));
        assertEquals(Map.of("MixedCaseParameter", "body"), body);

        metadata.put("body", null);
        assertNull(this.source.jsonBody(hints));
    }

    @Test
    void mixedCaseMultipleHeaderKeyIsRecognized() {
        HintsSet hints = this.hints(Map.of("HeAdErVaLuEs", Map.of("X-Name", List.of("a", "b"))));
        assertEquals(List.of("a", "b"), this.source.headerArray("x-name", hints));
    }

    @Test
    void malformedOrMissingSubMapsProduceEmptyResults() {
        HintsSet hints = this.hints(Map.of("headers", 3, "cookies", List.of("not a map")));
        assertTrue(this.source.headerMap(hints).isEmpty());
        assertTrue(this.source.cookieMap(hints).isEmpty());
        assertNull(this.source.jsonBody(hints));

        hints.setHint(WebUdfSource.HINT_REQUEST, Map.of());
        assertTrue(this.source.headerArrayMap(hints).isEmpty());
        assertTrue(this.source.cookieArrayMap(hints).isEmpty());
    }

    @Test
    void jsonBodyReturnsTheParsedBodyWithoutMergingParametersOrSerializingIt() {
        Map<String, Object> body = Map.of("items", List.of(Map.of("id", 3)));
        HintsSet hints = this.hints(Map.of("body", body, "parameters", Map.of("queryOnly", "hidden")));

        assertSame(body, this.source.jsonBody(hints));
        assertFalse(((Map<?, ?>) this.source.jsonBody(hints)).containsKey("queryOnly"));
    }
}
