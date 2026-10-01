/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.example.config;
import java.util.Map;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.solon.example.config.auth.JwtTokenService;
import net.hasor.dataway.solon.example.config.auth.LoginInterceptor;
import net.hasor.dataway.solon.example.service.UserService;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.AppContext;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Handler;
import org.noear.solon.core.route.RouterInterceptorChain;

@Configuration
public class WebConfiguration {
    @Inject
    private AppContext      context;
    @Inject
    private JwtTokenService tokens;

    @Bean
    public JwtTokenService jwtTokens(AppContext context, UserService users) {
        String secret = context.cfg().get("example.jwt.secret", "");
        int expiration = context.cfg().getInt("example.jwt.expiration-seconds", 900);
        return new JwtTokenService(secret, expiration, users);
    }

    @Init
    public void initialize() {
        var app = this.context.app();
        app.chains().addRouterInterceptor(this::handleException, -10);
        app.chains().addRouterInterceptor(new LoginInterceptor(this.tokens, app.cfg()), 0);
        app.renders().register("@json", (value, request) -> request.outputAsJson(JsonUtils.writeValueAsString(value)));
    }

    private void handleException(Context context, Handler handler, RouterInterceptorChain chain) throws Throwable {
        try {
            chain.doIntercept(context, handler);
        } catch (DatawayException error) {
            context.status(error.status());
            context.setHandled(true);
            context.render(Map.of("message", error.getMessage()));
        }
    }
}
