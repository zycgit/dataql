/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.rule;
import java.sql.SQLException;
import java.util.Map;
import net.hasor.cobble.CollectionUtils;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.rule.dto.UserFutures;
import net.hasor.dataql.sqlproc.dynamic.segment.DynamicParsed;
import net.hasor.dataql.sqlproc.dynamic.segment.PlanDynamicSql;
import net.hasor.dataql.sqlproc.types.SqlArg;
import net.hasor.dataql.sqlproc.types.SqlMode;
import net.hasor.dataql.sqlproc.types.number.ShortTypeHandler;
import net.hasor.dataql.sqlproc.types.string.StringTypeHandler;
import org.junit.Test;

public class ArgRuleTest {

    @Test
    public void ruleTest_3() throws SQLException {
        Map<String, Object> ctx1 = CollectionUtils.asMap("name", "abc");
        PlanDynamicSql segment1 = DynamicParsed.getParsedSql("#{name}");
        SqlBuilder sqlBuilder1 = segment1.buildQuery(ctx1, new TestQueryContext());
        assert sqlBuilder1.getSqlString().equals("?");
        assert sqlBuilder1.getArgs().length == 1;
        assert ((SqlArg) sqlBuilder1.getArgs()[0]).getSqlMode() == null;
        assert ((SqlArg) sqlBuilder1.getArgs()[0]).getJdbcType() == null;
        assert ((SqlArg) sqlBuilder1.getArgs()[0]).getTypeHandler() == null;

        //
        Map<String, Object> ctx2 = CollectionUtils.asMap("name", "abc");
        PlanDynamicSql segment2 = DynamicParsed.getParsedSql("#{name,mode=out,jdbcType=123,typeHandler=" + ShortTypeHandler.class.getName() + "}");
        SqlBuilder sqlBuilder2 = segment2.buildQuery(ctx2, new TestQueryContext());
        assert sqlBuilder2.getSqlString().equals("?");
        assert sqlBuilder2.getArgs().length == 1;
        assert ((SqlArg) sqlBuilder2.getArgs()[0]).getSqlMode() == SqlMode.Out;
        assert ((SqlArg) sqlBuilder2.getArgs()[0]).getJdbcType() == 123;
        assert ((SqlArg) sqlBuilder2.getArgs()[0]).getTypeHandler() instanceof ShortTypeHandler;
    }

    @Test
    public void ruleTest_6() throws SQLException {
        Map<String, Object> ctx2 = CollectionUtils.asMap("name", new UserFutures());
        PlanDynamicSql segment2 = DynamicParsed.getParsedSql("#{name}");
        SqlBuilder sqlBuilder2 = segment2.buildQuery(ctx2, new TestQueryContext());
        assert sqlBuilder2.getSqlString().equals("?");
        assert sqlBuilder2.getArgs().length == 1;
        assert ((SqlArg) sqlBuilder2.getArgs()[0]).getTypeHandler() == null;
    }

    @Test
    public void ruleTest_7() throws SQLException {
        Map<String, Object> ctx = CollectionUtils.asMap("name", "abc");
        PlanDynamicSql segment = DynamicParsed.getParsedSql("#{name,mode=out,jdbcType=123,typeHandler=" + StringTypeHandler.class.getName() + "}");

        SqlBuilder sqlBuilder1 = segment.buildQuery(ctx, new TestQueryContext());
        assert sqlBuilder1.getSqlString().equals("?");
        assert sqlBuilder1.getArgs().length == 1;
        assert ((SqlArg) sqlBuilder1.getArgs()[0]).getSqlMode() == SqlMode.Out;
        assert ((SqlArg) sqlBuilder1.getArgs()[0]).getJdbcType() == 123;
        assert ((SqlArg) sqlBuilder1.getArgs()[0]).getTypeHandler() instanceof StringTypeHandler;
    }

    @Test
    public void ruleTest_8() throws SQLException {
        Map<String, Object> ctx = CollectionUtils.asMap("name", "abc");
        PlanDynamicSql segment = DynamicParsed.getParsedSql("#{name,,,,}");

        SqlBuilder sqlBuilder1 = segment.buildQuery(ctx, new TestQueryContext());
        assert sqlBuilder1.getSqlString().equals("?");
        assert sqlBuilder1.getArgs().length == 1;
        assert ((SqlArg) sqlBuilder1.getArgs()[0]).getSqlMode() == null;
        assert ((SqlArg) sqlBuilder1.getArgs()[0]).getJdbcType() == null;
        assert ((SqlArg) sqlBuilder1.getArgs()[0]).getTypeHandler() == null;
    }

    @Test
    public void toStringTest_1() {
        assert ArgRule.INSTANCE.toString().startsWith("arg [");
    }
}
