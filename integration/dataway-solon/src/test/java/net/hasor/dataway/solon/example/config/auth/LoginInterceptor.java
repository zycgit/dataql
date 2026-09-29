/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.example.config.auth;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.service.DatawayException;
import org.noear.solon.core.Props;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Handler;
import org.noear.solon.core.route.RouterInterceptor;
import org.noear.solon.core.route.RouterInterceptorChain;

/** Verifies the host's JWT cookie before MVC execution and restores its identity. */
public final class LoginInterceptor implements RouterInterceptor {
    public static final String          IDENTITY_ATTRIBUTE = "host.identity";
    private final       JwtTokenService tokens;
    private final       String          apiPrefix;
    private final       String          adminPrefix;
    private final       String          uiPrefix;
    private final       String          docsPrefix;

    public LoginInterceptor(JwtTokenService tokens, Props settings) {
        this.tokens = tokens;
        this.apiPrefix = settings.get("dataway.api-prefix", "/api");
        this.adminPrefix = settings.get("dataway.admin-prefix", "/admin/api");
        this.uiPrefix = settings.get("dataway.admin-ui", "/admin");
        this.docsPrefix = settings.get("dataway.docs-prefix", "/docs");
    }

    @Override
    public void doIntercept(Context context, Handler handler, RouterInterceptorChain chain) throws Throwable {
        context.headerSet("X-Host-Interceptor", "applied");
        UserIdentity identity = this.tokens.identity(context.cookie(JwtTokenService.COOKIE_NAME));
        context.attrSet(IDENTITY_ATTRIBUTE, identity);
        String path = context.path();
        boolean publicPath = path.equals("/") || path.equals("/index.html") || path.equals("/app.css") || path.equals("/app.js") || path.equals("/session/login") || path.equals("/example/config");
        if (!publicPath) {
            if (!identity.authenticated()) {
                throw new DatawayException(401, "Login required");
            }
            boolean ui = this.matches(path, this.uiPrefix) && !this.matches(path, this.apiPrefix) && !this.matches(path, this.adminPrefix) && !this.matches(path, this.docsPrefix);
            if (ui && !identity.checkOperation(Operation.LIST)) {
                throw new DatawayException(401, "Console access required");
            }
        }
        chain.doIntercept(context, handler);
    }

    private boolean matches(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }
}
