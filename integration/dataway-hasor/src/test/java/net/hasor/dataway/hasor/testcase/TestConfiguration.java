/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.testcase;
import net.hasor.config.Bean;
import net.hasor.config.Configuration;
import net.hasor.core.ApiBinder;
import net.hasor.core.Module;
import net.hasor.dataway.authorization.IdentityProvider;
import net.hasor.dataway.authorization.RequestIdentityProvider;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.hasor.DatawayModule;
import net.hasor.dataway.hasor.example.config.auth.LoginInterceptor;

/** Applies scenario-specific overrides without involving the example application. */
@Configuration
public class TestConfiguration implements Module {
    private final TestBootstrap bootstrap = TestBootstrap.CURRENT.get();

    @Bean
    public IdentityProvider identityProvider() {
        return new RequestIdentityProvider(LoginInterceptor.IDENTITY_ATTRIBUTE);
    }

    @Override
    public void loadModule(ApiBinder binder) throws Throwable {
        ApiDataAccessLayer storage = this.bootstrap.storage();
        if (storage != null) {
            binder.bindType(ApiDataAccessLayer.class).toInstance(storage);
        }
        Module module = this.bootstrap.module();
        if (module == null) {
            module = new DatawayModule(this.bootstrap.configuration());
        }
        binder.installModule(module);
    }
}
