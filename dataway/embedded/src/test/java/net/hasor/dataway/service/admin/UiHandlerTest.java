/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.admin;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.config.MemoryRequest;
import net.hasor.dataway.service.config.MemoryResponse;
import net.hasor.dataway.service.config.ServiceTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class UiHandlerTest extends ServiceTestSupport {
    @ParameterizedTest
    @CsvSource(value = { "/admin,/admin/api,/api,api/,../api/", "/console,/console/manage,/console/invoke,manage/,invoke/", "/tools/console,/ops/manage,/open/v2,../../ops/manage/,../../open/v2/", "/tools/console,/tools,/tools/console,../,./", "/,/manage,/invoke,manage/,invoke/", "/console,/manage,OFF,../manage/,OFF" }, nullValues = "OFF")
    void initializerUsesConfiguredAddressesRelativeToTheConsole(String ui, String admin, String api, String expectedAdmin, String expectedApi) throws Exception {
        DatawayUiHandler handler = (DatawayUiHandler) this.config.createDataway().getAdminUiHandler();
        handler.configureAddresses(ui, admin, api);
        MemoryResponse response = this.handle(handler, "GET", "/initializer.js");
        assertEquals(200, response.getStatus());
        assertEquals(List.of("text/javascript; charset=utf-8"), response.getHeaders().get("Content-Type"));
        assertEquals(List.of("no-cache"), response.getHeaders().get("Cache-Control"));
        String script = response.text();
        int start = script.indexOf("window.DatawayUI(") + "window.DatawayUI(".length();
        Map<?, ?> options = JsonUtils.readValue(script.substring(start, script.indexOf(");", start)), Map.class);
        assertEquals(expectedAdmin, options.get("adminApi"));
        assertEquals(expectedApi, options.get("api"));
        if (expectedApi == null) {
            assertFalse(options.containsKey("api"));
        }
        MemoryResponse head = this.handle(handler, "HEAD", "/initializer.js");
        assertEquals(response.getHeaders(), head.getHeaders());
        assertEquals(0, head.bytes().length);
    }

    @ParameterizedTest
    @CsvSource({ "html,text/html; charset=utf-8", "js,text/javascript; charset=utf-8", "css,text/css; charset=utf-8", "json,application/json; charset=utf-8", "svg,image/svg+xml", "ico,image/x-icon", "ttf,font/ttf", "woff,font/woff", "woff2,font/woff2", "bin,application/octet-stream" })
    void assetsKeepTheirMimeTypeAndSecurityHeaders(String extension, String type) throws Exception {
        String path = "/service-test/asset." + extension;
        MemoryResponse response = this.handle(this.config.createDataway().getAdminUiHandler(), "GET", path);
        assertEquals(200, response.getStatus());
        try (InputStream resource = UiHandlerTest.class.getResourceAsStream("/META-INF/dataway-ui" + path)) {
            assertNotNull(resource);
            assertArrayEquals(resource.readAllBytes(), response.bytes());
        }
        assertEquals(List.of(type), response.getHeaders().get("Content-Type"));
        assertEquals(List.of("nosniff"), response.getHeaders().get("X-Content-Type-Options"));
        assertEquals(List.of("no-cache"), response.getHeaders().get("Cache-Control"));
        assertTrue(response.getHeaders().get("Content-Security-Policy").get(0).contains("frame-ancestors 'none'"));
    }

    @Test
    void mountRedirectUsesARelativeLocationAndIndexAndHeadDoNotRunOperationInterceptors() throws Exception {
        this.config.authorizationCheck((identity, operation) -> {
            fail("Static assets do not authorize an API operation");
            return false;
        }).adminInterceptor((context, chain) -> {
            fail("Static assets do not run management interceptors");
            return null;
        });
        Dataway dataway = this.config.createDataway();
        MemoryRequest request = this.request("GET", "");
        request.setPath("/proxy/console");
        MemoryResponse redirect = new MemoryResponse();
        dataway.getAdminUiHandler().handle(request, redirect);
        assertEquals(308, redirect.getStatus());
        assertEquals(List.of("console/"), redirect.getHeaders().get("Location"));
        assertArrayEquals(new byte[0], redirect.bytes());
        assertFalse(this.handle(dataway.getAdminUiHandler(), "GET", "/").text().isEmpty());
        MemoryResponse head = this.handle(dataway.getAdminUiHandler(), "head", "/service-test/asset.js");
        assertEquals(200, head.getStatus());
        assertArrayEquals(new byte[0], head.bytes());
    }

    @ParameterizedTest
    @ValueSource(strings = { "relative", "/../secret", "/path\\secret", "/%2e%2e/secret", "/missing-asset.txt" })
    void missingAndUnsafeResourcePathsNeverReadOutsideTheUi(String path) {
        assertEquals(404, assertThrows(DatawayException.class, () -> this.handle(this.config.createDataway().getAdminUiHandler(), "GET", path)).status());
    }

    @Test
    void resourceEntryRejectsWrites() {
        assertEquals(405, assertThrows(DatawayException.class, () -> this.handle(this.config.createDataway().getAdminUiHandler(), "POST", "/")).status());
    }
}
