/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.testcase;
import net.hasor.dataway.authorization.IdentityProvider;
import net.hasor.dataway.authorization.RequestIdentityProvider;
import net.hasor.dataway.spring.example.config.HostExceptionAdvice;
import net.hasor.dataway.spring.example.config.WebConfiguration;
import net.hasor.dataway.spring.example.config.auth.LoginInterceptor;
import net.hasor.dataway.spring.example.web.ExampleController;
import net.hasor.dataway.spring.example.web.LoginController;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Reuses the example's web and identity configuration with scenario-specific storage. */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@Import({ WebConfiguration.class, HostExceptionAdvice.class, LoginController.class, ExampleController.class })
public class TestBootConfiguration {
    @Bean
    public IdentityProvider identityProvider() {
        return new RequestIdentityProvider(LoginInterceptor.IDENTITY_ATTRIBUTE);
    }
}
