/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.script;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.hasor.dataql.compiler.qil.QIL;
import net.hasor.dataql.host.HostContext;
import net.hasor.dataql.host.QueryBuilder;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.kernel.CustomizeScope;
import net.hasor.dataql.parser.ast.inst.ReturnInst;
import net.hasor.dataql.parser.ast.inst.RootBlockSet;
import net.hasor.dataql.parser.ast.inst.VarInst;
import net.hasor.dataql.parser.ast.token.IntegerToken;
import net.hasor.dataql.parser.ast.token.StringToken;
import net.hasor.dataql.parser.ast.value.EnterRouteVariable;
import net.hasor.dataql.parser.ast.value.EnterRouteVariable.RouteType;
import net.hasor.dataql.parser.ast.value.EnterRouteVariable.SpecialType;
import net.hasor.dataql.parser.ast.value.FragmentVariable;
import net.hasor.dataql.parser.ast.value.FragmentVariable.FragmentParam;
import net.hasor.dataql.parser.ast.value.FunCallRouteVariable;
import net.hasor.dataql.parser.ast.value.NameRouteVariable;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.DatawayException;
import static net.hasor.dataway.model.ResultInfoUtils.JSON;

/** Creates Dataway queries with the configured host, query customizers and defaults. */
public class DatawayEngine {
    private final QueryManager                 queryManager;
    private final CustomizeScope               customizeScope;
    private final List<Consumer<QueryBuilder>> customizers;
    private final List<ApiInterceptor>         interceptors;
    private       String                       responseFormat;
    private       boolean                      resultStructure;
    private       boolean                      wrapAllParameters;
    private       String                       wrapParameterName;

    DatawayEngine(HostContext hostContext, CustomizeScope customizeScope, //
            List<ApiInterceptor> interceptors, List<Consumer<QueryBuilder>> customizers) {
        this.queryManager = new QueryManager(hostContext);
        this.customizeScope = customizeScope;
        this.interceptors = interceptors;
        this.customizers = customizers;
    }

    public HostContext getHostContext() {
        return this.queryManager.getHostContext();
    }

    public void setResponseFormat(String responseFormat) {
        this.responseFormat = responseFormat;
    }

    public void setResultStructure(boolean resultStructure) {
        this.resultStructure = resultStructure;
    }

    public void setWrapAllParameters(boolean wrapAllParameters) {
        this.wrapAllParameters = wrapAllParameters;
    }

    public void setWrapParameterName(String wrapParameterName) {
        this.wrapParameterName = wrapParameterName;
    }

    /** Creates a query from declared parameter names and options overriding engine defaults. */
    public DatawayQuery newQuery(ApiDefinition definition, List<String> parameterNames, Map<String, Object> options) throws IOException {
        if (options == null) {
            options = Map.of();
        }

        Map<?, ?> responseFormat = this.readResponseFormat(options);
        boolean resultStructure = this.readResultStructure(options);
        boolean wrapAllParameters = this.readWrapAllParameters(options);
        String wrapParameterName = this.readWrapParameterName(options);

        QueryBuilder builder = this.queryManager.newBuilder();
        this.customizers.forEach(c -> c.accept(builder));
        ApiScriptType type = definition.getType();
        QIL compiled;
        if (type == ApiScriptType.DATAQL) {
            compiled = builder.compilerQuery(definition.getScript());
        } else {
            if (wrapAllParameters) {
                parameterNames = List.of(wrapParameterName);
            } else if (parameterNames == null) {
                parameterNames = List.of();
            }
            RootBlockSet model = this.atFragment(type, definition.getScript(), parameterNames);
            compiled = builder.compilerQuery(model);
        }

        return new DatawayQuery(definition, compiled, this.interceptors, builder, this.customizeScope, //
                responseFormat, resultStructure, wrapAllParameters, wrapParameterName);
    }

    private RootBlockSet atFragment(ApiScriptType type, String script, List<String> parameterNames) {
        StringToken fragmentName = new StringToken(type.getTypeName());
        FragmentVariable fragment = new FragmentVariable(fragmentName, new StringToken(script), false);
        StringToken functionName = new StringToken("tempCall");
        EnterRouteVariable variables = new EnterRouteVariable(RouteType.Expr, SpecialType.Special_A);
        FunCallRouteVariable call = new FunCallRouteVariable(new NameRouteVariable(variables, functionName));
        EnterRouteVariable parameters = new EnterRouteVariable(RouteType.Params, SpecialType.Special_B);
        for (String name : parameterNames) {
            StringToken parameter = new StringToken(name);
            fragment.getParamList().add(new FragmentParam(parameter, null));
            call.addParam(new NameRouteVariable(parameters, parameter));
        }

        RootBlockSet model = new RootBlockSet();
        model.addInst(new VarInst(functionName, fragment));
        model.addInst(new ReturnInst(new IntegerToken(0), call));
        return model;
    }

    private Map<?, ?> readResponseFormat(Map<String, Object> options) {
        Object responseOption = options.getOrDefault("responseFormat", this.responseFormat);
        if (!(responseOption instanceof String s)) {
            throw new DatawayException(400, "responseFormat must be a JSON object string");
        }

        try {
            Object template = JSON.readValue(s, Object.class);
            if (!(template instanceof Map<?, ?> format)) {
                throw new IllegalArgumentException("Expected a JSON object");
            }
            return format;
        } catch (RuntimeException e) {
            throw new DatawayException(400, "responseFormat must be a JSON object string", e);
        }
    }

    private boolean readResultStructure(Map<String, Object> options) {
        Object structureOption = options.getOrDefault("resultStructure", this.resultStructure);
        if (!(structureOption instanceof Boolean resultStructure)) {
            throw new DatawayException(400, "resultStructure must be a boolean");
        }
        return resultStructure;
    }

    private boolean readWrapAllParameters(Map<String, Object> options) {
        Object wrapOption = options.getOrDefault("wrapAllParameters", this.wrapAllParameters);
        if (!(wrapOption instanceof Boolean wrapAllParameters)) {
            throw new DatawayException(400, "wrapAllParameters must be a boolean");
        }
        return wrapAllParameters;
    }

    private String readWrapParameterName(Map<String, Object> options) {
        Object wrapperOption = options.getOrDefault("wrapParameterName", this.wrapParameterName);
        if (!(wrapperOption instanceof String wrapParameterName)) {
            throw new DatawayException(400, "wrapParameterName must be a string");
        }

        wrapParameterName = wrapParameterName.trim();
        if (!wrapParameterName.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            throw new DatawayException(400, "Invalid parameter wrapper name");
        }
        return wrapParameterName;
    }
}
