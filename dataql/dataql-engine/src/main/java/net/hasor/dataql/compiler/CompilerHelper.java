/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.compiler;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.hasor.cobble.loader.ResourceLoader;
import net.hasor.dataql.compiler.qil.CompilerContext;
import net.hasor.dataql.compiler.qil.InstQueue;
import net.hasor.dataql.compiler.qil.Instruction;
import net.hasor.dataql.compiler.qil.QIL;
import net.hasor.dataql.parser.DataQLLexer;
import net.hasor.dataql.parser.DataQLParser;
import net.hasor.dataql.parser.DataQLParserVisitor;
import net.hasor.dataql.parser.DefaultDataQLVisitor;
import net.hasor.dataql.parser.QueryModel;
import net.hasor.dataql.parser.QueryParseException;
import net.hasor.dataql.parser.ThrowingErrorListener;
import net.hasor.dataql.parser.ast.inst.RootBlockSet;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

/**
 * 编译期工具方法。
 */
public class CompilerHelper {
    public static QueryModel queryParser(String queryString) throws IOException {
        return queryParser(new StringReader(queryString));
    }

    public static QueryModel queryParser(Reader queryReader) throws IOException {
        return queryParser(CharStreams.fromReader(queryReader));
    }

    public static QueryModel queryParser(InputStream queryInput) throws IOException {
        return queryParser(queryInput, StandardCharsets.UTF_8);
    }

    public static QueryModel queryParser(InputStream inputStream, Charset charset) throws IOException {
        return queryParser(CharStreams.fromStream(Objects.requireNonNull(inputStream), charset));
    }

    public static QueryModel queryParser(CharStream charStream) throws QueryParseException {
        DataQLLexer lexer = new DataQLLexer(charStream);
        lexer.removeErrorListeners();
        lexer.addErrorListener(ThrowingErrorListener.INSTANCE);
        DataQLParser qlParser = new DataQLParser(new CommonTokenStream(lexer));
        qlParser.removeErrorListeners();
        qlParser.addErrorListener(ThrowingErrorListener.INSTANCE);
        DataQLParserVisitor visitor = new DefaultDataQLVisitor();
        return (RootBlockSet) visitor.visit(qlParser.rootInstSet());
    }

    public static QIL queryCompiler(String queryString, ResourceLoader resourceLoader) throws IOException {
        return queryCompiler(queryParser(queryString), CompilerArguments.DEFAULT, resourceLoader);
    }

    public static QIL queryCompiler(Reader queryReader, ResourceLoader resourceLoader) throws IOException {
        return queryCompiler(queryParser(queryReader), CompilerArguments.DEFAULT, resourceLoader);
    }

    public static QIL queryCompiler(InputStream queryInput, ResourceLoader resourceLoader) throws IOException {
        return queryCompiler(queryParser(queryInput, StandardCharsets.UTF_8), CompilerArguments.DEFAULT, resourceLoader);
    }

    public static QIL queryCompiler(InputStream queryInput, Charset charset, ResourceLoader resourceLoader) throws IOException {
        return queryCompiler(queryParser(queryInput, charset), CompilerArguments.DEFAULT, resourceLoader);
    }

    public static QIL queryCompiler(QueryModel queryModel, CompilerArguments compilerArguments, ResourceLoader resourceLoader) throws IOException {
        RootBlockSet rootBlockSet;
        if (queryModel instanceof RootBlockSet) {
            rootBlockSet = (RootBlockSet) queryModel;
        } else {
            rootBlockSet = (RootBlockSet) queryParser(CharStreams.fromString(queryModel.toQueryString()));
        }
        compilerArguments = (compilerArguments == null) ? CompilerArguments.DEFAULT : compilerArguments;
        Set<String> compilerVar = compilerArguments.getCompilerVar();
        if (compilerVar == null) {
            compilerVar = Collections.emptySet();
        }

        InstQueue queue = new InstQueue(compilerArguments);
        CompilerContext compilerContext = new CompilerContext(resourceLoader);
        Map<String, Integer> compilerVarMap = new HashMap<>();
        compilerVar.forEach(var -> {
            int localIdx = compilerContext.push(var);
            compilerVarMap.put(var, localIdx);
        });
        compilerContext.findInstCompilerByInst(rootBlockSet).doCompiler(queue);
        Instruction[][] queueSet = queue.buildArrays();
        return new QIL(queueSet, compilerVarMap);
    }
}
