/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.example.config.auth;
import java.util.function.Supplier;
import javax.servlet.http.Cookie;
import net.hasor.cobble.setting.Settings;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.service.DatawayException;
import net.hasor.web.HandlerInterceptor;
import net.hasor.web.Invoker;

/** Verifies the host's JWT cookie before MVC execution and restores its identity. */
public final class LoginInterceptor implements HandlerInterceptor {
    public static final String                    IDENTITY_ATTRIBUTE = "host.identity";
    private final       Supplier<JwtTokenService> tokens;
    private final       String                    apiPrefix;
    private final       String                    adminPrefix;
    private final       String                    uiPrefix;
    private final       String                    docsPrefix;

    public LoginInterceptor(Supplier<JwtTokenService> tokens, Settings settings) {
        this.tokens = tokens;
        this.apiPrefix = settings.getString("dataway.api-prefix", "/api");
        this.adminPrefix = settings.getString("dataway.admin-prefix", "/admin/api");
        this.uiPrefix = settings.getString("dataway.admin-ui", "/admin");
        this.docsPrefix = settings.getString("dataway.docs-prefix", "/docs");
    }

    @Override
    public boolean preHandle(Invoker invoker) {
        var request = invoker.getHttpRequest();
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (JwtTokenService.COOKIE_NAME.equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        UserIdentity identity = this.tokens.get().identity(token);
        request.setAttribute(IDENTITY_ATTRIBUTE, identity);

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.equals("/session/login") || path.equals("/example/config")) {
            return true;
        }
        if (!identity.authenticated()) {
            throw new DatawayException(401, "Login required");
        }

        boolean ui = this.matches(path, this.uiPrefix) && !this.matches(path, this.apiPrefix) && !this.matches(path, this.adminPrefix) && !this.matches(path, this.docsPrefix);
        if (ui && !identity.checkOperation(Operation.LIST)) {
            throw new DatawayException(401, "Console access required");
        }
        return true;
    }

    private boolean matches(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }
}
