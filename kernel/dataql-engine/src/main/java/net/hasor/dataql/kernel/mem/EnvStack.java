/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.mem;
import java.util.Stack;

/**
 * 栈数据
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2019-11-22
 */
public class EnvStack extends Stack<Object> {
    /** 从栈顶乡下获取指定深度位置的数据 */
    public Object peekOfDepth(int depth) {
        if (depth < 0) {
            throw new ArrayIndexOutOfBoundsException(depth);
        }
        if (depth >= elementCount) {
            throw new ArrayIndexOutOfBoundsException(depth);
        }
        return this.get(elementCount - depth - 1);
    }

    @Override
    public EnvStack clone() {
        return (EnvStack) super.clone();
    }
}
