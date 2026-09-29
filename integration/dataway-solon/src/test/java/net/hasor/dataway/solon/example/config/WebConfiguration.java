/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.example.config;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.solon.example.config.auth.JwtTokenService;
import net.hasor.dataway.solon.example.config.auth.LoginInterceptor;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.AppContext;

@Configuration
public class WebConfiguration {
    @Inject
    private AppContext      context;
    @Inject
    private JwtTokenService tokens;

    @Bean
    public JwtTokenService jwtTokens(AppContext context) {
        String secret = context.cfg().get("example.jwt.secret", "");
        int expiration = context.cfg().getInt("example.jwt.expiration-seconds", 900);
        return new JwtTokenService(secret, expiration);
    }

    @Init
    public void initialize() {
        var app = this.context.app();
        app.chains().addRouterInterceptor(new HostExceptionHandler(), -10);
        app.chains().addRouterInterceptor(new LoginInterceptor(this.tokens, app.cfg()), 0);
        app.renders().register("@json", (value, request) -> request.outputAsJson(JsonUtils.writeValueAsString(value)));
    }
}