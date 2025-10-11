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
package net.hasor.dataql.sqlproc.dynamic.config;

import net.hasor.cobble.CollectionUtils;
import net.hasor.dataql.runtime.HintsSet;
import net.hasor.dataql.sqlproc.dynamic.SqlArg;
import net.hasor.dataql.sqlproc.dynamic.SqlArgSource;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.args.BeanSqlArgSource;
import net.hasor.dataql.sqlproc.dynamic.resolve.ConfigResolveByXmlSql;
import net.hasor.dataql.sqlproc.dynamic.rule.TestQueryContext;
import net.hasor.dataql.sqlproc.utils.UserInfo;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/**
 * @author 赵永春 (zyc@hasor.net)
 * @version 2013-12-10
 */
public class DynamicXmlMapperTest {
    @Test
    public void bind_1() throws Exception {
        String config = "<bind name=\"abc\" value=\"sellerId + 'abc'\"/>\n" + //
                "select * from console_job where aac = #{abc}";
        Map<String, Object> ctx = CollectionUtils.asMap("sellerId", "123");

        SqlConfig sqlConfig = new ConfigResolveByXmlSql().parseConfig("select", new HintsSet(), config);
        SqlBuilder sqlBuilder = sqlConfig.buildQuery(ctx, new TestQueryContext());
        assert sqlBuilder.getSqlString().trim().equals("select * from console_job where aac = ?");
        assert sqlBuilder.getArgs().length == 1;
        assert ((SqlArg) sqlBuilder.getArgs()[0]).getValue().equals("123abc");
    }

    @Test
    public void choose_1() throws Exception {
        String config = "select * from t_blog\n" +//
                "<where>\n" +       //
                "    <choose>\n" +  //
                "        <when test=\"title != null\">and title = #{title}</when>\n" +      //
                "        <when test=\"content != null\">and content = #{content}</when>\n" +//
                "        <otherwise>and owner = \"owner1\"</otherwise>\n" +                 //
                "    </choose>\n" + //
                "</where>";
        Map<String, Object> ctx = CollectionUtils.asMap("title", "123", "content", "aaa");

        SqlConfig sqlConfig = new ConfigResolveByXmlSql().parseConfig("select", new HintsSet(), config);
        SqlBuilder sqlBuilder = sqlConfig.buildQuery(ctx, new TestQueryContext());
        assert sqlBuilder.getSqlString().equals("select * from t_blog\nwhere  title = ? ");
        assert sqlBuilder.getArgs().length == 1;
        assert ((SqlArg) sqlBuilder.getArgs()[0]).getValue().equals("123");
    }

    @Test
    public void choose_2() throws Exception {
        String config = "select * from t_blog\n" +//
                "<where>\n" +       //
                "    <choose>\n" +  //
                "        <when test=\"title != null\">and title = #{title}</when>\n" +      //
                "        <when test=\"content != null\">and content = #{content}</when>\n" +//
                "        <otherwise>and owner = \"owner1\"</otherwise>\n" +                 //
                "    </choose>\n" + //
                "</where>";
        Map<String, Object> ctx = Collections.emptyMap();

        SqlConfig sqlConfig = new ConfigResolveByXmlSql().parseConfig("select", new HintsSet(), config);
        SqlBuilder sqlBuilder = sqlConfig.buildQuery(ctx, new TestQueryContext());
        assert sqlBuilder.getSqlString().equals("select * from t_blog\nwhere  owner = \"owner1\" ");
        assert sqlBuilder.getArgs().length == 0;
    }

    @Test
    public void foreach_1() throws Exception {
        String config = "SELECT <include refid=\"alertConfigDetail_allColumns\"/> FROM alert_detail\n" +//
                "WHERE alert_detail.event_type IN\n" + //
                "<foreach collection=\"eventTypes\" item=\"eventType\" separator=\",\" open=\"(\" close=\")\">\n" +//
                "    #{eventType}\n" +//
                "</foreach>";
        Map<String, Object> ctx = CollectionUtils.asMap("eventTypes", Arrays.asList("a", "b", "c", "d", "e"));

        TestQueryContext context = new TestQueryContext();
        context.addMacro("alertConfigDetail_allColumns", "*");
        SqlConfig sqlConfig = new ConfigResolveByXmlSql().parseConfig("select", new HintsSet(), config);
        SqlBuilder sqlBuilder = sqlConfig.buildQuery(ctx, context);
        assert sqlBuilder.getSqlString().equals("SELECT * FROM alert_detail\nWHERE alert_detail.event_type IN\n(\n    ?\n,\n    ?\n,\n    ?\n,\n    ?\n,\n    ?\n)");
        assert sqlBuilder.getArgs().length == 5;
        assert ((SqlArg) sqlBuilder.getArgs()[0]).getValue().equals("a");
        assert ((SqlArg) sqlBuilder.getArgs()[1]).getValue().equals("b");
        assert ((SqlArg) sqlBuilder.getArgs()[2]).getValue().equals("c");
        assert ((SqlArg) sqlBuilder.getArgs()[3]).getValue().equals("d");
        assert ((SqlArg) sqlBuilder.getArgs()[4]).getValue().equals("e");
    }

    @Test
    public void if_1() throws Exception {
        String config = "select * from PROJECT_INFO\n" +//
                "where status = 2\n" + //
                "<if test=\"ownerID != null and ownerType !=null\">\n" +//
                "    and owner_id = #{ownerID}\n" + //
                "    and owner_type = #{ownerType}\n" + //
                "</if>\n" +//
                "order by name asc";
        Map<String, Object> ctx = CollectionUtils.asMap("ownerID", "123", "ownerType", "SYSTEM");

        SqlConfig sqlConfig = new ConfigResolveByXmlSql().parseConfig("select", new HintsSet(), config);
        SqlBuilder sqlBuilder = sqlConfig.buildQuery(ctx, new TestQueryContext());
        assert sqlBuilder.getSqlString().equals("select * from PROJECT_INFO\nwhere status = 2\n\n    and owner_id = ?\n    and owner_type = ?\n\norder by name asc");
        assert sqlBuilder.getArgs().length == 2;
        assert ((SqlArg) sqlBuilder.getArgs()[0]).getValue().equals("123");
        assert ((SqlArg) sqlBuilder.getArgs()[1]).getValue().equals("SYSTEM");
    }

    @Test
    public void if_2() throws Exception {
        String config = "select * from PROJECT_INFO\n" +//
                "where status = 2\n" + //
                "<if test=\"ownerID != null and ownerType !=null\">\n" +//
                "    and owner_id = #{ownerID}\n" + //
                "    and owner_type = #{ownerType}\n" + //
                "</if>\n" +//
                "order by name asc";
        Map<String, Object> ctx = CollectionUtils.asMap("ownerID", "123", "ownerType", null);

        SqlConfig sqlConfig = new ConfigResolveByXmlSql().parseConfig("select", new HintsSet(), config);
        SqlBuilder sqlBuilder = sqlConfig.buildQuery(ctx, new TestQueryContext());
        assert sqlBuilder.getSqlString().equals("select * from PROJECT_INFO\nwhere status = 2\n\norder by name asc");
        assert sqlBuilder.getArgs().length == 0;
    }

    @Test
    public void include_1() throws Exception {
        String config = "SELECT <include refid=\"alertConfigDetail_allColumns\"/> FROM alert_detail\n" +//
                "WHERE config_detail.event_type = #{eventType}";
        Map<String, Object> ctx = CollectionUtils.asMap("eventType", "123");

        TestQueryContext context = new TestQueryContext();
        context.addMacro("alertConfigDetail_allColumns", "*");
        SqlConfig sqlConfig = new ConfigResolveByXmlSql().parseConfig("select", new HintsSet(), config);
        SqlBuilder sqlBuilder = sqlConfig.buildQuery(ctx, context);
        assert sqlBuilder.getSqlString().equals("SELECT * FROM alert_detail\nWHERE config_detail.event_type = ?");
        assert sqlBuilder.getArgs().length == 1;
        assert ((SqlArg) sqlBuilder.getArgs()[0]).getValue().equals("123");
    }

    @Test
    public void selectKey_1() throws Exception {
        String config = "insert into test_user (\n" +//
                "    <include refid=\"user_do_allColumns\"/>\n) values (\n" + //
                "    #{user.name} , #{user.loginName}\n" + //
                ");\n" +//
                "<selectKey keyProperty=\"id\" order=\"AFTER\">\n" +//
                "    SELECT LAST_INSERT_ID()\n" +//
                "</selectKey>";
        UserInfo user = new UserInfo();
        user.setName("name");
        user.setLoginName("loginName");
        Map<String, Object> ctx = CollectionUtils.asMap("user", user);
        TestQueryContext context = new TestQueryContext();
        context.addMacro("user_do_allColumns", "name,login_name");

        SqlConfig sqlConfig = new ConfigResolveByXmlSql().parseConfig("insert", new HintsSet(), config);
        SqlBuilder sqlBuilder1 = sqlConfig.buildQuery(ctx, context);
        assert sqlBuilder1.getSqlString().equals("insert into test_user (\n    name,login_name\n) values (\n    ? , ?\n);\n");
        assert sqlBuilder1.getArgs().length == 2;
        assert ((SqlArg) sqlBuilder1.getArgs()[0]).getValue().equals("name");
        assert ((SqlArg) sqlBuilder1.getArgs()[1]).getValue().equals("loginName");

        SelectKeyConfig keyConfig = ((InsertConfig) sqlConfig).getSelectKey();
        SqlBuilder sqlBuilder2 = keyConfig.buildQuery(ctx, context);
        assert sqlBuilder2.getSqlString().trim().equals("SELECT LAST_INSERT_ID()");
        assert sqlBuilder2.getArgs().length == 0;
    }

    @Test
    public void set_1() throws Exception {
        String config = "UPDATE alert_users         \n" +//
                "<set>                              \n" +//
                "    <if test=\"name != null\">     \n" +//
                "        name = #{name},            \n" +//
                "    </if>                          \n" +//
                "    <if test=\"loginName != null\">\n" +//
                "        loginName = #{loginName},  \n" +//
                "    </if>                          \n" +//
                "    <if test=\"email != null\">    \n" +//
                "        email = #{email},          \n" +//
                "    </if>                          \n" +//
                "    <if test=\"seq != null\">      \n" +//
                "        seq = #{seq},              \n" +//
                "    </if>                          \n" +//
                "</set>                             \n" +//
                "WHERE uid = #{userUuid}            \n";
        UserInfo user = new UserInfo();
        user.setName("name");
        user.setLoginName("loginName");
        user.setUserUuid("abc");
        SqlArgSource ctx = new BeanSqlArgSource(user);

        SqlConfig sqlConfig = new ConfigResolveByXmlSql().parseConfig("update", new HintsSet(), config);
        SqlBuilder sqlBuilder = sqlConfig.buildQuery(ctx, new TestQueryContext());
        assert sqlBuilder.getSqlString().equals("UPDATE alert_users         \nset name = ?,            \n                              \n    \n        loginName = ?                              \nWHERE uid = ?            \n");
        assert sqlBuilder.getArgs().length == 3;
        assert ((SqlArg) sqlBuilder.getArgs()[0]).getValue().equals("name");
        assert ((SqlArg) sqlBuilder.getArgs()[1]).getValue().equals("loginName");
        assert ((SqlArg) sqlBuilder.getArgs()[2]).getValue().equals("abc");
    }

    @Test
    public void set_2() throws Exception {
        String config = "UPDATE alert_users         \n" +//
                "<set>                              \n" +//
                "    <if test=\"name != null\">     \n" +//
                "        name = #{name},            \n" +//
                "    </if>                          \n" +//
                "    <if test=\"loginName != null\">\n" +//
                "        loginName = #{loginName},  \n" +//
                "    </if>                          \n" +//
                "    <if test=\"email != null\">    \n" +//
                "        email = #{email},          \n" +//
                "    </if>                          \n" +//
                "    <if test=\"seq != null\">      \n" +//
                "        seq = #{seq},              \n" +//
                "    </if>                          \n" +//
                "</set>                             \n" +//
                "WHERE uid = #{userUuid}            \n";
        UserInfo user = new UserInfo();
        user.setUserUuid("abc");
        SqlArgSource ctx = new BeanSqlArgSource(user);

        SqlConfig sqlConfig = new ConfigResolveByXmlSql().parseConfig("update", new HintsSet(), config);
        SqlBuilder sqlBuilder = sqlConfig.buildQuery(ctx, new TestQueryContext());
        assert sqlBuilder.getSqlString().equals("UPDATE alert_users         \n                             \nWHERE uid = ?            \n");
        assert sqlBuilder.getArgs().length == 1;
        assert ((SqlArg) sqlBuilder.getArgs()[0]).getValue().equals("abc");
    }

    @Test
    public void where_1() throws Exception {
        String config = "SELECT * FROM BLOG\n" +//
                "<where>\n" + //
                "    <if test=\"name != null\">\n" +//
                "        and name = #{name}\n" +//
                "    </if>\n" + //
                "    <if test=\"loginName != null\">\n" +//
                "        and login_name like #{loginName}\n" +//
                "    </if>\n" +//
                "</where>";
        Map<String, Object> ctx = CollectionUtils.asMap("name", "name", "loginName", "loginName");

        SqlConfig sqlConfig = new ConfigResolveByXmlSql().parseConfig("select", new HintsSet(), config);
        SqlBuilder sqlBuilder = sqlConfig.buildQuery(ctx, new TestQueryContext());
        assert sqlBuilder.getSqlString().equals("SELECT * FROM BLOG\nwhere  name = ?\n    \n    \n        and login_name like ? ");
        assert sqlBuilder.getArgs().length == 2;
        assert ((SqlArg) sqlBuilder.getArgs()[0]).getValue().equals("name");
        assert ((SqlArg) sqlBuilder.getArgs()[1]).getValue().equals("loginName");
    }

    @Test
    public void where_2() throws Exception {
        String config = "SELECT * FROM BLOG\n" +//
                "<where>\n" + //
                "    <if test=\"name != null\">\n" +//
                "        and name = #{name}\n" +//
                "    </if>\n" + //
                "    <if test=\"loginName != null\">\n" +//
                "        and login_name like #{loginName}\n" +//
                "    </if>\n" +//
                "</where>";
        Map<String, Object> ctx = Collections.emptyMap();

        SqlConfig sqlConfig = new ConfigResolveByXmlSql().parseConfig("select", new HintsSet(), config);
        SqlBuilder sqlBuilder = sqlConfig.buildQuery(ctx, new TestQueryContext());
        assert sqlBuilder.getSqlString().equals("SELECT * FROM BLOG\n");
        assert sqlBuilder.getArgs().length == 0;
    }
}
