/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.admin;
import java.util.List;
import java.util.Map;
import net.hasor.cobble.ref.Tuple;
import net.hasor.dataway.Dataway;
import net.hasor.dataway.authorization.AuthorizationCheck;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.model.ResultInfoUtils;
import net.hasor.dataway.model.WebRequest;
import net.hasor.dataway.model.WebResponse;
import net.hasor.dataway.service.AbstractWebHandler;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.DatawayService;
import net.hasor.dataway.service.HttpSupport;
import net.hasor.dataway.service.Interceptor;
import net.hasor.dataway.service.InterceptorChain;
import net.hasor.dataway.service.InterceptorContext;
import net.hasor.dataway.web.*;

/** Routes, authorizes and intercepts management requests before invoking the selected controller. */
public final class DatawayAdminHandler extends AbstractWebHandler {
    private final Map<String, Tuple> routes;

    public DatawayAdminHandler(Dataway dataway) {
        super(dataway);

        DatawayService service = dataway.getService();
        this.routes = Map.ofEntries(//
                // read
                Map.entry("/api-list", Tuple.of("GET", new ApiListController(service))),     //
                Map.entry("/api-info", Tuple.of("GET", new ApiInfoController(service))),     //
                Map.entry("/api-detail", Tuple.of("GET", new ApiDetailController(service))), //
                Map.entry("/api-history", Tuple.of("GET", new ApiHistoryListController(service))), //
                Map.entry("/get-history", Tuple.of("GET", new ApiHistoryGetController(service))),  //
                // write
                Map.entry("/save-api", Tuple.of("POST", new SaveApiController(service))),    //
                Map.entry("/perform", Tuple.of("POST", new PerformController(service))),     //
                Map.entry("/smoke", Tuple.of("POST", new SmokeController(service))),         //
                Map.entry("/publish", Tuple.of("POST", new PublishController(service))),     //
                Map.entry("/disable", Tuple.of("POST", new DisableController(service))),     //
                Map.entry("/delete", Tuple.of("POST", new DeleteController(service))));
    }

    @Override
    protected ResultInfo handleRequest(WebRequest request, WebResponse response) throws Exception {
        Tuple route = this.routes.get(request.getPathInfo());
        if (route == null) {
            throw new DatawayException(404, "Not found");
        }

        String method = route.getArg0();
        if (!request.getMethod().equals(method)) {
            throw new DatawayException(405, "Method not allowed");
        }

        AbstractApiController controller = route.getArg1();
        AuthorizationCheck check = this.getDataway().getAuthorizationCheck();
        if (!check.check(request.getIdentity(), controller.getOperation())) {
            throw new DatawayException(401, "Unauthorized");
        }

        Map<String, ?> metadata = HttpSupport.metadata(request, Map.of(), Map.of());
        var context = new InterceptorContext(null, null, controller.getOperation(), request.getIdentity(), metadata, response, Map.of());
        List<Interceptor> interceptors = this.getDataway().getActionInterceptors();

        InterceptorChain chain = () -> controller.handle(request, response);
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            Interceptor interceptor = interceptors.get(i);
            InterceptorChain next = chain;
            chain = () -> interceptor.invoke(context, next);
        }

        return ResultInfoUtils.result(chain.proceed());
    }
}
