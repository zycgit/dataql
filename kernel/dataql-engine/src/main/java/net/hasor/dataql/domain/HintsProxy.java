/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.domain;
import java.util.Objects;

/**
 * Hints 代理基类。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2026-07-09
 */
public class HintsProxy implements Hints {
    protected final Hints target;

    public HintsProxy(Hints target) {
        this.target = Objects.requireNonNull(target, "target is null.");
    }

    @Override
    public String[] getHints() {
        return this.target.getHints();
    }

    @Override
    public Object getHint(String optionKey) {
        return this.target.getHint(optionKey);
    }

    @Override
    public void removeHint(String optionKey) {
        this.target.removeHint(optionKey);
    }

    @Override
    public void setHint(String hintName, String value) {
        this.target.setHint(hintName, value);
    }

    @Override
    public void setHint(String hintName, Number value) {
        this.target.setHint(hintName, value);
    }

    @Override
    public void setHint(String hintName, boolean value) {
        this.target.setHint(hintName, value);
    }
}
