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
package net.hasor.dataql.sqlproc.dynamic.rule;
import net.hasor.cobble.NumberUtils;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.ref.LinkedCaseInsensitiveMap;
import net.hasor.dataql.sqlproc.dynamic.*;
import net.hasor.dataql.sqlproc.internal.OgnlUtils;
import net.hasor.dataql.sqlproc.types.TypeHandler;

import java.sql.JDBCType;
import java.sql.SQLException;
import java.util.Map;

/**
 * 动态参数规则，负责动态 SQL 中 #{} 的解析。
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-06-05
 */
public class ArgRule implements SqlRule {
    public static final ArgRule INSTANCE             = new ArgRule();
    public static final String  CFG_KEY_MODE         = "mode";
    public static final String  CFG_KEY_JDBC_TYPE    = "jdbcType";
    public static final String  CFG_KEY_TYPE_HANDLER = "typeHandler";
    // for procedure
    public static final String  CFG_KEY_NAME         = "name";
    public static final String  CFG_KEY_TYPE_NAME    = "typeName";
    public static final String  CFG_KEY_SCALE        = "scale";

    private static final Map<String, Integer> JDBC_TYPE_MAP = new LinkedCaseInsensitiveMap<>();

    static {
        JDBC_TYPE_MAP.put("INT", JDBCType.INTEGER.getVendorTypeNumber());
        for (JDBCType typeElement : JDBCType.values()) {
            JDBC_TYPE_MAP.put(typeElement.name(), typeElement.getVendorTypeNumber());
        }
    }

    public static Map<String, String> parserConfig(String[] content, int start, int length) {
        Map<String, String> exprMap = new LinkedCaseInsensitiveMap<>();
        for (int i = start; i < length; i++) {
            if (i >= content.length) {
                break;
            }

            String data = content[i];
            String[] kv = data.split("=");
            if (kv.length != 2) {
                throw new IllegalArgumentException("analysisSQL failed, config must be 'key = value' , '" + content[i] + "' with '" + data + "'");
            }
            if (StringUtils.isNotBlank(kv[0])) {
                exprMap.put(kv[0].trim(), kv[1].trim());
            }
        }

        return exprMap;
    }

    @Override
    public boolean test(SqlArgSource data, QueryContext context, String activeExpr) {
        return true;
    }

    @Override
    public void executeRule(SqlArgSource data, QueryContext context, SqlBuilder sqlBuilder, String activeExpr, String ruleValue) throws SQLException {
        String[] testSplit = ruleValue.split(",");
        if (testSplit.length > 6 || testSplit.length == 0) {
            throw new IllegalArgumentException("analysisSQL failed, format error -> '#{valueExpr [,mode= IN|OUT|INOUT] [,jdbcType=INT] [,javaType=java.lang.String] [,typeHandler=YouTypeHandlerClassName]}'");
        }

        boolean noExpr = StringUtils.contains(testSplit[0], "=");
        String expr = noExpr ? "" : testSplit[0];
        Map<String, String> config = ArgRule.INSTANCE.parserConfig(testSplit, noExpr ? 0 : 1, testSplit.length);

        this.executeRule(data, context, sqlBuilder, expr, config);
    }

    public void executeRule(SqlArgSource data, QueryContext context, SqlBuilder sqlBuilder, String expr, Map<String, String> config) throws SQLException {
        SqlMode sqlMode = this.convertSqlMode((config != null) ? config.get(CFG_KEY_MODE) : null);
        Integer jdbcType = this.convertJdbcType((config != null) ? config.get(CFG_KEY_JDBC_TYPE) : null);
        String handlerType = (config != null) ? config.get(CFG_KEY_TYPE_HANDLER) : null;
        Object argValue = (sqlMode != null && !sqlMode.isIn() && sqlMode.isOut()) ? null : OgnlUtils.evalOgnl(expr, data);
        String asName = (config != null) ? config.getOrDefault(CFG_KEY_NAME, null) : null;
        String typeName = (config != null) ? config.getOrDefault(CFG_KEY_TYPE_NAME, null) : null;
        Integer scale = this.convertInteger((config != null) ? config.get(CFG_KEY_SCALE) : null);

        if (argValue instanceof SqlArg) {
            sqlBuilder.appendSql("?", argValue);
            return;
        }

        TypeHandler typeHandler = this.createTypeHandler(context, handlerType, argValue);
        SqlArg arg = new SqlArg(expr, argValue, sqlMode, jdbcType, typeHandler);
        arg.setAsName(asName);
        arg.setJdbcTypeName(typeName);
        arg.setScale(scale);

        sqlBuilder.appendSql("?", arg);
    }

    private SqlMode convertSqlMode(String sqlMode) {
        if (StringUtils.isNotBlank(sqlMode)) {
            for (SqlMode mode : SqlMode.values()) {
                if (mode.name().equalsIgnoreCase(sqlMode)) {
                    return mode;
                }
            }
        }
        return null;
    }

    private Integer convertJdbcType(String jdbcType) {
        if (NumberUtils.isNumber(jdbcType)) {
            return NumberUtils.createInteger(jdbcType);
        }

        if (StringUtils.isNotBlank(jdbcType)) {
            return JDBC_TYPE_MAP.get(jdbcType);
        }
        return null;
    }

    private Integer convertInteger(String jdbcType) {
        if (NumberUtils.isNumber(jdbcType)) {
            return NumberUtils.createInteger(jdbcType);
        }
        return null;
    }

    private TypeHandler createTypeHandler(QueryContext context, String handlerType, Object argValue) throws SQLException {
        if (StringUtils.isBlank(handlerType)) {
            return null;
        }

        // try load from cache
        TypeHandler typeHandler = context.getTypeRegistry().getHandlerByHandlerType(handlerType);
        if (typeHandler != null) {
            return typeHandler;
        }

        // handlerClass
        try {
            Class<?> handlerClass = context.loadClass(handlerType);
            return context.getTypeRegistry().createTypeHandler(handlerClass);
        } catch (ClassNotFoundException e) {
            throw new SQLException("handlerType '" + handlerType + "' ClassNotFoundException.");
        }

    }

    @Override
    public String toString() {
        return "arg [" + this.hashCode() + "]";
    }
}
