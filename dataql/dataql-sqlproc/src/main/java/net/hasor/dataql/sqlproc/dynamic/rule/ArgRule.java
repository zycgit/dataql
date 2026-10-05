/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.rule;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.util.*;
import net.hasor.cobble.NumberUtils;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.ref.LinkedCaseInsensitiveMap;
import net.hasor.cobble.reflect.resolvable.ResolvableType;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.internal.OgnlUtils;
import net.hasor.dataql.sqlproc.dynamic.segment.DynamicParsed;
import net.hasor.dataql.sqlproc.types.SqlArg;
import net.hasor.dataql.sqlproc.types.SqlArgSource;
import net.hasor.dataql.sqlproc.types.SqlMode;
import net.hasor.dataql.sqlproc.types.TypeHandler;

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

    private static final Set<String>          CONFIG_KEYS;
    private static final Map<String, Integer> JDBC_TYPE_MAP = new LinkedCaseInsensitiveMap<>();

    static {
        Set<String> configKeys = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        configKeys.addAll(Arrays.asList(CFG_KEY_MODE, CFG_KEY_JDBC_TYPE, CFG_KEY_TYPE_HANDLER, //
                CFG_KEY_NAME, CFG_KEY_TYPE_NAME, CFG_KEY_SCALE));
        CONFIG_KEYS = Collections.unmodifiableSet(configKeys);

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
            if (StringUtils.isBlank(data)) {
                continue;
            }

            String[] kv = data.split("=", 2);
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
        String[] testSplit = DynamicParsed.splitByComma(ruleValue);
        if (testSplit.length > CONFIG_KEYS.size() + 1 || testSplit.length == 0) {
            throw new IllegalArgumentException("analysisSQL failed, format error -> '#{valueExpr [,mode= IN|OUT|INOUT] [,jdbcType=INT] [,typeHandler=YouTypeHandlerClassName]}'");
        }

        boolean noExpr = isConfigEntry(testSplit[0]);
        String expr = noExpr ? "" : testSplit[0];
        Map<String, String> config = ArgRule.parserConfig(testSplit, noExpr ? 0 : 1, testSplit.length);

        this.executeRule(data, context, sqlBuilder, expr, config);
    }

    /** Distinguish a configuration assignment from comparisons and quoted equals signs in OGNL. */
    public static boolean isConfigEntry(String content) {
        char quote = 0;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (quote != 0) {
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            if (c == '\'' || c == '"') {
                quote = c;
                continue;
            }
            if (c != '=') {
                continue;
            }

            char previous = i > 0 ? content.charAt(i - 1) : 0;
            char next = i + 1 < content.length() ? content.charAt(i + 1) : 0;
            if (previous == '=' || previous == '!' || previous == '<' || previous == '>' || next == '=') {
                continue;
            }

            String key = content.substring(0, i).trim();
            if (CONFIG_KEYS.contains(key)) {
                return true;
            }

            // Do not turn formerly invalid configuration into an expression that mutates parameter data.
            throw new IllegalArgumentException("unsupported parameter configuration or assignment expression: " + content);
        }
        return false;
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

        Class<?> handlerClass;
        try {
            handlerClass = context.loadClass(handlerType);
        } catch (ClassNotFoundException e) {
            throw new SQLException("handlerType '" + handlerType + "' ClassNotFoundException.");
        }

        if (argValue == null) {
            return context.getTypeRegistry().createTypeHandler(handlerClass);
        } else {
            return context.getTypeRegistry().createTypeHandler(handlerClass, ResolvableType.forType(argValue.getClass()));
        }
    }

    @Override
    public String toString() {
        return "arg [" + this.hashCode() + "]";
    }
}
