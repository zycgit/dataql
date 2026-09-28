/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.hasor.dataway.model.WebCookie;
import net.hasor.dataway.service.config.MemoryRequest;
import net.hasor.dataway.service.config.MemoryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class CookieTest {
    @Test
    void defaultCookiesAreSessionCookiesAtTheRootPath() throws Exception {
        WebCookie cookie = new WebCookie("session", "token");
        assertEquals("/", cookie.getPath());
        assertNull(cookie.getDomain());
        assertNull(cookie.getMaxAge());
        assertNull(cookie.getSameSite());
        assertFalse(cookie.isSecure());
        assertFalse(cookie.isHttpOnly());

        MemoryResponse response = new MemoryResponse();
        response.setCookie(cookie);
        response.write(200, Map.of());
        assertEquals(List.of("session=token; Path=/"), response.getHeaders().get("Set-Cookie"));
    }

    @Test
    void configuredCookiePropertiesReachTheResponseWhenItIsCommitted() throws Exception {
        WebCookie cookie = new WebCookie("old", "old-token");
        cookie.setName("session");
        cookie.setValue("new%20token");
        cookie.setPath("/console");
        cookie.setDomain("example.test");
        cookie.setMaxAge(3600L);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setSameSite("sTrIcT");
        assertEquals("session", cookie.getName());
        assertEquals("new%20token", cookie.getValue());
        assertEquals("/console", cookie.getPath());
        assertEquals("example.test", cookie.getDomain());
        assertEquals(3600L, cookie.getMaxAge());
        assertTrue(cookie.isSecure());
        assertTrue(cookie.isHttpOnly());
        assertEquals("sTrIcT", cookie.getSameSite());

        MemoryResponse response = new MemoryResponse();
        response.setCookie(cookie);
        assertFalse(response.isCommitted());
        assertTrue(response.getHeaders().isEmpty());
        response.write(200, Map.of());
        assertEquals(List.of("session=new%20token; Path=/console; Domain=example.test; Max-Age=3600; Secure; HttpOnly; SameSite=Strict"), response.getHeaders().get("Set-Cookie"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "two words", "tab\tvalue", "\u4e2d\u6587", "one,two", "one;two", "one\"two", "one\\two" })
    void unencodedCookieValuesAreRejectedWithoutQueuingAPartialHeader(String value) throws Exception {
        MemoryResponse response = new MemoryResponse();
        assertThrows(IllegalArgumentException.class, () -> response.setCookie(new WebCookie("invalid", value)));
        assertFalse(response.isStarted());
        response.setCookie(new WebCookie("valid", "encoded%20value"));
        response.write(200, Map.of());
        assertEquals(List.of("valid=encoded%20value; Path=/"), response.getHeaders().get("Set-Cookie"));
    }

    @Test
    void requestCookiesPreserveRepeatedAndEncodedValuesAndIgnoreInvalidOrObsoleteParts() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Cookie", List.of("session=first; invalid; =missing;   =blank; $Path=/; quoted=\"two words\"; encoded=a%3Bb; empty=; payload=a=b=c"));
        headers.put("COOKIE", List.of("session=second; quoted=\"other\""));
        MemoryRequest request = new MemoryRequest();
        request.setHeaderValues(headers);

        assertEquals(Map.of("session", List.of("first", "second"), "quoted", List.of("two words", "other"), "encoded", List.of("a%3Bb"), "empty", List.of(""), "payload", List.of("a=b=c")), request.getCookies());
        assertEquals(0, request.getReads());
    }
}
