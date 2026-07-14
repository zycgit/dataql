/*
 * Copyright 2015-2022 the original author or authors.
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
import net.hasor.dataql.compiler.qil.QIL;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.HostContext;
import net.hasor.dataql.host.QueryBuilder;
import net.hasor.dataql.host.QueryManager;

/**
 * JSR223 引擎机制的实现。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-10-19
 */
public class QueryScriptEngine extends AbstractScriptEngine implements ScriptEngine, Compilable, Hints {
    private final HintsSet                 optionSet = new HintsSet();
    private final QueryScriptEngineFactory factory;
    private final QueryManager             queryManager;

    QueryScriptEngine(QueryScriptEngineFactory factory, HostConfiguration configuration) {
        this.factory = Objects.requireNonNull(factory, "factory is null.");
        Objects.requireNonNull(configuration, "configuration is null.");

        HostContext ctx = configuration.getHostContext();
        this.queryManager = new QueryManager(ctx);
        this.setContext(new QueryScriptContext(ctx));
    }

    QueryManager getQueryManager() {
        return this.queryManager;
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
    // -------------------------------------------------------------------------------------------- ScriptEngine

    @Override
    public ScriptEngineFactory getFactory() {
        return this.factory;
    }

    @Override
    public Bindings createBindings() {
        return new SimpleBindings();
    }

    // -------------------------------------------------------------------------------------------- ScriptEngine

    @Override
    public void setContext(ScriptContext context) {
        this.checkContext(context);
        super.setContext(context);
    }

    private void checkContext(ScriptContext context) {
        Objects.requireNonNull(context, "context is null.");
        if (!(context instanceof QueryScriptContext)) {
            throw new IllegalArgumentException("context must be QueryScriptContext.");
        }
        if (((QueryScriptContext) context).getHostContext() != this.queryManager.hostContext()) {
            throw new IllegalArgumentException("context hostContext must match engine hostContext.");
        }
    }

    @Override
    public CompiledScript compile(Reader queryString) throws ScriptException {
        return this.compile(queryString, this.getContext());
    }

    @Override
    public CompiledScript compile(String queryString) throws ScriptException {
        return this.compile(new StringReader(queryString));
    }

    @Override
    public Object eval(Reader queryString, ScriptContext context) throws ScriptException {
        return compile(queryString, context).eval(context);
    }

    @Override
    public Object eval(String queryString, ScriptContext context) throws ScriptException {
        return compile(new StringReader(queryString), context).eval(context);
    }

    private CompiledScript compile(Reader queryString, ScriptContext context) throws ScriptException {
        try {
            this.checkContext(context);
            Bindings global = context.getBindings(ScriptContext.GLOBAL_SCOPE);
            if (global == null) {
                global = createBindings();
                context.setBindings(global, ScriptContext.GLOBAL_SCOPE);
            }
            //
            QueryBuilder queryBuilder = this.queryManager.newBuilder();
            Bindings compileBindings = global;
            global.keySet().forEach(key -> queryBuilder.addShareVar(key, () -> compileBindings.get(key)));

            QIL compilerQIL = queryBuilder.compilerQuery(queryString);
            return new QueryCompiledScript(compilerQIL, this, this.queryManager, this);
        } catch (IOException e) {
            throw new ScriptException(e);
        }
    }
}
