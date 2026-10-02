/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.script;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import net.hasor.dataql.DataQueryException;
import net.hasor.dataql.compiler.qil.QIL;
import net.hasor.dataql.host.Query;
import net.hasor.dataql.host.QueryBuilder;
import net.hasor.dataql.kernel.CustomizeScope;
import net.hasor.dataql.kernel.QueryResult;
import net.hasor.dataql.kernel.ThrowRuntimeException;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.model.WebResponse;
import net.hasor.dataway.result.ResultContext;
import net.hasor.dataway.result.ResultHandler;
import net.hasor.dataway.result.structure.StructureResultHandler;
import net.hasor.dataway.service.ResultInfoUtils;
import static net.hasor.dataway.function.WebUdfSource.HINT_REQUEST;
import static net.hasor.dataway.function.WebUdfSource.HINT_RESPONSE;

/** Prepares, intercepts and executes a script, then formats its result. */
public class DatawayQuery {
    private final List<ApiInterceptor> interceptors;
    private final Map<String, Object>  resultOptions;
    private final boolean              wrapAllParameters;
    private final String               wrapParameterName;
    //
    private final ApiDefinition        definition;
    private final QueryBuilder         queryBuilder;
    private final CustomizeScope       scope;
    private final QIL                  compiled;
    private final ResultHandler        resultHandler;

    DatawayQuery(ApiDefinition definition, QIL compiled, List<ApiInterceptor> interceptors, QueryBuilder queryBuilder, CustomizeScope scope, //
            Map<String, Object> resultOptions, boolean wrapAllParameters, String wrapParameterName, ResultHandler resultHandler) {
        this.definition = definition;
        this.compiled = compiled;
        this.interceptors = interceptors;
        this.queryBuilder = queryBuilder;
        this.scope = scope;

        this.resultOptions = resultOptions;
        this.wrapAllParameters = wrapAllParameters;
        this.wrapParameterName = wrapParameterName;
        this.resultHandler = resultHandler;
    }

    /** Prepares parameters, invokes interceptors, and formats results or unhandled execution exceptions. */
    public ResultInfo execute(Operation operation, UserIdentity identity, Map<String, ?> parameters, Map<String, ?> request, WebResponse response) throws Exception {
        Map<String, Object> options = this.resultHandler.prepareOptions(this.resultOptions);
        long started = System.nanoTime();

        // real call
        ApiInterceptorChain chain = c -> {
            Query query = this.queryBuilder.createQuery(this.compiled);
            try {
                query.setHint(HINT_REQUEST, request);
                query.setHint(HINT_RESPONSE, response);
                return query.execute(symbol -> {
                    return switch (symbol) {
                        case "$" -> c.parameters();
                        case "@", "#" -> this.scope.findCustomizeEnvironment(symbol);
                        default -> {
                            throw new IllegalArgumentException("Unsupported parameter access modifier: " + symbol);
                        }
                    };
                });
            } finally {
                query.removeHint(HINT_REQUEST);
                query.removeHint(HINT_RESPONSE);
            }
        };

        // chain call
        for (int i = this.interceptors.size() - 1; i >= 0; i--) {
            ApiInterceptor interceptor = this.interceptors.get(i);
            ApiInterceptorChain next = chain;
            chain = c -> interceptor.invoke(c, next);
        }

        ResultContext resultContext;
        try {
            ApiInterceptorContext context = new ApiInterceptorContext(this.definition, operation, identity, parameters);
            Object result = chain.proceed(this.prepareContext(context));
            if (!(result instanceof QueryResult queryResult)) {
                return ResultInfoUtils.convertToResultInfo(result);
            }

            resultContext = new ResultContext();
            resultContext.setSuccess(true);
            resultContext.setCode(queryResult.getCode());
            resultContext.setMessage("OK");
            resultContext.setValue(queryResult.getData().unwrap());
            resultContext.setExecutionTime(queryResult.executionTime());
        } catch (Exception error) {
            resultContext = this.failureContext(error);
        }

        resultContext.setLifeCycleTime(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        resultContext.setOptions(options);
        return this.processResult(resultContext);
    }

    private ApiInterceptorContext prepareContext(ApiInterceptorContext context) {
        Map<String, Object> values = new LinkedHashMap<>();
        Map<String, ?> defaults = this.scope.findCustomizeEnvironment("$");
        if (defaults != null) {
            values.putAll(defaults);
        }

        values.putAll(context.parameters());
        Map<String, ?> parameters = values;
        if (this.wrapAllParameters) {
            parameters = Map.of(this.wrapParameterName, values);
        }
        return new ApiInterceptorContext(context.definition(), context.operation(), context.identity(), parameters);
    }

    private ResultInfo processResult(ResultContext context) throws Exception {
        try {
            ResultInfo response = this.resultHandler.handle(context);
            if (response == null) {
                throw new IllegalStateException("Result handler returned null");
            }

            return response;
        } catch (Exception error) {
            if (context.getError() != null && error != context.getError()) {
                error.addSuppressed(context.getError());
            }
            ResultContext failure = this.failureContext(error);
            failure.setLifeCycleTime(context.getLifeCycleTime());
            failure.setOptions(this.resultOptions);
            return new StructureResultHandler().handle(failure);
        }
    }

    private ResultContext failureContext(Exception error) {
        if (error instanceof ExecutionException && error.getCause() instanceof Exception cause) {
            error = cause;
        }

        ResultContext context = new ResultContext();
        context.setSuccess(false);
        context.setError(error);
        context.setMessage(error.getLocalizedMessage());
        context.setValue(error.getMessage());
        context.setCode(500);
        context.setExecutionTime(-1);
        context.setLocation("Unknown");

        if (error instanceof ThrowRuntimeException e) {
            context.setValue(e.getResult() == null ? null : e.getResult().unwrap());
            context.setCode(e.getThrowCode());
            context.setExecutionTime(e.getExecutionTime());
        }

        if (error instanceof DataQueryException e) {
            context.setLocation(e.getLocation().toString());
        }
        return context;
    }
}
