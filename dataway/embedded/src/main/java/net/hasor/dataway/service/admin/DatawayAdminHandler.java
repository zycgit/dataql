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
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.ref.Tuple;
import net.hasor.dataway.authorization.AuthorizationCheck;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.model.WebRequest;
import net.hasor.dataway.model.WebResponse;
import net.hasor.dataway.service.BeanContainer;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.ResultInfoUtils;
import net.hasor.dataway.service.WebHandler;
import net.hasor.dataway.service.script.DatawayEngine;
import net.hasor.dataway.web.*;

/** Routes, authorizes and intercepts management requests before invoking the selected controller. */
public final class DatawayAdminHandler extends WebHandler {
    private final Map<String, Tuple>     routes;
    private final AuthorizationCheck     authorizationCheck;
    private final List<AdminInterceptor> interceptors;

    public DatawayAdminHandler(BeanContainer beans) {
        super(beans);
        this.authorizationCheck = beans.getBean(AuthorizationCheck.class);
        this.interceptors = beans.getBeans(AdminInterceptor.class);
        AdminService adminService = beans.getBean(AdminService.class);
        DatawayEngine engine = beans.getBean(DatawayEngine.class);

        this.routes = Map.ofEntries(//
                // read
                Map.entry("/api-list", Tuple.of("GET", new ApiListController(adminService))),           //
                Map.entry("/api-info", Tuple.of("GET", new ApiInfoController(adminService))),           //
                Map.entry("/api-detail", Tuple.of("GET", new ApiDetailController(adminService))),       //
                Map.entry("/api-history", Tuple.of("GET", new ApiHistoryListController(adminService))), //
                Map.entry("/get-history", Tuple.of("GET", new ApiHistoryGetController(adminService))),  //
                Map.entry("/get-handlers", Tuple.of("GET", new ResultHandlersController(adminService, engine))), //
                // write
                Map.entry("/save-api", Tuple.of("POST", new SaveApiController(adminService))),          //
                Map.entry("/perform", Tuple.of("POST", new PerformController(adminService, engine))),   //
                Map.entry("/smoke", Tuple.of("POST", new SmokeController(adminService, engine))),       //
                Map.entry("/publish", Tuple.of("POST", new PublishController(adminService))),           //
                Map.entry("/disable", Tuple.of("POST", new DisableController(adminService))),           //
                Map.entry("/delete", Tuple.of("POST", new DeleteController(adminService))));
    }

    @Override
    protected ResultInfo handleRequest(WebRequest request, WebResponse response) throws Exception {
        Tuple route = this.routes.get(request.getPathInfo());
        if (route == null) {
            throw new DatawayException(404, "Not found");
        }

        String method = route.getArg0();
        if (!StringUtils.equalsIgnoreCase(request.getMethod(), method)) {
            throw new DatawayException(405, "Method not allowed");
        }

        AbstractApiController controller = route.getArg1();
        if (!this.authorizationCheck.check(request.getIdentity(), controller.getOperation())) {
            throw new DatawayException(401, "Unauthorized");
        }

        var context = new AdminInterceptorContext(null, controller.getOperation(), request.getIdentity(), Map.of());
        AdminInterceptorChain chain = () -> controller.handle(request, response);
        for (int i = this.interceptors.size() - 1; i >= 0; i--) {
            AdminInterceptor interceptor = this.interceptors.get(i);
            AdminInterceptorChain next = chain;
            chain = () -> interceptor.invoke(context, next);
        }

        return ResultInfoUtils.convertToResultInfo(chain.proceed());
    }
}
