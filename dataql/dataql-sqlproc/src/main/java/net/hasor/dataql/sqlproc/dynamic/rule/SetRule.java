/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.rule;
/**
 * 如果参数不为空，则生成 'column = ?' 或者 ', column = ?' 。
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-06-05
 */
public class SetRule extends ConditionRule {
    public static final SqlRule INSTANCE = new SetRule(false);

    public SetRule(boolean usingIf) {
        super(usingIf, new String[] { "set", "," }, "set", "set ", ", ");
    }

    @Override
    protected String name() {
        return this.usingIf ? "ifset" : "set";
    }

    @Override
    protected boolean allowNullValue() {
        return true;
    }
}
