/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.script;
import java.io.InputStream;
import java.util.ArrayList;
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
import static net.hasor.dataway.function.WebUdfSource.HINT_REQUEST;
import static net.hasor.dataway.function.WebUdfSource.HINT_RESPONSE;

/** Prepares, intercepts and executes a script, then formats its result. */
public class DatawayQuery {
    private final List<ApiInterceptor> interceptors;
    private final Map<?, ?>            responseFormat;
    private final boolean              resultStructure;
    private final boolean              wrapAllParameters;
    private final String               wrapParameterName;
    //
    private final ApiDefinition        definition;
    private final QueryBuilder         queryBuilder;
    private final CustomizeScope       scope;
    private final QIL                  compiled;

    DatawayQuery(ApiDefinition definition, QIL compiled, List<ApiInterceptor> interceptors, QueryBuilder queryBuilder, CustomizeScope scope, //
            Map<?, ?> responseFormat, boolean resultStructure, boolean wrapAllParameters, String wrapParameterName) {
        this.definition = definition;
        this.compiled = compiled;
        this.interceptors = interceptors;
        this.queryBuilder = queryBuilder;
        this.scope = scope;

        this.responseFormat = responseFormat;
        this.resultStructure = resultStructure;
        this.wrapAllParameters = wrapAllParameters;
        this.wrapParameterName = wrapParameterName;
    }

    /** Prepares parameters, invokes interceptors, and formats results or unhandled execution exceptions. */
    public Object execute(Operation operation, UserIdentity identity, Map<String, ?> parameters, Map<String, ?> request, WebResponse response) throws Exception {
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

        // do call
        try {
            ApiInterceptorContext context = new ApiInterceptorContext(this.definition, operation, identity, parameters);
            ApiInterceptorContext invocation = this.prepareContext(context);
            Object result = chain.proceed(invocation);
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            return this.processResult(result, elapsed);
        } catch (Exception e) {
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            return this.processException(e, elapsed);
        }
    }

    //

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

    private Object processResult(Object result, long elapsed) {
        if (!(result instanceof QueryResult r)) {
            return result;
        }

        Object value = r.getData().unwrap();
        if (!this.resultStructure || value instanceof ResultInfo || value instanceof byte[] || value instanceof InputStream) {
            return value;
        }

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("success", true);
        fields.put("message", "OK");
        fields.put("code", r.getCode());
        fields.put("location", null);
        fields.put("lifeCycleTime", elapsed);
        fields.put("executionTime", r.executionTime());
        fields.put("value", value);
        return this.formatResult(fields);
    }

    private Object processException(Exception error, long elapsed) {
        if (error instanceof ExecutionException && error.getCause() instanceof Exception cause) {
            error = cause;
        }

        Object value = error.getMessage();
        int code = 500;
        long executionTime = -1;
        if (error instanceof ThrowRuntimeException e) {
            value = e.getResult() == null ? null : e.getResult().unwrap();
            code = e.getThrowCode();
            executionTime = e.getExecutionTime();
        }
        if (!this.resultStructure && value != null) {
            return value;
        }

        String location = "Unknown";
        if (error instanceof DataQueryException e) {
            location = e.getLocation().toString();
        }

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("success", false);
        fields.put("message", error.getLocalizedMessage());
        fields.put("code", code);
        fields.put("location", location);
        fields.put("lifeCycleTime", elapsed);
        fields.put("executionTime", executionTime);
        fields.put("value", value);
        return this.formatResult(fields);
    }

    private Object formatResult(Map<String, Object> fields) {
        Map<String, Object> formatted = new LinkedHashMap<>();
        this.responseFormat.forEach((key, placeholder) -> {
            String field = switch (String.valueOf(placeholder)) {
                case "@resultStatus" -> "success";
                case "@resultMessage" -> "message";
                case "@resultCode" -> "code";
                case "@blockLocation", "@codeLocation" -> "location";
                case "@timeLifeCycle" -> "lifeCycleTime";
                case "@timeExecution" -> "executionTime";
                case "@resultData" -> "value";
                default -> null;
            };
            formatted.put(key.toString(), field == null ? this.copyTemplateValue(placeholder) : fields.get(field));
        });
        return formatted;
    }

    /** Keeps literal objects and arrays independent between executions. */
    private Object copyTemplateValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, item) -> copy.put(key.toString(), this.copyTemplateValue(item)));
            return copy;
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            for (Object item : list) {
                copy.add(this.copyTemplateValue(item));
            }
            return copy;
        }
        return value;
    }
}
