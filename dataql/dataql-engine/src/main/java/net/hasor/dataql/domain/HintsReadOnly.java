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
 * 用于封装 Hint。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class HintsReadOnly implements Hints {
    private final Hints target;

    public HintsReadOnly(Hints target) {
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
        throw new UnsupportedOperationException("readOnly.");
    }

    @Override
    public void setHint(String hintName, Object value) {
        throw new UnsupportedOperationException("readOnly.");
    }
}
