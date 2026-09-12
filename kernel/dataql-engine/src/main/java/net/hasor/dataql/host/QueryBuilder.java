/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.host;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.hasor.dataql.ConfigOption;
import net.hasor.dataql.compiler.CompilerArguments;
import net.hasor.dataql.compiler.CompilerArguments.CodeLocationEnum;
import net.hasor.dataql.compiler.CompilerHelper;
import net.hasor.dataql.compiler.qil.QIL;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.parser.QueryModel;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;

/**
 * Query 创建配置，承载一次 Query 创建所需的 HostContext、编译参数、共享变量和 hints。
 */
public class QueryBuilder extends HintsSet {
    private final HostContext              hostContext;
    private final CompilerArguments        useArguments = CompilerArguments.DEFAULT.copyAsNew();
    private final Map<String, Supplier<?>> shareVarMap  = new HashMap<>();

    QueryBuilder(HostContext hostContext) {
        this.hostContext = Objects.requireNonNull(hostContext, "hostContext is null.");
    }

    public HostContext getHostContext() {
        return this.hostContext;
    }

    public void configOption(ConfigOption optionKey, Object value) {
        if (optionKey == ConfigOption.CODE_LOCATION && value != null) {
            this.useArguments.setCodeLocation(CodeLocationEnum.valueOf(value.toString()));
        }
    }

    public QueryBuilder addShareVar(String name, Supplier<?> provider) {
        this.shareVarMap.put(name, provider);
        return this;
    }

    public QueryBuilder addShareVar(String name, Class<?> implementation) {
        this.shareVarMap.put(name, () -> this.hostContext.findBean(implementation));
        return this;
    }

    public Query createQuery(String queryString) throws IOException {
        return this.createQuery(this.compilerQuery(queryString));
    }

    public Query createQuery(Reader queryReader) throws IOException {
        return this.createQuery(this.compilerQuery(queryReader));
    }

    public Query createQuery(InputStream queryInput) throws IOException {
        return this.createQuery(this.compilerQuery(queryInput));
    }

    public Query createQuery(InputStream inputStream, Charset charset) throws IOException {
        return this.createQuery(this.compilerQuery(inputStream, charset));
    }

    public Query createQuery(CharStream charStream) {
        return this.createQuery(this.compilerQuery(this.parserQuery(charStream)));
    }

    public Query createQuery(QueryModel queryModel, CompilerArguments compilerArguments) {
        return this.createQuery(this.compilerQuery(queryModel, compilerArguments));
    }

    public Query createQuery(QIL compilerQIL) {
        Query query = new QueryImpl(Objects.requireNonNull(compilerQIL, "compilerQIL is null."), this.hostContext);
        query.putShareVar(this.shareVarMap);
        query.setHints(this);
        return query;
    }

    public QueryModel parserQuery(String queryString) throws IOException {
        return this.parserQuery(CharStreams.fromString(queryString));
    }

    public QueryModel parserQuery(Reader queryReader) throws IOException {
        return this.parserQuery(CharStreams.fromReader(queryReader));
    }

    public QueryModel parserQuery(InputStream queryInput) throws IOException {
        return this.parserQuery(queryInput, StandardCharsets.UTF_8);
    }

    public QueryModel parserQuery(InputStream queryInput, Charset charset) throws IOException {
        return this.parserQuery(CharStreams.fromStream(queryInput, charset));
    }

    public QueryModel parserQuery(CharStream charStream) {
        return CompilerHelper.queryParser(charStream);
    }

    public QIL compilerQuery(String queryString) throws IOException {
        return this.compilerQuery(this.parserQuery(queryString));
    }

    public QIL compilerQuery(Reader queryReader) throws IOException {
        return this.compilerQuery(this.parserQuery(queryReader));
    }

    public QIL compilerQuery(InputStream queryInput) throws IOException {
        return this.compilerQuery(this.parserQuery(queryInput));
    }

    public QIL compilerQuery(InputStream queryInput, Charset charset) throws IOException {
        return this.compilerQuery(this.parserQuery(queryInput, charset));
    }

    public QIL compilerQuery(QueryModel queryModel) {
        return this.compilerQuery(queryModel, this.compilerArguments());
    }

    public QIL compilerQuery(QueryModel queryModel, CompilerArguments compilerArguments) {
        try {
            return CompilerHelper.queryCompiler(queryModel, compilerArguments, this.hostContext.getResourceLoader());
        } catch (IOException e) {
            throw new RuntimeException("compile error", e);
        }
    }

    private CompilerArguments compilerArguments() {
        CompilerArguments arguments = this.useArguments.copyAsNew();
        arguments.getCompilerVar().addAll(this.shareVarMap.keySet());
        return arguments;
    }
}
