/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.rule;
import java.util.Map;
import java.util.Objects;
import net.hasor.cobble.ref.LinkedCaseInsensitiveMap;

/**
 * SqlBuildRule 注册器
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-06-05
 */
public class RuleRegistry {
    public static final RuleRegistry         DEFAULT = new RuleRegistry();
    private final       Map<String, SqlRule> ruleMap = new LinkedCaseInsensitiveMap<>();

    static {
        DEFAULT.register("if", IfRule.INSTANCE_IF);
        DEFAULT.register("ifin", new InRule(true));
        DEFAULT.register("iftext", new TextRule(true));
        DEFAULT.register("ifmacro", new MacroRule(true));
        DEFAULT.register("ifand", new AndRule(true));
        DEFAULT.register("ifor", new OrRule(true));
        DEFAULT.register("ifset", new SetRule(true));
        DEFAULT.register("in", InRule.INSTANCE);
        DEFAULT.register("text", TextRule.INSTANCE);
        DEFAULT.register("md5", MD5Rule.INSTANCE);
        DEFAULT.register("uuid32", UUID32Rule.INSTANCE);
        DEFAULT.register("uuid36", UUID36Rule.INSTANCE);
        DEFAULT.register("macro", MacroRule.INSTANCE);
        DEFAULT.register("and", AndRule.INSTANCE);
        DEFAULT.register("or", OrRule.INSTANCE);
        DEFAULT.register("set", SetRule.INSTANCE);

        DEFAULT.register("arg", ArgRule.INSTANCE);
        DEFAULT.register("case", CaseRule.INSTANCE);
        DEFAULT.register("when", WhenRule.INSTANCE_WHEN);
        DEFAULT.register("else", WhenRule.INSTANCE_ELSE);
        DEFAULT.register("pairs", PairsRule.INSTANCE);
    }

    public SqlRule findRule(String ruleName) {
        SqlRule rule = this.ruleMap.get(ruleName);
        if (rule == null && this != DEFAULT) {
            rule = DEFAULT.findRule(ruleName);
        }
        return rule;
    }

    /** 注册 SqlBuildRule */
    public void register(String ruleName, SqlRule rule) {
        this.ruleMap.put(ruleName, Objects.requireNonNull(rule, "rule is null."));
    }
}
