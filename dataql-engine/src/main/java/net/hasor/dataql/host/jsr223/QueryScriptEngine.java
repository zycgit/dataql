/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.dataql.host.jsr223;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Objects;
import javax.script.*;
import net.hasor.cobble.loader.ResourceLoader;
import net.hasor.cobble.loader.providers.ClassPathResourceLoader;
import net.hasor.dataql.compiler.CompilerArguments;
import net.hasor.dataql.compiler.CompilerHelper;
import net.hasor.dataql.compiler.qil.QIL;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.kernel.Finder;
import net.hasor.dataql.parser.QueryModel;

/**
 * JSR223 引擎机制的实现。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-10-19
 */
public class QueryScriptEngine extends AbstractScriptEngine implements ScriptEngine, Compilable, Hints {
    private final HintsSet                  optionSet         = new HintsSet();
    private final QueryScriptEngineFactory  engineFactory;
    private       Finder                    parentFinder;
    private       ResourceLoader            resourceLoader    = ClassPathResourceLoader.INSTANCE;
    private       HostConfiguration         hostConfiguration = new HostConfiguration();
    private       QueryManager              queryManager      = new QueryManager(this.hostConfiguration);

    QueryScriptEngine(QueryScriptEngineFactory engineFactory) {
        this.engineFactory = engineFactory;
    }

    // -------------------------------------------------------------------------------------------- Option
    @Override
    public String[] getHints() {
        return this.optionSet.getHints();
    }

    @Override
    public Object getHint(String optionKey) {
        return this.optionSet.getHint(optionKey);
    }

    @Override
    public void removeHint(String optionKey) {
        this.optionSet.removeHint(optionKey);
    }

    @Override
    public void setHint(String hintName, String value) {
        this.optionSet.setHint(hintName, value);
    }

    @Override
    public void setHint(String hintName, Number value) {
        this.optionSet.setHint(hintName, value);
    }

    @Override
    public void setHint(String hintName, boolean value) {
        this.optionSet.setHint(hintName, value);
    }

    public Finder getFinder() {
        return this.hostConfiguration;
    }

    public void setFinder(Finder finder) {
        this.parentFinder = Objects.requireNonNull(finder, "finder is null.");
        this.hostConfiguration = new HostConfiguration(this.parentFinder);
        this.hostConfiguration.setResourceLoader(this.resourceLoader);
        this.queryManager = new QueryManager(this.hostConfiguration);
    }

    public ResourceLoader getResourceLoader() {
        return this.resourceLoader;
    }

    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = Objects.requireNonNull(resourceLoader, "resourceLoader is null.");
        this.hostConfiguration.setResourceLoader(this.resourceLoader);
    }

    QueryManager getQueryManager() {
        return this.queryManager;
    }
    // -------------------------------------------------------------------------------------------- ScriptEngine

    @Override
    public ScriptEngineFactory getFactory() {
        return this.engineFactory;
    }

    @Override
    public Bindings createBindings() {
        return new SimpleBindings();
    }
    // -------------------------------------------------------------------------------------------- ScriptEngine

    @Override
    public CompiledScript compile(Reader queryString) throws ScriptException {
        try {
            Bindings global = this.getBindings(ScriptContext.GLOBAL_SCOPE);
            if (global == null) {
                global = createBindings();
                this.setBindings(createBindings(), ScriptContext.GLOBAL_SCOPE);
            }
            //
            QueryModel queryModel = CompilerHelper.queryParser(queryString);
            CompilerArguments compilerArguments = CompilerArguments.DEFAULT.copyAsNew();
            compilerArguments.getCompilerVar().addAll(global.keySet());
            QIL compilerQIL = CompilerHelper.queryCompiler(queryModel, compilerArguments, this.resourceLoader);
            return new QueryCompiledScript(compilerQIL, this);
        } catch (IOException e) {
            throw new ScriptException(e);
        }
    }

    @Override
    public CompiledScript compile(String queryString) throws ScriptException {
        return this.compile(new StringReader(queryString));
    }

    @Override
    public Object eval(Reader queryString, ScriptContext context) throws ScriptException {
        this.setContext(context);
        return compile(queryString).eval();
    }

    @Override
    public Object eval(String queryString, ScriptContext context) throws ScriptException {
        this.setContext(context);
        return compile(queryString).eval();
    }
}
