/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.testcase;
import java.util.Properties;
import net.hasor.dataway.authorization.RequestIdentityProvider;
import net.hasor.dataway.service.DatawayConfig;
import net.hasor.dataway.spring.example.config.auth.LoginInterceptor;

public final class TestSettings {
    private TestSettings() {
    }

    public static DatawayConfig configuration() {
        return new DatawayConfig().identityProvider(new RequestIdentityProvider(LoginInterceptor.IDENTITY_ATTRIBUTE));
    }

    public static Properties enabled() {
        Properties settings = new Properties();
        settings.setProperty("dataway.api-enabled", "true");
        settings.setProperty("dataway.admin-enabled", "true");
        settings.setProperty("dataway.docs-enabled", "true");
        return settings;
    }
}
