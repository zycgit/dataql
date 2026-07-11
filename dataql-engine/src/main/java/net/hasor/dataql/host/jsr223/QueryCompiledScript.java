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
import java.util.HashMap;
import java.util.Map;
import javax.script.*;
import net.hasor.dataql.compiler.qil.QIL;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.host.Query;
import net.hasor.dataql.kernel.CustomizeScope;
import net.hasor.dataql.kernel.QueryResult;
import net.hasor.dataql.kernel.QueryRuntimeException;

/**
 * JSR223 编译机制的实现。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-10-19
 */
class QueryCompiledScript extends CompiledScript implements Hints {
    private final QIL                compilerQIL;
    private final HintsSet           optionSet;
    private final QueryScriptEngine engine;

    public QueryCompiledScript(QIL compilerQIL, QueryScriptEngine engine) {
        this.compilerQIL = compilerQIL;
        this.optionSet = new HintsSet();
        this.optionSet.setHints(engine);
        this.engine = engine;
    }

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

    @Override
    public ScriptEngine getEngine() {
        return this.engine;
    }

    @Override
    public QueryResult eval(ScriptContext context) throws ScriptException {
        Query query = this.engine.getQueryManager().createQuery(this.compilerQIL);
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
            query.setHints(this);
            return query.execute(customizeScope);
        } catch (QueryRuntimeException e) {
            throw new ScriptException(e);
        }
    }
}
