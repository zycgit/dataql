/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.function;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.hasor.dataql.domain.DomainHelper;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.domain.UdfParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class WebUdfSourceWriteTest {
    private final WebUdfSource         source = new WebUdfSource();
    private       HintsSet             hints;
    private       RecordingWebResponse response;

    @BeforeEach
    void bindResponse() {
        this.hints = new HintsSet();
        this.response = new RecordingWebResponse();
        this.hints.setHint(WebUdfSource.HINT_RESPONSE, this.response);
    }

    @Test
    void headerOperationsAreBufferedAndAppliedInOrderOverDefaultHeaders() throws Exception {
        assertTrue(this.source.setHeader("X-Trace", "first", this.hints));
        assertTrue(this.source.addHeader("x-trace", "second", this.hints));
        assertTrue(this.source.setHeader("X-Replaced", "new", this.hints));
        assertTrue(this.source.setHeader("Content-Type", "text/plain", this.hints));
        assertTrue(this.response.getHeaders().isEmpty());
        assertFalse(this.response.isStarted());
        assertFalse(this.response.isCommitted());

        this.response.write(200, Map.of("X-Trace", "default", "X-Replaced", "old", "Content-Type", "application/json"));

        assertEquals(List.of("first", "second"), this.response.getHeaders().get("X-Trace"));
        assertEquals(List.of("new"), this.response.getHeaders().get("X-Replaced"));
        assertEquals(List.of("text/plain"), this.response.getHeaders().get("Content-Type"));
    }

    @Test
    void laterSetReplacesAllEarlierAppendedValues() throws Exception {
        this.source.addHeader("X-Trace", "first", this.hints);
        this.source.addHeader("X-Trace", "second", this.hints);
        this.source.setHeader("X-Trace", "replacement", this.hints);
        this.response.write(200, Map.of());

        assertEquals(List.of("replacement"), this.response.getHeaders().get("X-Trace"));
    }

    @Test
    void bulkHeadersUnwrapModelsAndAppendEachListElement() throws Exception {
        Map<Object, Object> replacements = new LinkedHashMap<>();
        replacements.put(DomainHelper.convertTo("X-Count"), DomainHelper.convertTo(2));
        replacements.put("X-Flag", true);
        assertTrue(this.source.setHeaderAll(replacements, this.hints));

        Map<String, Object> additions = new LinkedHashMap<>();
        additions.put("X-Count", DomainHelper.convertTo(List.of(3, 4)));
        additions.put("X-Other", "single");
        additions.put("X-Empty", List.of());
        assertTrue(this.source.addHeaderAll(additions, this.hints));
        this.response.write(200, Map.of());

        assertEquals(List.of("2", "3", "4"), this.response.getHeaders().get("X-Count"));
        assertEquals(List.of("true"), this.response.getHeaders().get("X-Flag"));
        assertEquals(List.of("single"), this.response.getHeaders().get("X-Other"));
        assertFalse(this.response.getHeaders().containsKey("X-Empty"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "X Bad", "X:Bad", "X\r\nInjected" })
    void illegalHeaderNamesAreRejectedBeforeTheyReachTheHost(String name) throws Exception {
        assertThrows(IllegalArgumentException.class, () -> this.source.setHeader(name, "value", this.hints));
        this.response.write(200, Map.of());
        assertTrue(this.response.getHeaders().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = { "safe\r\nInjected: yes", "bad\nvalue", "bad\u0000value", "bad\u007fvalue" })
    void headerControlCharactersCannotInjectAnotherHeader(String value) throws Exception {
        assertThrows(IllegalArgumentException.class, () -> this.source.addHeader("X-Name", value, this.hints));
        this.response.write(200, Map.of());
        assertTrue(this.response.getHeaders().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void responseWritesAreRejectedAfterCommitOrOpeningTheBody(boolean committedByHost) throws Exception {
        if (committedByHost) {
            this.response.commit();
        } else {
            this.response.write(200, Map.of());
        }

        assertThrows(IllegalStateException.class, () -> this.source.setHeader("X-Name", "value", this.hints));
        assertThrows(IllegalStateException.class, () -> this.source.addHeader("X-Name", "value", this.hints));
        assertThrows(IllegalStateException.class, () -> this.source.setCookie(this.params("session", "value"), this.hints));
        assertThrows(IllegalStateException.class, () -> this.source.removeCookie(this.params("session"), this.hints));
    }

    private UdfParams params(Object... values) {
        return () -> values;
    }

    @Test
    void writesRequireTheCurrentExecutionsResponse() {
        this.hints.removeHint(WebUdfSource.HINT_RESPONSE);
        assertThrows(IllegalStateException.class, () -> this.source.setHeader("X-Name", "value", this.hints));
        assertThrows(IllegalStateException.class, () -> this.source.setCookie(this.params("session", "value"), this.hints));

        this.hints.setHint(WebUdfSource.HINT_RESPONSE, "not a response");
        assertThrows(IllegalStateException.class, () -> this.source.addHeader("X-Name", "value", this.hints));
        assertThrows(IllegalStateException.class, () -> this.source.removeCookie(this.params("session"), this.hints));
    }

    @Test
    void settingCookiesAppendsIndependentHeadersAndPreservesTheHostCookie() throws Exception {
        assertTrue(this.source.setCookie(this.params("first", "a%20b"), this.hints));
        assertTrue(this.source.setCookie(this.params("second", ""), this.hints));
        assertTrue(this.response.getHeaders().isEmpty());
        this.response.write(200, Map.of("Set-Cookie", "host=existing"));

        assertEquals(List.of("host=existing", "first=a%20b; Path=/", "second=; Path=/"), this.response.getHeaders().get("Set-Cookie"));
    }

    @Test
    void cookieOptionsIgnoreCaseAndUnwrapDataModels() throws Exception {
        Map<String, Object> attributes = Map.of("PaTh", "/console", "DOMAIN", "example.org", "MAXAGE", 60, "SeCuRe", true, "HTTPONLY", true, "SameSITE", "nOnE");
        assertTrue(this.source.setCookie(this.params(DomainHelper.convertTo("session"), DomainHelper.convertTo("value"), DomainHelper.convertTo(attributes)), this.hints));
        this.response.write(200, Map.of());

        assertEquals(List.of("session=value; Path=/console; Domain=example.org; Max-Age=60; Secure; HttpOnly; SameSite=None"), this.response.getHeaders().get("Set-Cookie"));
    }

    @Test
    void optionalCookieAttributesCanBeOmittedOrExplicitlyCleared() throws Exception {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("path", null);
        attributes.put("domain", null);
        attributes.put("maxAge", null);
        attributes.put("sameSite", null);
        attributes.put("secure", false);
        attributes.put("httpOnly", false);
        this.source.setCookie(this.params("cleared", "value", attributes), this.hints);
        this.source.setCookie(this.params("default", "value", null), this.hints);
        this.response.write(200, Map.of());

        assertEquals(List.of("cleared=value", "default=value; Path=/"), this.response.getHeaders().get("Set-Cookie"));
    }

    @Test
    void removalRetainsScopeAndForcesAnExpiredEmptyCookie() throws Exception {
        assertTrue(this.source.removeCookie(this.params("session", Map.of("pAtH", "/console", "domain", "example.org", "maxAge", 3600)), this.hints));
        assertTrue(this.source.removeCookie(this.params("default"), this.hints));
        this.response.write(200, Map.of());

        assertEquals(List.of("session=; Path=/console; Domain=example.org; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT", "default=; Path=/; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT"), this.response.getHeaders().get("Set-Cookie"));
    }

    @ParameterizedTest
    @MethodSource("invalidCookieArguments")
    void cookieArityAndAttributeShapeAreValidated(boolean remove, Object[] arguments) throws Exception {
        if (remove) {
            assertThrows(IllegalArgumentException.class, () -> this.source.removeCookie(() -> arguments, this.hints));
        } else {
            assertThrows(IllegalArgumentException.class, () -> this.source.setCookie(() -> arguments, this.hints));
        }
        this.response.write(200, Map.of());
        assertTrue(this.response.getHeaders().isEmpty());
    }

    private static Stream<Arguments> invalidCookieArguments() {
        return Stream.of(Arguments.of(false, new Object[] {}), Arguments.of(false, new Object[] { "name" }), Arguments.of(false, new Object[] { "name", "value", Map.of(), "extra" }), Arguments.of(false, new Object[] { "name", "value", "not a map" }), Arguments.of(true, new Object[] {}), Arguments.of(true, new Object[] { "name", Map.of(), "extra" }), Arguments.of(true, new Object[] { "name", "not a map" }));
    }

    @ParameterizedTest
    @MethodSource("invalidCookieAttributes")
    void invalidCookieAttributesAreRejectedWithoutWriting(Map<String, Object> attributes) throws Exception {
        assertThrows(IllegalArgumentException.class, () -> this.source.setCookie(this.params("session", "value", attributes), this.hints));
        this.response.write(200, Map.of());
        assertTrue(this.response.getHeaders().isEmpty());
    }

    private static Stream<Map<String, Object>> invalidCookieAttributes() {
        return Stream.of(Map.of("unknown", true), Map.of("path", "/\r\nInjected: yes"), Map.of("domain", "a; Secure"), Map.of("sameSite", "invalid"), Map.of("sameSite", "None"), Map.of("maxAge", "not a number"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "a b", "a;b", "a,b", "a\"b", "a\\b", "a\r\nb" })
    void unencodedCookieValuesAreRejected(String value) throws Exception {
        assertThrows(IllegalArgumentException.class, () -> this.source.setCookie(this.params("session", value), this.hints));
        this.response.write(200, Map.of());
        assertTrue(this.response.getHeaders().isEmpty());
    }
}
