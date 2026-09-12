/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.compiler;
import java.io.IOException;
import java.util.List;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.loader.providers.ClassPathResourceLoader;
import net.hasor.dataql.AbstractTestResource;
import net.hasor.dataql.compiler.CompilerArguments.CodeLocationEnum;
import net.hasor.dataql.compiler.qil.QIL;
import net.hasor.dataql.parser.QueryModel;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * 测试用例
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-19
 */
public class CompilerTest extends AbstractTestResource {
    private static CompilerArguments arguments(CodeLocationEnum codeLocation) {
        CompilerArguments arguments = CompilerArguments.DEFAULT.copyAsNew();
        arguments.setCodeLocation(codeLocation);
        return arguments;
    }

    private void qilTest(String testCase) throws IOException {
        String query1 = getScript("/net_hasor_dataql_ast/" + testCase + "/ast.ql");
        QueryModel queryModel = CompilerHelper.queryParser(query1);
        QIL qilFast = CompilerHelper.queryCompiler(queryModel, arguments(CodeLocationEnum.NONE), ClassPathResourceLoader.INSTANCE);
        QIL qilDefault = CompilerHelper.queryCompiler(queryModel, arguments(CodeLocationEnum.LINE), ClassPathResourceLoader.INSTANCE);
        QIL qilDebug = CompilerHelper.queryCompiler(queryModel, arguments(CodeLocationEnum.TERM), ClassPathResourceLoader.INSTANCE);
        //
        String qilStringFast1 = qilFast.toString();
        String qilStringDefault1 = qilDefault.toString();
        String qilStringDebug1 = qilDebug.toString();
        //
        String qilStringFast2 = getScript("/net_hasor_dataql_ast/" + testCase + "/fast.qil");
        String qilStringDefault2 = getScript("/net_hasor_dataql_ast/" + testCase + "/default.qil");
        String qilStringDebug2 = getScript("/net_hasor_dataql_ast/" + testCase + "/debug.qil");
        assert qilStringFast1.trim().equals(qilStringFast2.trim());
        assert qilStringDefault1.trim().equals(qilStringDefault2.trim());
        assert qilStringDebug1.trim().equals(qilStringDebug2.trim());
    }

    private void astTest(String testCase) throws IOException {
        String query1 = getScript("/net_hasor_dataql_ast/" + testCase + "/ast.ql");
        QueryModel queryModel1 = CompilerHelper.queryParser(query1);
        String q11 = queryModel1.toQueryString();
        String q12 = CompilerHelper.queryParser(q11).toQueryString();
        List<String> list1 = acceptVisitor(queryModel1);
        String visitor1 = StringUtils.join(list1.toArray(), "\n");
        //
        String query2 = getScript("/net_hasor_dataql_ast/" + testCase + "/ast.format");
        //
        assert q11.equals(q12);                     // 原始QL 格式化之后再次 parser 应该和第一次 parser 相同。
        assert q11.trim().equals(query2.trim());    // 格式化之后和 预先格式化的 .format 应该相等
        assert !query1.trim().equals(query2.trim());// 格式化前后不相等
        //
        String basicVisitor = getScript("/net_hasor_dataql_ast/" + testCase + "/ast.visitor");
        assert visitor1.trim().equals(basicVisitor.trim());
        //
        qilTest(testCase);
    }

    @Test
    public void hint1_ast_format_test() throws IOException {
        astTest("hint_1");
    }

    @Test
    public void hint2_ast_format_test() throws IOException {
        astTest("hint_2");
    }

    @Test
    public void hint3_ast_format_test() throws IOException {
        astTest("hint_3");
    }

    @Test
    public void hintDeclaration_ast_format_test() throws IOException {
        QueryModel queryModel = CompilerHelper.queryParser("hint tenant;\nreturn 1;");
        String queryString = queryModel.toQueryString();
        QIL qil = CompilerHelper.queryCompiler(queryModel, arguments(CodeLocationEnum.NONE), ClassPathResourceLoader.INSTANCE);

        assertEquals("hint tenant;\n\nreturn 1;\n", queryString);
        assertFalse(qil.toString().contains("HINT"));
    }

    @Test
    public void import1_ast_format_test() throws IOException {
        astTest("import_1");
    }

    @Test
    public void import2_ast_format_test() throws IOException {
        astTest("import_2");
    }

    @Test
    public void basictype1_ast_format_test() throws IOException {
        astTest("basictype_1");
    }

    @Test
    public void return1_ast_format_test() throws IOException {
        astTest("return_1");
    }

    @Test
    public void return2_ast_format_test() throws IOException {
        astTest("return_2");
    }

    @Test
    public void exit1_ast_format_test() throws IOException {
        astTest("exit_1");
    }

    @Test
    public void exit2_ast_format_test() throws IOException {
        astTest("exit_2");
    }

    @Test
    public void throw1_ast_format_test() throws IOException {
        astTest("throw_1");
    }

    @Test
    public void throw2_ast_format_test() throws IOException {
        astTest("throw_2");
    }

    @Test
    public void switch1_ast_format_test() throws IOException {
        astTest("switch_1");
    }

    @Test
    public void switch2_ast_format_test() throws IOException {
        astTest("switch_2");
    }

    @Test
    public void switch3_ast_format_test() throws IOException {
        astTest("switch_3");
    }

    @Test
    public void switch4_ast_format_test() throws IOException {
        astTest("switch_4");
    }

    @Test
    public void switch5_ast_format_test() throws IOException {
        astTest("switch_5");
    }

    @Test
    public void expr1_ast_format_test() throws IOException {
        astTest("expr_1");
    }

    @Test
    public void expr2_ast_format_test() throws IOException {
        astTest("expr_2");
    }

    @Test
    public void object1_ast_format_test() throws IOException {
        astTest("val_object_1");
    }

    @Test
    public void object2_ast_format_test() throws IOException {
        astTest("val_object_2");
    }

    @Test
    public void object3_ast_format_test() throws IOException {
        astTest("val_object_3");
    }

    @Test
    public void object4_ast_format_test() throws IOException {
        astTest("val_object_4");
    }

    @Test
    public void fmt1_ast_format_test() throws IOException {
        astTest("fmt_1");
    }

    @Test
    public void fmt2_ast_format_test() throws IOException {
        astTest("fmt_2");
    }

    @Test
    public void fmt3_ast_format_test() throws IOException {
        astTest("fmt_3");
    }

    @Test
    public void fmt4_ast_format_test() throws IOException {
        astTest("fmt_4");
    }

    @Test
    public void fmt5_ast_format_test() throws IOException {
        astTest("fmt_5");
    }

    @Test
    public void fmt6_ast_format_test() throws IOException {
        astTest("fmt_6");
    }

    @Test
    public void fmt7_ast_format_test() throws IOException {
        astTest("fmt_7");
    }

    @Test
    public void fmt8_ast_format_test() throws IOException {
        astTest("fmt_8");
    }

    @Test
    public void route1_ast_format_test() throws IOException {
        astTest("route_1");
    }

    @Test
    public void route2_ast_format_test() throws IOException {
        astTest("route_2");
    }

    @Test
    public void route3_ast_format_test() throws IOException {
        astTest("route_3");
    }

    @Test
    public void route4_ast_format_test() throws IOException {
        astTest("route_4");
    }

    @Test
    public void route5_ast_format_test() throws IOException {
        astTest("route_5");
    }

    @Test
    public void run1_ast_format_test() throws IOException {
        astTest("run_1");
    }

    @Test
    public void lambda1_ast_format_test() throws IOException {
        astTest("lambda_1");
    }

    @Test
    public void lambda2_ast_format_test() throws IOException {
        astTest("lambda_2");
    }

    @Test
    public void fragment1_ast_format_test() throws IOException {
        astTest("fragment_1");
    }

    @Test
    public void fragment2_ast_format_test() throws IOException {
        astTest("fragment_2");
    }

    @Test
    public void fragment3_ast_format_test() throws IOException {
        astTest("fragment_3");
    }

    @Test
    public void error_ast_format_test() throws IOException {
        try {
            CompilerHelper.queryParser("return [1,2,3,4,5,6] => [ # ]"); // 不支持的语法
            assert false;
        } catch (Exception e) {
            assert true;
        }
    }
}
