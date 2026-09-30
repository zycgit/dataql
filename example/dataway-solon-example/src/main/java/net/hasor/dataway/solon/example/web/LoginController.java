/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.example.web;
import java.util.Map;
import net.hasor.dataway.authorization.IdentityProvider;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.solon.SolonWebRequest;
import net.hasor.dataway.solon.example.config.auth.JwtTokenService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.core.handle.Context;

@Controller
@Mapping("/session")
public class LoginController {
    @Inject
    private JwtTokenService  tokens;
    @Inject
    private IdentityProvider identityProvider;

    @Post
    @Mapping("/login")
    public Map<String, ?> login(Context context) {
        String username = context.param("username");
        String token = this.tokens.login(username, context.param("password"));
        if (token == null) {
            throw new DatawayException(401, "Invalid credentials");
        }
        this.writeCookie(context, token);
        return Map.of("identity", username);
    }

    @Post
    @Mapping("/logout")
    public Map<String, ?> logout(Context context) {
        this.writeCookie(context, "");
        return Map.of("success", true);
    }

    @Post
    @Mapping("/me")
    public Map<String, ?> currentUser(Context context) {
        var identity = this.identityProvider.resolve(new SolonWebRequest(context));
        return Map.of("identity", identity.identityId(), "authenticated", identity.authenticated(), "attributes", identity.attributes(), "consoleAccess", identity.checkOperation(Operation.LIST), "consoleManage", identity.checkOperation(Operation.SAVE), "documentAccess", identity.checkOperation(Operation.DOCUMENT));
    }

    private void writeCookie(Context context, String token) {
        String cookie = JwtTokenService.COOKIE_NAME + "=" + token + "; Path=/; HttpOnly; SameSite=Strict";
        if (token.isEmpty()) {
            cookie += "; Max-Age=0";
        }
        if ("https".equalsIgnoreCase(context.uri().getScheme())) {
            cookie += "; Secure";
        }
        context.headerAdd("Set-Cookie", cookie);
    }
}
