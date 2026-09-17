/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.compiler.qil;
/**
 * Label
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class Label {
    private final String  labelID;
    private       Integer index;

    Label(int labelID) {
        this.labelID = "label_" + labelID;
    }

    public String getID() {
        return this.labelID;
    }

    public Integer getIndex() {
        return this.index;
    }

    @Override
    public String toString() {
        return this.index == null ? "null" : this.index.toString();
    }

    void updateIndex(int index) {
        this.index = index;
    }
}
