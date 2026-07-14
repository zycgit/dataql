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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.script.*;
import net.hasor.dataql.compiler.qil.QIL;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.host.Query;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.kernel.CustomizeScope;
import net.hasor.dataql.kernel.QueryResult;
import net.hasor.dataql.kernel.QueryRuntimeException;

/**
 * JSR223 编译机制的实现。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-10-19
 */
class QueryCompiledScript extends CompiledScript implements Hints {
    private final QIL          compilerQIL;
    private final ScriptEngine engine;
    private final QueryManager queryManager;
    private final Hints        engineHints;

    public QueryCompiledScript(QIL compilerQIL, ScriptEngine engine, QueryManager queryManager, Hints engineHints) {
        this.compilerQIL = compilerQIL;
        this.engine = engine;
        this.queryManager = queryManager;
        this.engineHints = engineHints;
    }

    @Override
    public String[] getHints() {
        return this.engineHints.getHints();
    }

    @Override
    public Object getHint(String optionKey) {
        return this.engineHints.getHint(optionKey);
    }

    @Override
    public void removeHint(String optionKey) {
        this.engineHints.removeHint(optionKey);
    }

    @Override
    public void setHint(String hintName, String value) {
        this.engineHints.setHint(hintName, value);
    }

    @Override
    public void setHint(String hintName, Number value) {
        this.engineHints.setHint(hintName, value);
    }

    @Override
    public void setHint(String hintName, boolean value) {
        this.engineHints.setHint(hintName, value);
    }

    @Override
    public ScriptEngine getEngine() {
        return this.engine;
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
    public QueryResult eval(ScriptContext context) throws ScriptException {
        this.checkContext(context);

        Query query = this.queryManager.newBuilder().createQuery(this.compilerQIL);
        Bindings globalBindings = context.getBindings(ScriptContext.GLOBAL_SCOPE);
        if (globalBindings != null) {
            globalBindings.forEach(query::addShareVar);
        }
        //
        Bindings engineBindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
        Map<String, Object> dataMap = new HashMap<>();
        if (globalBindings != null) {
            dataMap.putAll(globalBindings);
        }
        if (engineBindings != null) {
            dataMap.putAll(engineBindings);
        }
        CustomizeScope customizeScope = symbol -> {
            return dataMap;
        };
        try {
            HintsSet hints = new HintsSet();
            hints.setHints(this.engineHints);
            if (context instanceof Hints) {
                hints.setHints((Hints) context);
            }
            Object contextHints = context.getAttribute(Hints.class.getName());
            if (contextHints instanceof Hints) {
                hints.setHints((Hints) contextHints);
            }
            query.setHints(hints);
            return query.execute(customizeScope);
        } catch (QueryRuntimeException e) {
            throw new ScriptException(e);
        }
    }
}
