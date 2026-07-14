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
