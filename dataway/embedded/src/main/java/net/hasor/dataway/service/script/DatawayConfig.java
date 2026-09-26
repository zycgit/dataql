/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.script;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.hasor.cobble.loader.ResourceLoader;
import net.hasor.dataql.domain.Udf;
import net.hasor.dataql.domain.UdfSource;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.QueryBuilder;
import net.hasor.dataql.kernel.CustomizeScope;
import net.hasor.dataql.kernel.Finder;
import net.hasor.dataql.kernel.FragmentProcess;

/** Configures host lookups, loaders and script extensions for a Dataway runtime. */
public class DatawayConfig {

    private       Finder                            finder;
    private       ResourceLoader                    resourceLoader;
    private       ClassLoader                       classLoader;
    private       CustomizeScope                    customizeScope;
    private       boolean                           resultStructure   = true;
    private       boolean                           wrapAllParameters = false;
    private       String                            wrapParameterName = "root";
    private       String                            responseFormat    = """
            {
                "success"      : "@resultStatus",
                "message"      : "@resultMessage",
                "location"     : "@blockLocation",
                "code"         : "@resultCode",
                "lifeCycleTime": "@timeLifeCycle",
                "executionTime": "@timeExecution",
                "value"        : "@resultData"
            }
            """;
    private final List<Consumer<HostConfiguration>> hostCustomizers   = new ArrayList<>();
    private final List<Consumer<QueryBuilder>>      queryCustomizers  = new ArrayList<>();
    private final List<ApiInterceptor>              interceptors      = new ArrayList<>();

    public DatawayConfig finder(Finder finder) {
        this.finder = finder;
        return this;
    }

    public DatawayConfig resourceLoader(ResourceLoader loader) {
        this.resourceLoader = loader;
        return this;
    }

    public DatawayConfig classLoader(ClassLoader loader) {
        this.classLoader = loader;
        return this;
    }

    public DatawayConfig customizeScope(CustomizeScope scope) {
        this.customizeScope = scope;
        return this;
    }

    public DatawayConfig resultStructure(boolean resultStructure) {
        this.resultStructure = resultStructure;
        return this;
    }

    public DatawayConfig responseFormat(String responseFormat) {
        this.responseFormat = responseFormat;
        return this;
    }

    public DatawayConfig wrapAllParameters(boolean wrapAllParameters) {
        this.wrapAllParameters = wrapAllParameters;
        return this;
    }

    public DatawayConfig wrapParameterName(String wrapParameterName) {
        this.wrapParameterName = wrapParameterName;
        return this;
    }

    public DatawayConfig configureHost(Consumer<HostConfiguration> c) {
        this.hostCustomizers.add(c);
        return this;
    }

    public DatawayConfig configureQuery(Consumer<QueryBuilder> c) {
        this.queryCustomizers.add(c);
        return this;
    }

    public DatawayConfig interceptor(ApiInterceptor interceptor) {
        this.interceptors.add(interceptor);
        return this;
    }

    public DatawayConfig function(String name, Udf function) {
        return this.configureQuery(b -> b.addShareVar(name, () -> function));
    }

    public DatawayConfig library(String namespace, Map<String, Udf> functions) {
        Map<String, Udf> copy = Map.copyOf(functions);
        return this.importSource(namespace, () -> (UdfSource) f -> () -> copy);
    }

    public DatawayConfig importSource(String name, Supplier<?> provider) {
        return this.configureHost(h -> h.addImport(name, provider));
    }

    public DatawayConfig fragment(String name, Supplier<? extends FragmentProcess> provider) {
        return this.configureHost(h -> h.addFragment(name, provider));
    }

    //

    /** Creates a Dataway engine with its own configured host. */
    public DatawayEngine createEngine() {
        Finder finder = this.finder;
        if (finder != null && (this.resourceLoader != null || this.classLoader != null)) {
            finder = new DatawayFinder(finder, this.resourceLoader, this.classLoader);
        }

        HostConfiguration host = finder == null ? new HostConfiguration(this.resourceLoader, this.classLoader) : new HostConfiguration(finder);
        this.hostCustomizers.forEach(c -> c.accept(host));
        CustomizeScope scope = this.customizeScope;
        if (scope == null) {
            scope = symbol -> Map.of();
        }
        DatawayEngine engine = new DatawayEngine(host, scope, this.interceptors, this.queryCustomizers);
        engine.setResponseFormat(this.responseFormat);
        engine.setResultStructure(this.resultStructure);
        engine.setWrapAllParameters(this.wrapAllParameters);
        engine.setWrapParameterName(this.wrapParameterName);
        return engine;
    }
}
