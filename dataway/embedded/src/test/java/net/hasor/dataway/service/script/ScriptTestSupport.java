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
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.HostContext;
import net.hasor.dataql.kernel.CustomizeScope;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.service.BeanContainer;
import net.hasor.dataway.service.config.MemoryResponse;
import net.hasor.dataway.service.config.ServiceTestSupport;

abstract class ScriptTestSupport extends ServiceTestSupport {
    protected final HostConfiguration    host         = new HostConfiguration();
    protected final List<ApiInterceptor> interceptors = new ArrayList<>();
    protected       CustomizeScope       scope        = symbol -> Map.of();

    protected DatawayEngine engine() {
        BeanContainer beans = new BeanContainer();
        beans.setBean(HostContext.class, this.host);
        beans.setBean(CustomizeScope.class, this.scope);
        for (ApiInterceptor interceptor : this.interceptors) {
            beans.addBean(ApiInterceptor.class, interceptor);
        }
        DatawayEngine engine = new DatawayEngine(beans, List.of(builder -> builder.addShareVar("customized", () -> "query")));
        engine.setResultHandlers(this.config.getResultHandlers());
        engine.setResultHandler(this.config.getDefaultResultHandler());
        engine.setWrapAllParameters(this.config.isWrapAllParameters());
        engine.setWrapParameterName(this.config.getWrapParameterName());
        return engine;
    }

    protected Object execute(DatawayQuery query, Map<String, ?> parameters) throws Exception {
        return query.execute(Operation.INVOKE, null, ApiCallSource.PROGRAMMATIC, parameters, Map.of(), new MemoryResponse()).getData();
    }
}
