/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.compiler;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.hasor.dataql.domain.Udf;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.QueryBuilder;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.kernel.QueryResult;
import net.hasor.dataql.kernel.QueryRuntimeException;
import net.hasor.dataql.kernel.ThrowRuntimeException;
import net.hasor.dataql.util.JsonUtils;
import org.junit.Test;
import static org.junit.Assert.*;

public class SyntaxExecutionTest {
    private Object execute(String script) throws Exception {
        return new QueryManager(new HostConfiguration()).newBuilder().createQuery(script).execute().getData().unwrap();
    }

    @Test
    public void commentsAreIgnoredExceptInsideStringsAndFragments() throws Exception {
        assertEquals(3, ((Number) this.execute("/* heading\ncomment */ var a=1; // line comment\nreturn a/* between tokens */+2;")).intValue());
        assertEquals("/* data */", this.execute("return '/* data */';"));
        HostConfiguration host = new HostConfiguration();
        host.addFragment("text", () -> (hints, params, text) -> text.trim());
        assertEquals("/* native comment */", new QueryManager(host).newBuilder()
                .createQuery("var f=@@text()<% /* native comment */ %>; return f();").execute().getData().unwrap());
    }

    @Test
    public void subtractionDoesNotDependOnWhitespace() throws Exception {
        for (String expression : List.of("3-1", "3 -1", "3- 1", "3 - 1", "3/* gap */-1")) {
            assertEquals(expression, 2, ((Number) this.execute("return " + expression + ";")).intValue());
        }
        assertEquals(4, ((Number) this.execute("return 3--1;")).intValue());
        assertEquals(-4, ((Number) this.execute("return -3-1;")).intValue());
        assertEquals(2, ((Number) this.execute("var a=3; return a-1;")).intValue());
        assertEquals(2, ((Number) this.execute("var a=[3]; return a[0]-1;")).intValue());
    }

    @Test
    public void signedLiteralsRemainValidInHintsIndexesAndExitCodes() throws Exception {
        assertEquals(-16, ((Number) this.execute("return -0x10;")).intValue());
        assertEquals(-8, ((Number) this.execute("return -0o10;")).intValue());
        assertEquals(-2, ((Number) this.execute("return -0b10;")).intValue());
        assertEquals(Integer.MIN_VALUE, this.execute("return -2147483648;"));
        assertEquals(Long.MIN_VALUE, this.execute("return -9223372036854775808;"));
        assertEquals(new BigInteger("-9223372036854775809"), this.execute("return -9223372036854775809;"));
        assertEquals(3, ((Number) this.execute("hint CUSTOM = -1; var a=[1,2,3]; return a[-1];")).intValue());
        QueryResult result = new QueryManager(new HostConfiguration()).newBuilder().createQuery("return -2, 'failed';").execute();
        assertEquals(-2, result.getCode());
        assertEquals("failed", result.getData().unwrap());
    }

    @Test
    public void scientificNotationSupportsZeroDigitsAndSignedExponents() throws Exception {
        String[] values = { "1e0", "10e2", "1.0e0", "0e0", "1e+02", "1e-2", "-.5E+1", "-10e2" };
        double[] expected = { 1, 1000, 1, 0, 100, 0.01, -5, -1000 };
        for (int i = 0; i < values.length; i++) {
            assertEquals(values[i], expected[i], ((Number) this.execute("return " + values[i] + ";")).doubleValue(), 0.000001);
        }
    }

    @Test
    public void unaryPlusAndMinusWorkOnNumbers() throws Exception {
        assertEquals(3, ((Number) this.execute("return +3;")).intValue());
        assertEquals(-3, ((Number) this.execute("return +-3;")).intValue());
        assertEquals(3, ((Number) this.execute("return - -3;")).intValue());
        assertEquals(-6, ((Number) this.execute("return -(1+2)*2;")).intValue());
        assertThrows(QueryRuntimeException.class, () -> this.execute("return +'3';"));
        assertThrows(QueryRuntimeException.class, () -> this.execute("return !1;"));
    }

    @Test
    public void precedenceFollowsJavaAndJavaScript() throws Exception {
        assertEquals(7, ((Number) this.execute("return 1+2*3;")).intValue());
        assertEquals(16L, ((Number) this.execute("return 1+1<<2+1;")).longValue());
        assertEquals(true, this.execute("return 1<<2<5;"));
        assertEquals(true, this.execute("return false==2>3;"));
        assertEquals(3L, ((Number) this.execute("return 1|2^3&1;")).longValue());
        assertEquals(true, this.execute("return true||false&&false;"));
        assertEquals(false, this.execute("return (true||false)&&false;"));
        assertEquals(3, ((Number) this.execute("return false?1:true?3:4;")).intValue());
        assertEquals(4, ((Number) this.execute("return 8-3-1;")).intValue());
        assertEquals(3, ((Number) this.execute("return 18\\3\\2;")).intValue());
    }

    @Test
    public void logicalOperatorsSkipUnneededCallsAndPreserveEvaluationOrder() throws Exception {
        HostConfiguration host = new HostConfiguration();
        AtomicInteger calls = new AtomicInteger();
        host.addImport("probe", () -> (Udf) (hints, params) -> {
            calls.incrementAndGet();
            return true;
        });
        QueryBuilder builder = new QueryManager(host).newBuilder();
        assertEquals(List.of(false, true, true, true), builder.createQuery("""
                import 'probe' as probe;
                return [false && probe(), true || probe(), true && probe(), false || probe()];
                """).execute().getData().unwrap());
        assertEquals(2, calls.get());
        assertEquals(true, this.execute("var fail=()->{throw 'unexpected';}; return true||false&&fail();"));
        assertEquals(false, this.execute("var fail=()->{throw 'unexpected';}; return false&&fail()||false;"));
        assertThrows(QueryRuntimeException.class, () -> this.execute("return 1||true;"));
        assertThrows(QueryRuntimeException.class, () -> this.execute("return true&&1;"));
    }

    @Test
    public void assertionsRequireBooleanAndStopExecutionOnFailure() throws Exception {
        assertEquals(1, ((Number) this.execute("assert true; return 1;")).intValue());
        assertThrows(ThrowRuntimeException.class, () -> this.execute("assert false; return 1;"));
        assertThrows(ThrowRuntimeException.class, () -> this.execute("assert 1; return 1;"));
    }

    @Test
    public void fragmentExpressionsExecuteInsideEachCall() throws Exception {
        HostConfiguration host = new HostConfiguration();
        host.addFragment("echo", () -> (hints, params, text) -> params);
        QueryBuilder builder = new QueryManager(host).newBuilder();
        assertEquals("[{\"x\":2,\"y\":5},{\"x\":4,\"y\":7}]", JsonUtils.writeValueAsString(builder.createQuery("""
                var offset=3;
                var f=@@echo(x,y=x+offset)<% ignored %>;
                return [f(2), f(4)];
                """).execute().getData().unwrap()));
        assertEquals(Map.of("x", (byte) 3), builder.createQuery("var f=@@echo(x=3)<% ignored %>; return f();")
                .execute().getData().unwrap());
        assertEquals(Map.of("x", (byte) 3), builder.createQuery("var f=@@echo(x=3)<% ignored %>; return f(9);")
                .execute().getData().unwrap());
        assertEquals(List.of(Map.of("x", (byte) 1), Map.of("x", (byte) 2)), builder.createQuery("var f=@@echo[](x)<% ignored %>; return f([1,2]);")
                .execute().getData().unwrap());
        assertThrows(QueryRuntimeException.class, () -> builder.createQuery("var f=@@echo[](x,y)<% ignored %>; return f([1],[1,2]);").execute());
    }

    @Test
    public void formattedQueriesRetainSignsAndPrecedence() throws Exception {
        for (String script : List.of("return -0x10;", "return -1, -2;", "var a=[1,2]; return a[-1];",
                "return 1|2^3&1;", "return true||false&&false;", "return 10e2;")) {
            String formatted = CompilerHelper.queryParser(script).toQueryString();
            assertEquals(script, this.execute(script), this.execute(formatted));
        }
    }
    @Test
    public void stringsDecodeEscapesAndRemainStableAfterFormatting() throws Exception {
        String script = "return ['\\u0041', 'line1\\nline2', 'C:\\\\temp', 'it\\'s', 'it''s', '\\t\\b\\f\\r'];";
        List<String> expected = List.of("A", "line1\nline2", "C:\\temp", "it's", "it's", "\t\b\f\r");
        assertEquals(expected, this.execute(script));
        assertEquals(expected, this.execute(CompilerHelper.queryParser(script).toQueryString()));
        String fields = "var value={'line\\nkey':'quote\\\"value'}; return value['line\\nkey'];";
        assertEquals("quote\"value", this.execute(fields));
        assertEquals(this.execute(fields), this.execute(CompilerHelper.queryParser(fields).toQueryString()));
    }

    @Test
    public void explicitFieldsBypassLocalNamesAndSurviveFormatting() throws Exception {
        String script = """
                var name='local';
                var data={'name':'root','child':{'name':'child'}};
                return data => {'variable':name,'field':#.name,
                    'child':child => {'root':$.name,'current':#.name}};
                """;
        Map<String, Object> expected = Map.of("variable", "local", "field", "root", "child", Map.of("root", "root", "current", "child"));
        assertEquals(expected, this.execute(script));
        assertEquals(expected, this.execute(CompilerHelper.queryParser(script).toQueryString()));
    }

    @Test
    public void exitStopsNestedFunctionsAndCollectionCallbacks() throws Exception {
        QueryBuilder builder = new QueryManager(new HostConfiguration()).newBuilder();
        for (String script : List.of("""
                var stop=()->{exit 10,'stopped';};
                var outer=()->{run stop(); throw 'unreachable';};
                run outer();
                throw 'unreachable';
                """, """
                import 'net.hasor.dataql.host.function.basic.CollectionUdfSource' as collect;
                run collect.filter([1,2,3], (value)->{exit 10,'stopped';});
                throw 'unreachable';
                """)) {
            QueryResult result = builder.createQuery(script).execute();
            assertTrue(result.isExit());
            assertEquals(10, result.getCode());
            assertEquals("stopped", result.getData().unwrap());
        }
        assertEquals(2, ((Number) this.execute("var f=()->{return 1;}; run f(); return 2;")).intValue());
        assertThrows(ThrowRuntimeException.class, () -> this.execute("var f=()->{throw 10,'failed';}; run f();"));
    }

}
