/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute.interceptor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;
import net.hasor.dataql.sqlproc.execute.fragment.SelectFragmentProcess;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContext;
import org.junit.AfterClass;
import org.junit.Test;

/**
 * 拦截器过滤与 SQL 执行性能基线。
 * 运行：./gradlew :dataql-sqlproc:cleanTest :dataql-sqlproc:test --tests "net.hasor.dataql.sqlproc.execute.interceptor.InterceptorFilterPerf"
 * 结果写入 build/perf/interceptor-filter-baseline.json（可用测试 JVM 系统属性 perf.file 覆盖），
 * 每次运行追加一条带时间戳的记录，并输出与上一次基线的对比。
 * runFragment 链路使用 close-shield 连接（close 变 no-op）复用同一连接，消除连接创建噪声，
 * 以便量化 filterInterceptors 在缓存命中场景下的真实边际开销。
 */
public class InterceptorFilterPerf extends AbstractSqlProcTest {

    private static final int WARMUP     = 2_000;
    private static final int ITERATIONS = 20_000;

    private static final Map<String, Long> RESULTS = new ConcurrentHashMap<>();

    @AfterClass
    public static void saveBaseline() throws IOException {
        String file = System.getProperty("perf.file", "build/perf/interceptor-filter-baseline.json");
        Path path = Paths.get(file);
        Map<String, Object> run = new LinkedHashMap<>();
        run.put("timestamp", System.currentTimeMillis());
        run.put("jvm", System.getProperty("java.version") + " (" + System.getProperty("java.vm.name") + ")");
        run.put("os", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        Map<String, Long> ordered = new LinkedHashMap<>();
        RESULTS.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> ordered.put(e.getKey(), e.getValue()));
        run.put("results", ordered);

        List<Map<String, Object>> history = new ArrayList<>();
        if (Files.exists(path)) {
            String prev = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            int idx = prev.indexOf('[');
            if (idx >= 0) {
                history = parseHistory(prev.substring(idx));
            }
        }
        history.add(run);

        Files.createDirectories(path.getParent());
        Files.write(path, renderJson(history).getBytes(StandardCharsets.UTF_8));
        System.out.println("[baseline] saved to " + path.toAbsolutePath());

        // compare with previous run
        if (history.size() >= 2) {
            Map<String, Object> prevRun = history.get(history.size() - 2);
            System.out.println("[baseline] compare vs " + prevRun.get("timestamp"));
            @SuppressWarnings("unchecked") Map<String, Number> prevResults = (Map<String, Number>) prevRun.get("results");
            for (Map.Entry<String, Long> e : ordered.entrySet()) {
                Number prev = prevResults.get(e.getKey());
                if (prev != null) {
                    double delta = (e.getValue() - prev.doubleValue()) / prev.doubleValue() * 100.0;
                    System.out.printf("[baseline]   %-55s %10d ns/op  (%+.1f%%)%n", e.getKey(), e.getValue(), delta);
                } else {
                    System.out.printf("[baseline]   %-55s %10d ns/op  (new)%n", e.getKey(), e.getValue());
                }
            }
        } else {
            System.out.println("[baseline] no previous run, saved as first baseline");
            for (Map.Entry<String, Long> e : ordered.entrySet()) {
                System.out.printf("[baseline]   %-55s %10d ns/op%n", e.getKey(), e.getValue());
            }
        }
    }

    /** 纯 filterInterceptors 开销：拦截器数量 × predicate 复杂度。 */
    @Test
    public void filterInterceptorsOnly() {
        int[] counts = { 0, 5, 50, 500 };
        boolean[] predicates = { false, true }; // false=always-true, true=类型匹配

        for (boolean realPredicate : predicates) {
            String prefix = realPredicate ? "filter/type-match" : "filter/always-true";
            for (int count : counts) {
                ExecuteContext ctx = newQueryContext();
                register(ctx, count, realPredicate);

                QueryType type = QueryType.Select;
                String sql = "SELECT name FROM users WHERE age = :age";
                HintsSet hints = hints();

                for (int i = 0; i < WARMUP; i++) {
                    ctx.filterInterceptors(type, sql, hints);
                }

                long best = Long.MAX_VALUE;
                for (int round = 0; round < 3; round++) {
                    long start = System.nanoTime();
                    for (int i = 0; i < ITERATIONS; i++) {
                        ctx.filterInterceptors(type, sql, hints);
                    }
                    long elapsed = System.nanoTime() - start;
                    best = Math.min(best, elapsed / ITERATIONS);
                }
                RESULTS.put(prefix + "/" + count, best);
            }
        }
    }

    /** runFragment 全链路：复用 close-shield 连接，缓存命中场景；验证 predicate 只被调用一次。 */
    @Test
    public void runFragmentWithCachedConfig() throws Exception {
        Connection real = newH2WithUsers();
        Connection shielded = shieldClose(real);
        ConnectionProvider provider = (sourceName, hints) -> shielded;
        int[] counts = { 0, 5, 50 };
        for (int count : counts) {
            ExecuteContext ctx = newQueryContext(provider);
            register(ctx, count, true);
            SelectFragmentProcess fragment = new SelectFragmentProcess(ctx);

            HintsSet hints = hints();
            Map<String, Object> params = Map.of("age", 20);
            String sql = "SELECT name FROM users WHERE age = :age";

            // predicate 调用计数：缓存命中后应只有首次 buildConfig 调用一次
            AtomicLong predicateCalls = new AtomicLong();
            ctx.addInterceptor(invocation -> invocation.proceed(), (type, fragmentString, h) -> {
                predicateCalls.incrementAndGet();
                return true;
            });

            for (int i = 0; i < WARMUP; i++) {
                try {
                    fragment.runFragment(hints, params, sql);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
            // warmup 后重置计数，测正式迭代的调用次数
            predicateCalls.set(0);

            long best = Long.MAX_VALUE;
            for (int round = 0; round < 3; round++) {
                long start = System.nanoTime();
                for (int i = 0; i < ITERATIONS; i++) {
                    try {
                        fragment.runFragment(hints, params, sql);
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                }
                long elapsed = System.nanoTime() - start;
                best = Math.min(best, elapsed / ITERATIONS);
            }
            System.out.println("[baseline]   predicate calls over " + ITERATIONS + " iterations (count=" + count + "): " + predicateCalls.get());
            RESULTS.put("runFragment/" + count, best);
        }
    }

    private void register(ExecuteContext ctx, int count, boolean realPredicate) {
        for (int i = 0; i < count; i++) {
            final int idx = i;
            SqlExecutionInterceptor interceptor = invocation -> invocation.proceed();
            if (realPredicate) {
                ctx.addInterceptor(interceptor, (type, fragmentString, hints) -> {
                    return idx % 2 == 0 || type == QueryType.Select;
                });
            } else {
                ctx.addInterceptor(interceptor);
            }
        }
    }

    /** 输入吞吐：SQL 样本集（简单/中等/复杂）全链路 tokens/s + MB/s（复用连接，缓存命中）。 */
    @Test
    public void inputThroughput() throws Exception {
        Connection real = newH2WithUsers();
        Connection shielded = shieldClose(real);
        ConnectionProvider provider = (sourceName, hints) -> shielded;
        ExecuteContext ctx = newQueryContext(provider);
        SelectFragmentProcess fragment = new SelectFragmentProcess(ctx);

        HintsSet hints = hints();
        Map<String, Object> params = Map.of();
        int iterations = 5_000;
        String[] samples = { "SELECT id, name, age FROM users WHERE id = 1", "SELECT id, name, age FROM users WHERE age > 18 AND name != 'X' AND id IN (1, 2, 3, 4, 5) ORDER BY id", buildSelect(100), buildSelect(1000) };

        for (String sql : samples) {
            measureAndReport("cache-hit", fragment, hints, params, sql, iterations);
        }
    }

    /** 冷路径吞吐：每次不同 SQL（configCache 不命中，解析+过滤+执行全链路）tokens/s + MB/s。 */
    @Test
    public void coldPathThroughput() throws Exception {
        Connection real = newH2WithUsers();
        Connection shielded = shieldClose(real);
        ConnectionProvider provider = (sourceName, hints) -> shielded;
        ExecuteContext ctx = newQueryContext(provider);
        register(ctx, 50, true);
        SelectFragmentProcess fragment = new SelectFragmentProcess(ctx);

        HintsSet hints = hints();
        Map<String, Object> params = Map.of();
        int iterations = 5_000;
        // 100 个不同 SQL（WHERE 值变化），每次 cacheKey 不同 → 每次全链路解析
        int sqlCount = 100;
        String[] samples = new String[sqlCount];
        int totalTokens = 0;
        int totalBytes = 0;
        for (int i = 0; i < sqlCount; i++) {
            samples[i] = "SELECT id, name, age FROM users WHERE age > " + (i * 10 % 90 + 10) + " AND id IN (" + (i % 10) + ", " + (i % 10 + 1) + ")";
            totalTokens += countTokens(samples[i]);
            totalBytes += samples[i].getBytes(StandardCharsets.UTF_8).length;
        }

        for (int i = 0; i < 500; i++) {
            try {
                fragment.runFragment(hints, params, samples[i % sqlCount]);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        long best = Long.MAX_VALUE;
        for (int round = 0; round < 3; round++) {
            long start = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                try {
                    fragment.runFragment(hints, params, samples[i % sqlCount]);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
            long elapsed = System.nanoTime() - start;
            best = Math.min(best, elapsed / iterations);
        }
        double avgTokens = totalTokens / (double) sqlCount;
        double avgBytes = totalBytes / (double) sqlCount;
        System.out.printf("[baseline]   cold-path avg tokens=%-6.1f bytes=%-7.1f %8d ns/op  %12.0f tokens/s  %8.1f MB/s%n", avgTokens, avgBytes, best, avgTokens * 1e9 / best, avgBytes * 1e9 / best / 1e6);
        RESULTS.put("cold-path/ns", best);
    }

    /** 测量单个 SQL 样本并输出/记录吞吐指标。 */
    private void measureAndReport(String tag, SelectFragmentProcess fragment, HintsSet hints, Map<String, Object> params, String sql, int iterations) {
        int tokens = countTokens(sql);
        int bytes = sql.getBytes(StandardCharsets.UTF_8).length;

        try {
            for (int i = 0; i < 500; i++) {
                fragment.runFragment(hints, params, sql);
            }

            long best = Long.MAX_VALUE;
            for (int round = 0; round < 3; round++) {
                long start = System.nanoTime();
                for (int i = 0; i < iterations; i++) {
                    fragment.runFragment(hints, params, sql);
                }
                long elapsed = System.nanoTime() - start;
                best = Math.min(best, elapsed / iterations);
            }
            System.out.printf("[baseline]   %-10s tokens=%-6d bytes=%-7d %8d ns/op  %12.0f tokens/s  %8.1f MB/s%n", tag, tokens, bytes, best, tokens * 1e9 / best, bytes * 1e9 / best / 1e6);
            RESULTS.put(tag + "/" + tokens + "t", best);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /** 构造 SELECT 1, 1, ... , 1（cols 个常量列），token 多且 H2 执行合法。 */
    private static String buildSelect(int cols) {
        StringBuilder sb = new StringBuilder("SELECT 1");
        for (int i = 1; i < cols; i++) {
            sb.append(", 1");
        }
        return sb.toString();
    }

    /** 简单分词统计 token 数：按空白与 ,() 分隔符切分。 */
    private static int countTokens(String sql) {
        int count = 0;
        StringBuilder cur = new StringBuilder();
        for (char c : sql.toCharArray()) {
            if (Character.isWhitespace(c) || c == ',' || c == '(' || c == ')') {
                if (cur.length() > 0) {
                    count++;
                    cur.setLength(0);
                }
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) {
            count++;
        }
        return count;
    }

    /** 包装 Connection，close() 变 no-op，用于复用连接消除连接创建噪声。 */
    private static Connection shieldClose(Connection real) {
        return (Connection) Proxy.newProxyInstance(InterceptorFilterPerf.class.getClassLoader(), new Class[] { Connection.class }, (proxy, method, args) -> {
            if (method.getName().equals("close")) {
                return null;
            }
            try {
                return method.invoke(real, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }

    @Override
    protected ExecuteContext newQueryContext() {
        return super.newQueryContext();
    }

    @Override
    protected ExecuteContext newQueryContext(ConnectionProvider provider) {
        return super.newQueryContext(provider);
    }

    // ----------------------------------------------------------------
    // minimal JSON render/parse (no external dep)
    // ----------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> parseHistory(String jsonArray) {
        List<Map<String, Object>> history = new ArrayList<>();
        List<Object> parsed = (List<Object>) new JsonParser().parse(jsonArray);
        for (Object o : parsed) {
            history.add((Map<String, Object>) o);
        }
        return history;
    }

    private static String renderJson(List<Map<String, Object>> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < history.size(); i++) {
            Map<String, Object> run = history.get(i);
            sb.append("  {\n");
            sb.append("    \"timestamp\": ").append(run.get("timestamp")).append(",\n");
            sb.append("    \"jvm\": \"").append(escape(run.get("jvm").toString())).append("\",\n");
            sb.append("    \"os\": \"").append(escape(run.get("os").toString())).append("\",\n");
            sb.append("    \"results\": {\n");
            @SuppressWarnings("unchecked") Map<String, Long> results = (Map<String, Long>) run.get("results");
            int j = 0;
            for (Map.Entry<String, Long> e : results.entrySet()) {
                sb.append("      \"").append(escape(e.getKey())).append("\": ").append(e.getValue());
                if (++j < results.size()) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("    }\n");
            sb.append("  }");
            if (i < history.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]\n");
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** 极简 JSON 解析，只支持本文件格式（数组内对象 + 字符串/数字）。 */
    private static final class JsonParser {
        private String text;
        private int    pos;

        Object parse(String t) {
            this.text = t;
            this.pos = 0;
            skipWs();
            Object v = parseValue();
            return v;
        }

        private Object parseValue() {
            skipWs();
            char c = text.charAt(pos);
            if (c == '{') {
                return parseObject();
            }
            if (c == '[') {
                return parseArray();
            }
            if (c == '"') {
                return parseString();
            }
            return parseNumber();
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // {
            skipWs();
            if (text.charAt(pos) == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWs();
                String key = (String) parseValue();
                skipWs();
                pos++; // :
                Object value = parseValue();
                map.put(key, value);
                skipWs();
                char c = text.charAt(pos++);
                if (c == '}') {
                    break;
                }
            }
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++; // [
            skipWs();
            if (text.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWs();
                char c = text.charAt(pos++);
                if (c == ']') {
                    break;
                }
            }
            return list;
        }

        private String parseString() {
            pos++; // "
            StringBuilder sb = new StringBuilder();
            while (text.charAt(pos) != '"') {
                char c = text.charAt(pos);
                if (c == '\\') {
                    pos++;
                    c = text.charAt(pos);
                }
                sb.append(c);
                pos++;
            }
            pos++; // "
            return sb.toString();
        }

        private Number parseNumber() {
            int start = pos;
            while (pos < text.length() && (Character.isDigit(text.charAt(pos)) || text.charAt(pos) == '-')) {
                pos++;
            }
            return Long.parseLong(text.substring(start, pos));
        }

        private void skipWs() {
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
                pos++;
            }
        }
    }
}
