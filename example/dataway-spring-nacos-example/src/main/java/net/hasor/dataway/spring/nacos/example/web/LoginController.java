/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.nacos.example.web;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.hasor.dataway.authorization.IdentityProvider;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.spring.SpringWebRequest;
import net.hasor.dataway.spring.nacos.example.config.auth.JwtTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** MVC endpoints for the host's login and logout operations. */
@RequestMapping("/session")
@RestController
public final class LoginController {
    @Autowired
    private JwtTokenService  tokens;
    @Autowired
    private IdentityProvider identityProvider;

    @PostMapping("/login")
    public Map<String, ?> login(HttpServletRequest request, HttpServletResponse response) {
        String username = request.getParameter("username");
        String token = this.tokens.login(username, request.getParameter("password"));
        if (token == null) {
            throw new DatawayException(401, "Invalid credentials");
        }

        this.writeCookie(request, response, token);
        return Map.of("identity", username);
    }

    @PostMapping("/logout")
    public Map<String, ?> logout(HttpServletRequest request, HttpServletResponse response) {
        this.writeCookie(request, response, "");
        return Map.of("success", true);
    }

    @PostMapping("/me")
    public Map<String, ?> currentUser(HttpServletRequest request) {
        var identity = this.identityProvider.resolve(new SpringWebRequest(request));
        return Map.of(//
                "identity", identity.identityId(), //
                "authenticated", identity.authenticated(), //
                "attributes", identity.attributes(), //
                "consoleAccess", identity.checkOperation(Operation.LIST),//
                "consoleManage", identity.checkOperation(Operation.SAVE), //
                "documentAccess", identity.checkOperation(Operation.DOCUMENT));
    }

    private void writeCookie(HttpServletRequest request, HttpServletResponse response, String token) {
        String path = request.getContextPath().isEmpty() ? "/" : request.getContextPath();
        String cookie = JwtTokenService.COOKIE_NAME + "=" + token + "; Path=" + path + "; HttpOnly; SameSite=Strict";
        if (token.isEmpty()) {
            cookie += "; Max-Age=0";
        }
        if (request.isSecure()) {
            cookie += "; Secure";
        }
        response.addHeader("Set-Cookie", cookie);
    }
}
