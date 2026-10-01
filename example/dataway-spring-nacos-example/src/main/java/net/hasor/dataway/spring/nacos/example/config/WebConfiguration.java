/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.nacos.example.config;
import java.util.Map;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.spring.nacos.example.config.auth.JwtTokenService;
import net.hasor.dataway.spring.nacos.example.config.auth.LoginInterceptor;
import net.hasor.dataway.spring.nacos.example.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.MappedInterceptor;

@Configuration
@RestControllerAdvice
public class WebConfiguration implements WebMvcConfigurer {
    private final Environment settings;

    public WebConfiguration(Environment settings) {
        this.settings = settings;
    }

    @Bean
    public JwtTokenService jwtTokens(UserService users) {
        String secret = this.settings.getProperty("example.jwt.secret", "");
        int expiration = this.settings.getProperty("example.jwt.expiration-seconds", Integer.class, 900);
        return new JwtTokenService(secret, expiration, users);
    }

    @Bean
    public MappedInterceptor loginInterceptor(JwtTokenService tokens) {
        // A bean applies to custom HandlerMappings as well as Spring's default mapping.
        return new MappedInterceptor(null, new LoginInterceptor(tokens, this.settings));
    }

    @ExceptionHandler(DatawayException.class)
    public ResponseEntity<Map<String, String>> handleDatawayException(DatawayException error) {
        return ResponseEntity.status(error.status()).body(Map.of("message", error.getMessage()));
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger/**")//
                .addResourceLocations("classpath:/web/swagger/")//
                .setCacheControl(CacheControl.noCache());
        registry.addResourceHandler("/index.html", "/app.css", "/app.js")//
                .addResourceLocations("classpath:/web/")//
                .setCacheControl(CacheControl.noStore());
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/")//
                .setViewName("forward:/index.html");
    }
}
