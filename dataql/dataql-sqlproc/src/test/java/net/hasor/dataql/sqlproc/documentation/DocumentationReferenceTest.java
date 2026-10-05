/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.documentation;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.Query;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContext;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContextImpl;
import net.hasor.dataql.sqlproc.execute.transaction.TransactionProvider;
import net.hasor.dataql.util.JsonUtils;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import static org.junit.Assert.*;

/** Runs complete code blocks from the reference itself, preventing example drift. */
public class DocumentationReferenceTest {
    @Test
    public void positionParameters() throws Exception {
        this.assertExample("parameters/position.md", "按顺序绑定", 0, "\"Alice\"");
        this.assertExample("parameters/position.md", "重复使用一个值", 0, "1");
        this.assertExample("parameters/position.md", "为位置参数增加选项", 0, "\"Alice\"");
    }

    @Test
    public void namedParametersAndExpressions() throws Exception {
        for (String section : List.of("绑定与复用", "对象属性与集合下标", "特殊属性名称", "模糊查询", "参数表达式")) {
            this.assertExample("parameters/named.md", section, 0, "\"Alice\"");
        }
        this.assertExample("parameters/named.md", "空值与缺失参数", 0, "0");
    }

    @Test
    public void textReplacement() throws Exception {
        this.assertExample("parameters/injection.md", "动态排序", 0, "[{\"id\":2,\"name\":\"Bob\",\"age\":30},{\"id\":1,\"name\":\"Alice\",\"age\":25}]");
        this.assertExample("parameters/injection.md", "动态表名", 0, "2");
    }

    @Test
    public void ruleParameters() throws Exception {
        this.assertExample("parameters/rule-binding.md", "选填筛选条件", 0, "[{\"name\":\"Bob\",\"age\":30}]");
        this.assertExample("parameters/rule-binding.md", "集合展开", 0, "[{\"name\":\"Alice\"},{\"name\":\"Bob\"}]");
    }

    @Test
    public void xmlConditionsAndPrefixes() throws Exception {
        String people = "[{\"id\":1,\"name\":\"Alice\"},{\"id\":2,\"name\":\"Bob\"}]";
        this.assertExample("mybaits.md", "条件判断：if", 0, people);
        this.assertExample("mybaits.md", "多分支：choose、when、otherwise", 0, "[{\"id\":1,\"name\":\"Alice\"}]");
        this.assertExample("mybaits.md", "条件区域：where", 0, "[{\"id\":2,\"name\":\"Bob\"}]");
        this.assertExample("mybaits.md", "前后缀处理：trim", 0, people);
        this.assertExample("mybaits.md", "更新区域：set", 0, "1");
    }

    @Test
    public void xmlForeachAndEmptyCollections() throws Exception {
        String script = this.script("mybaits.md", "集合展开：foreach", 0);
        this.assertScript(script, "[{\"id\":1,\"name\":\"Alice\"},{\"id\":2,\"name\":\"Bob\"}]");
        this.assertScript(script.replace("return find([1, 2]);", "return find([]);"), "[]");
        this.assertScript(script.replace("return find([1, 2]);", "return find(null);"), "[]");
        this.assertExample("mybaits.md", "集合展开：foreach", 1, "2");
    }

    @Test
    public void xmlBindIncludeAndCdata() throws Exception {
        this.assertExample("mybaits.md", "表达式变量：bind", 0, "[{\"id\":1,\"name\":\"Alice\"}]");
        this.assertExample("mybaits.md", "公共片段：include", 0, "[{\"id\":1,\"name\":\"Alice\"},{\"id\":2,\"name\":\"Bob\"}]");
        this.assertExample("mybaits.md", "XML 字符与标签组合", 0, "[{\"NAME\":\"Alice\"}]");
    }

    @Test
    public void explicitJdbcTypesAndJsonHandler() throws Exception {
        this.assertExample("parameter-options.md", "jdbcType：明确数据库类型", 1, "1");
        this.assertExample("parameter-options.md", "typeHandler：指定转换", 0, "\"{\\\"name\\\":\\\"Alice\\\",\\\"tags\\\":[\\\"java\\\",\\\"dataql\\\"]}\"");
    }

    @Test
    public void resultPackagingAndSelection() throws Exception {
        this.assertExample("results.md", "查询结果", 0, "[{\"id\":1,\"name\":\"Alice\"}]");
        this.assertExample("results.md", "查询结果", 1, "\"Alice\"");
        this.assertExample("results.md", "列名转换", 0, "[{\"userId\":1,\"userName\":\"Alice\"},{\"userId\":2,\"userName\":\"Bob\"}]");
        this.assertExample("results.md", "指定输出", 0, "{\"#result-set-1\":2}");
    }

    @Test
    public void transactionCommitAndRollback() throws Exception {
        String script = this.script("transactions.md", "提交和回滚", 0);
        this.assertTransaction(script, null, "\"done\"", 90, 110, null);
        this.assertTransaction(script.replace("change(2, 10)", "change(999, 10)"), "Target account not found", null, 100, 100, null);
        this.assertTransaction(this.transactionSetup() + this.script("transactions.md", "required：共享提交边界", 0), "Cancel transfer", null, 100, 100, null);
    }

    private String transactionSetup() throws Exception {
        String script = this.script("transactions.md", "提交和回滚", 0);
        return script.substring(0, script.indexOf("return tran.required"));
    }

    @Test
    public void independentAndNestedTransactions() throws Exception {
        String script = this.transactionSetup() + this.script("transactions.md", "requiresNew：独立提交", 0);
        this.assertTransaction(script, "Cancel outer transaction", null, 100, 110, null);
        this.assertTransaction(script.replace("tran.requiresNew", "tran.nested"), "Cancel outer transaction", null, 100, 100, null);
    }

    @Test
    public void mandatoryAndNeverRequireTheDocumentedContext() throws Exception {
        String setup = this.transactionSetup();
        String mandatory = this.script("transactions.md", "mandatory、never：限制调用环境", 0);
        this.assertTransaction(setup + mandatory, null, "1", 110, 100, null);
        this.assertTransaction(setup + mandatory.replace("tran.mandatory", "tran.never"), "propagation NEVER", null, 100, 100, null);
        String direct = "return tran.mandatory(() -> { return change(1, 10); });";
        this.assertTransaction(setup + direct, "propagation MANDATORY", null, 100, 100, null);
        this.assertTransaction(setup + direct.replace("tran.mandatory", "tran.never"), null, "1", 110, 100, null);
    }

    @Test
    public void isolationHintReachesTheJdbcConnection() throws Exception {
        String script = this.script("transactions.md", "隔离级别", 0);
        this.assertTransaction(script, null, "{\"first\":100,\"second\":100}", 100, 100, Connection.TRANSACTION_READ_COMMITTED);
        this.assertTransaction(script.replace("READ_COMMITTED", "SERIALIZABLE"), null, "{\"first\":100,\"second\":100}", 100, 100, Connection.TRANSACTION_SERIALIZABLE);
    }

    private void assertTransaction(String script, String expectedFailure, String expectedJson, int first, int second, Integer isolation) throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:transaction_reference_" + UUID.randomUUID());
        try (Connection keeper = source.getConnection(); TransactionProvider provider = new TransactionProvider((name, hints) -> source.getConnection())) {
            try (Statement statement = keeper.createStatement()) {
                statement.execute("CREATE TABLE accounts (id INT PRIMARY KEY, balance INT)");
                statement.execute("INSERT INTO accounts VALUES (1,100),(2,100)");
            }
            HostConfiguration host = new HostConfiguration();
            host.addAttachment(ConnectionProvider.class, provider);
            List<Integer> observedIsolation = new ArrayList<>();
            if (isolation != null) {
                host.getAttachment(ExecuteContext.class).addInterceptor(invocation -> {
                    try (Connection connection = provider.findConnection(invocation.getSqlInfo().sourceName(), invocation.getSqlInfo().hints())) {
                        observedIsolation.add(connection.getTransactionIsolation());
                        return invocation.proceed();
                    }
                });
            }
            Query query = new QueryManager(host).newBuilder().createQuery(script);
            if (expectedFailure != null) {
                Exception failure = assertThrows(Exception.class, query::execute);
                assertTrue(failure.getMessage(), failure.getMessage().contains(expectedFailure));
            } else {
                this.assertData(JsonUtils.readValue(expectedJson, Object.class), query.execute().getData().unwrap());
            }
            try (Statement statement = keeper.createStatement(); ResultSet balances = statement.executeQuery("SELECT balance FROM accounts ORDER BY id")) {
                assertTrue(balances.next());
                assertEquals(first, balances.getInt(1));
                assertTrue(balances.next());
                assertEquals(second, balances.getInt(1));
                assertFalse(balances.next());
            }
            if (isolation != null) {
                assertEquals(List.of(isolation, isolation), observedIsolation);
            }
        }
    }

    @Test
    public void pagingExamples() throws Exception {
        this.assertExample("dialect.md", "查询一页数据", 0, "{\"rows\":[{\"id\":2,\"name\":\"Bob\"}],\"total\":2,\"currentPage\":2}");
        this.assertExample("dialect.md", "翻页", 0, "{\"first\":[{\"id\":1,\"name\":\"Alice\"}],\"second\":[{\"id\":2,\"name\":\"Bob\"}]}");
    }

    @Test
    public void nestedRuleExamples() throws Exception {
        String people = "[{\"id\":1,\"name\":\"Alice\"},{\"id\":2,\"name\":\"Bob\"}]";
        this.assertExample("rules.md", "从一个查询开始", 0, "[{\"id\":2,\"name\":\"Bob\"}]");
        this.assertExample("rules/nesting.md", "条件与集合", 0, people);
        this.assertExample("rules/nesting.md", "分组条件", 0, people);
        this.assertExample("rules/nesting.md", "分支与集合", 0, "[]");
        this.assertExample("rules/nesting.md", "循环与分隔符", 0, "[{\"name\":\"Alice\"},{\"name\":\"Bob\"}]");
    }

    @Test
    public void statementRuleSqlBlocks() throws Exception {
        String alice = "[{\"id\":1,\"name\":\"Alice\",\"age\":25,\"enabled\":1}]";
        String both = "[{\"id\":1,\"name\":\"Alice\",\"age\":25,\"enabled\":1},{\"id\":2,\"name\":\"Bob\",\"age\":30,\"enabled\":1}]";
        this.assertRule("AND、IFAND", "name, minAge", "'Alice', 18", "selectSql", alice);
        this.assertRule("AND、IFAND", "name, minAge", "null, null", "selectSql", both);
        this.assertRule("OR、IFOR", "name, id", "'Alice', 2", "selectSql", both);
        this.assertRule("SET、IFSET", "name, changeAge, age, id", "'Alice', true, null, 1", "updateSql", "1");
        this.assertRule("IF、TEXT、IFTEXT", "name, newest", "'Alice', true", "selectSql", alice);
        this.assertRule("IN、IFIN", "ids", "[1,2,3]", "selectSql", both);
        this.assertRule("IN、IFIN", "ids", "[]", "selectSql", both);
        this.assertRule("CASE、WHEN、ELSE", "name, minAge", "'Alice', 30", "selectSql", alice);
        this.assertRule("CASE、WHEN、ELSE", "name, minAge", "null, null", "selectSql", both);
        this.assertRule("ARG", "id", "1", "selectSql", alice);
        this.assertRule("PAIRS", "names", "['Alice','Bob']", "selectSql", "[{\"name\":\"Alice\"},{\"name\":\"Bob\"}]");
    }

    private void assertRule(String heading, String parameters, String values, String fragment, String expectedJson) throws Exception {
        String sql = this.codeBlock("rules/statements.md", heading, "sql", 0);
        String script = "hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';\n" + "hint FRAGMENT_SQL_COLUMN_CASE = 'lower';\n" + "var invoke = @@" + fragment + "(" + parameters + ")<%" + sql + "%>;\n" + "return invoke(" + values + ");";
        this.assertScript(script, expectedJson);
    }

    private void assertExample(String file, String heading, int blockIndex, String expectedJson) throws Exception {
        this.assertScript(this.script(file, heading, blockIndex), expectedJson);
    }

    private String script(String file, String heading, int blockIndex) throws Exception {
        return this.codeBlock(file, heading, "(?:javascript|dataql|dql)", blockIndex);
    }

    private String codeBlock(String file, String heading, String language, int blockIndex) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.isDirectory(root.resolve("document/docs/dataql/sql"))) {
            root = root.getParent();
        }
        assertNotNull("The documentation must be available in the repository", root);
        String markdown = Files.readString(root.resolve("document/docs/dataql/sql").resolve(file));
        Matcher headings = Pattern.compile("(?m)^#{2,3} " + Pattern.quote(heading) + "(?: [{][^}]+[}])?$").matcher(markdown);
        assertTrue("Missing section: " + file + " / " + heading, headings.find());
        int section = headings.start();
        Matcher nextHeading = Pattern.compile("(?m)^#{2,3} ").matcher(markdown);
        int end = nextHeading.find(headings.end()) ? nextHeading.start() : -1;
        String content = markdown.substring(section, end < 0 ? markdown.length() : end);
        Matcher blocks = Pattern.compile("```" + language + "[^\\n]*\\n(.*?)\\n```", Pattern.DOTALL).matcher(content);
        for (int index = 0; blocks.find(); index++) {
            if (index == blockIndex) {
                return blocks.group(1);
            }
        }
        throw new AssertionError("Missing executable block " + blockIndex + ": " + file + " / " + heading);
    }

    private void assertScript(String script, String expectedJson) throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:reference_" + UUID.randomUUID());
        try (Connection keeper = source.getConnection()) {
            try (Statement statement = keeper.createStatement()) {
                statement.execute("CREATE TABLE people (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, name VARCHAR(100), age INT, enabled INT DEFAULT 1)");
                statement.execute("INSERT INTO people(name, age) VALUES ('Alice',25),('Bob',30)");
            }
            HostConfiguration host = new HostConfiguration();
            host.addAttachment(ConnectionProvider.class, (name, hints) -> source.getConnection());
            ((ExecuteContextImpl) host.getAttachment(ExecuteContext.class)).addMacro("adult", "age >= 18");
            List<String> statements = new ArrayList<>();
            host.getAttachment(ExecuteContext.class).addInterceptor(invocation -> {
                statements.add(invocation.getSqlInfo().queryString());
                return invocation.proceed();
            });
            Object actual = new QueryManager(host).newBuilder().createQuery(script).execute().getData().unwrap();
            this.assertData(JsonUtils.readValue(expectedJson, Object.class), actual);
            assertFalse("The documented example must execute JDBC", statements.isEmpty());
        }
    }

    private void assertData(Object expected, Object actual) {
        if (expected instanceof Number && actual instanceof Number) {
            assertEquals(0, new BigDecimal(expected.toString()).compareTo(new BigDecimal(actual.toString())));
        } else if (expected instanceof List<?> expectedList && actual instanceof List<?> actualList) {
            assertEquals(expectedList.size(), actualList.size());
            for (int index = 0; index < expectedList.size(); index++) {
                this.assertData(expectedList.get(index), actualList.get(index));
            }
        } else if (expected instanceof Map<?, ?> expectedMap && actual instanceof Map<?, ?> actualMap) {
            assertEquals(expectedMap.keySet(), actualMap.keySet());
            for (Object key : expectedMap.keySet()) {
                this.assertData(expectedMap.get(key), actualMap.get(key));
            }
        } else {
            assertEquals(expected, actual);
        }
    }
}
