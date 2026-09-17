/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.host.jsr223;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.script.*;
import net.hasor.cobble.loader.ResourceLoader;
import net.hasor.cobble.loader.providers.ClassPathResourceLoader;
import net.hasor.dataql.domain.*;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.HostContext;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.kernel.QueryResult;
import org.junit.Test;
import static org.junit.Assert.*;

public class Jsr223Test {
    @Test
    public void jar223_engineContextIsHostContext() {
        ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("dataql");

        assertTrue(scriptEngine.getContext() instanceof HostContext);
    }

    @Test
    public void jar223_factoryMethodsUseDataqlSyntax() throws ScriptException {
        ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("dataql");
        ScriptEngineFactory factory = scriptEngine.getFactory();

        assertEquals("user.load(id, name)", factory.getMethodCallSyntax("user", "load", "id", "name"));
        assertEquals("return \"hello\"", factory.getOutputStatement("hello"));
        assertEquals("var a = 1" + System.lineSeparator() + "return a", factory.getProgram("var a = 1", "return a"));
        assertNull(factory.getParameter("unknown"));

        Object eval = scriptEngine.eval(factory.getOutputStatement("hello"));
        assertEquals("hello", ((QueryResult) eval).getData().unwrap());
    }

    @Test
    public void jar223_evalWithContextKeepsQueryScriptContext() throws ScriptException {
        ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("dataql");
        QueryScriptContext params = (QueryScriptContext) scriptEngine.getContext();
        params.setBindings(scriptEngine.createBindings(), ScriptContext.ENGINE_SCOPE);
        params.setAttribute("value", 1, ScriptContext.ENGINE_SCOPE);

        scriptEngine.eval("return ${value}", params);

        assertSame(params, scriptEngine.getContext());
        assertSame(params.getBindings(ScriptContext.ENGINE_SCOPE), scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE));
    }

    @Test
    public void jar223_evalWithExplicitContextDoesNotReplaceDefaultContext() throws ScriptException {
        QueryScriptEngine scriptEngine = (QueryScriptEngine) new ScriptEngineManager().getEngineByName("dataql");
        QueryScriptContext defaultContext = (QueryScriptContext) scriptEngine.getContext();
        QueryScriptContext explicitContext = new QueryScriptContext(defaultContext.getHostContext());
        explicitContext.setBindings(scriptEngine.createBindings(), ScriptContext.ENGINE_SCOPE);
        explicitContext.setAttribute("value", 1, ScriptContext.ENGINE_SCOPE);

        scriptEngine.eval("return ${value}", explicitContext);

        assertSame(defaultContext, scriptEngine.getContext());
    }

    @Test
    public void jar223_engineRejectsContextFromAnotherHost() {
        QueryScriptEngine scriptEngine = (QueryScriptEngine) new ScriptEngineManager().getEngineByName("dataql");
        QueryManager defaultManager = scriptEngine.getQueryManager();
        QueryScriptContext otherContext = (QueryScriptContext) new ScriptEngineManager().getEngineByName("dataql").getContext();

        try {
            scriptEngine.setContext(otherContext);
            fail("QueryScriptContext from another HostContext must not be accepted.");
        } catch (IllegalArgumentException e) {
            assertEquals("context hostContext must match engine hostContext.", e.getMessage());
        }
        assertSame(defaultManager, scriptEngine.getQueryManager());
    }

    @Test
    public void jar223_rejectsPlainScriptContext() throws ScriptException {
        QueryScriptEngine scriptEngine = (QueryScriptEngine) new ScriptEngineManager().getEngineByName("dataql");
        SimpleScriptContext plainContext = new SimpleScriptContext();

        try {
            scriptEngine.setContext(plainContext);
            fail("Plain ScriptContext must not be accepted.");
        } catch (IllegalArgumentException e) {
            assertEquals("context must be QueryScriptContext.", e.getMessage());
        }
        try {
            scriptEngine.eval("return 1", plainContext);
            fail("Plain ScriptContext must not be accepted.");
        } catch (IllegalArgumentException e) {
            assertEquals("context must be QueryScriptContext.", e.getMessage());
        }
    }

    @Test
    public void jar223_queryScriptContextCarriesHostContext() throws ScriptException {
        ResourceLoader resourceLoader = new ClassPathResourceLoader() {
            @Override
            public InputStream getResourceAsStream(String resource) {
                if ("jsr223-custom.ql".equals(resource)) {
                    return new ByteArrayInputStream("return 'HostFromAttribute'".getBytes(StandardCharsets.UTF_8));
                }
                return super.getResourceAsStream(resource);
            }
        };
        HostConfiguration configuration = new HostConfiguration(resourceLoader, null);
        ScriptEngine scriptEngine = new QueryScriptEngine(new QueryScriptEngineFactory(), configuration);

        Object eval = scriptEngine.eval("import @\"jsr223-custom.ql\" as custom; return custom()");

        assertEquals("HostFromAttribute", ((QueryResult) eval).getData().unwrap());
    }

    @Test
    public void jar223_scriptContextIsHostContext() throws ScriptException {
        ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("dataql");
        QueryScriptContext context = (QueryScriptContext) scriptEngine.getContext();
        context.setBindings(new SimpleBindings(), ScriptContext.GLOBAL_SCOPE);
        context.setAttribute("foo", (Udf) (readOnly, params) -> "from-context", ScriptContext.GLOBAL_SCOPE);

        assertTrue(context instanceof HostContext);

        Object eval = scriptEngine.eval("return foo()", context);

        assertEquals("from-context", ((QueryResult) eval).getData().unwrap());
    }

    @Test
    public void jar223_scriptContextHintsOverrideEngineHints() throws ScriptException {
        Udf testUdf = (readOnly, params) -> readOnly.getHint("tenant");
        QueryScriptEngine scriptEngine = (QueryScriptEngine) new ScriptEngineManager().getEngineByName("dataql");
        scriptEngine.setHint("tenant", "default");
        QueryScriptContext context = (QueryScriptContext) scriptEngine.getContext();
        context.setBindings(scriptEngine.createBindings(), ScriptContext.GLOBAL_SCOPE);
        context.setAttribute("foo", testUdf, ScriptContext.GLOBAL_SCOPE);
        context.setHint("tenant", "north");

        Object eval = scriptEngine.eval("return foo()", context);

        assertEquals("north", ((QueryResult) eval).getData().unwrap());
        assertEquals("default", scriptEngine.getHint("tenant"));
    }

    @Test
    public void jar223_queryScriptContextCanCarryHintsAttribute() throws ScriptException {
        Udf testUdf = (readOnly, params) -> readOnly.getHint("tenant");
        QueryScriptEngine scriptEngine = (QueryScriptEngine) new ScriptEngineManager().getEngineByName("dataql");
        scriptEngine.setHint("tenant", "default");
        QueryScriptContext context = (QueryScriptContext) scriptEngine.getContext();
        context.setBindings(scriptEngine.createBindings(), ScriptContext.GLOBAL_SCOPE);
        context.setAttribute("foo", testUdf, ScriptContext.GLOBAL_SCOPE);
        HintsSet hints = new HintsSet();
        hints.setHint("tenant", "west");
        context.setAttribute(Hints.class.getName(), hints, ScriptContext.ENGINE_SCOPE);

        Object eval = scriptEngine.eval("return foo()", context);

        assertEquals("west", ((QueryResult) eval).getData().unwrap());
    }

    @Test
    public void jar223_1() throws ScriptException {
        ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("dataql");
        Object eval = scriptEngine.eval("var a= 10 ; return a");
        //
        assert eval instanceof QueryResult;
        DataModel dataModel = ((QueryResult) eval).getData();
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).asInt() == 10;
    }

    @Test
    public void jar223_2() throws ScriptException {
        ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("dataql");
        //
        QueryScriptContext params = (QueryScriptContext) scriptEngine.getContext();
        params.setBindings(scriptEngine.createBindings(), ScriptContext.GLOBAL_SCOPE);
        params.setBindings(scriptEngine.createBindings(), ScriptContext.ENGINE_SCOPE);
        params.setAttribute("uid", "uid form env", ScriptContext.ENGINE_SCOPE);
        params.setAttribute("sid", "sid form env", ScriptContext.GLOBAL_SCOPE);
        //
        Object eval = scriptEngine.eval("return [${uid},${sid}]", params);
        assert eval instanceof QueryResult;
        DataModel dataModel = ((QueryResult) eval).getData();
        assert dataModel.isList();
        assert ((ListModel) dataModel).getValue(0).asString().equals("uid form env");
        assert ((ListModel) dataModel).getValue(1).asString().equals("sid form env");
    }

    @Test
    public void jar223_3() throws ScriptException {
        ScriptEngineManager engineManager = new ScriptEngineManager();
        QueryScriptEngine scriptEngine = (QueryScriptEngine) engineManager.getEngineByName("dataql");
        //
        QueryScriptContext params = (QueryScriptContext) scriptEngine.getContext();
        params.setBindings(scriptEngine.createBindings(), ScriptContext.GLOBAL_SCOPE);
        params.setBindings(scriptEngine.createBindings(), ScriptContext.ENGINE_SCOPE);
        params.setAttribute("uid", "uid form env", ScriptContext.ENGINE_SCOPE);
        params.setAttribute("sid", "sid form env", ScriptContext.GLOBAL_SCOPE);
        //
        Object eval = scriptEngine.eval("return [${uid},${sid}]", params);
        assert eval instanceof QueryResult;
        DataModel dataModel = ((QueryResult) eval).getData();
        assert dataModel.isList();
        assert ((ListModel) dataModel).getValue(0).asString().equals("uid form env");
        assert ((ListModel) dataModel).getValue(1).asString().equals("sid form env");
    }

    @Test
    public void jar223_4() throws ScriptException {
        Udf testUdf = (readOnly, params) -> readOnly.getHint("abc");
        //
        QueryScriptEngine scriptEngine = (QueryScriptEngine) new ScriptEngineManager().getEngineByName("dataql");
        QueryScriptContext params = (QueryScriptContext) scriptEngine.getContext();
        params.setBindings(scriptEngine.createBindings(), ScriptContext.GLOBAL_SCOPE);// GLOBAL is CompilerVar
        params.setAttribute("foo", testUdf, ScriptContext.GLOBAL_SCOPE);
        //
        scriptEngine.setHint("abc", 10);
        Object eval = scriptEngine.eval("return foo()", params);
        assert eval instanceof QueryResult;
        DataModel dataModel = ((QueryResult) eval).getData();
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).asInt() == 10;
    }

}
