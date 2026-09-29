/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.example.config;
import java.util.Map;
import net.hasor.cobble.loader.providers.PrefixResourceLoader;
import net.hasor.cobble.setting.Settings;
import net.hasor.config.Bean;
import net.hasor.config.Configuration;
import net.hasor.config.web.Exception;
import net.hasor.config.web.WebMvcConfigurer;
import net.hasor.config.web.render.JsonRenderConfigurer;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.hasor.example.config.auth.JwtTokenService;
import net.hasor.dataway.hasor.example.config.auth.LoginInterceptor;
import net.hasor.dataway.service.DatawayException;
import net.hasor.web.CacheControl;
import net.hasor.web.ExceptionHandler;
import net.hasor.web.Invoker;
import net.hasor.web.WebApiBinder;

/** Configures the application's JWT service, MVC authentication and JSON responses. */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    @Bean
    public JwtTokenService jwtTokens(Settings settings) {
        return new JwtTokenService(settings);
    }

    @Exception(DatawayException.class)
    public ExceptionHandler<DatawayException> errors() {
        return (invoker, error) -> {
            var response = invoker.getHttpResponse();
            response.setStatus(error.status());
            response.setContentType("application/json");
            JsonUtils.writeValue(response.getOutputStream(), Map.of("message", error.getMessage()));
            invoker.setSkipRender();
            return true;
        };
    }

    @Override
    public void addResourceHandlers(WebApiBinder binder) {
        var resources = new PrefixResourceLoader(binder.getResourceLoader(), "example/web");
        binder.addResource("/", resources)//
                .welcomeFile("index.html")//
                .cacheControl(CacheControl.noStore());
    }

    @Override
    public void addInterceptors(WebApiBinder binder) {
        binder.setEncodingCharacter("UTF-8", "UTF-8");
        binder.bindInterceptor(new LoginInterceptor(binder.getProvider(JwtTokenService.class), binder.getSettings()));
    }

    @Override
    public void configureJson(JsonRenderConfigurer configurer) {
        configurer.renderEngine((invoker, writer) -> {
            writer.write(JsonUtils.writeValueAsString(invoker.get(Invoker.RETURN_DATA_KEY)));
        });
    }
}
