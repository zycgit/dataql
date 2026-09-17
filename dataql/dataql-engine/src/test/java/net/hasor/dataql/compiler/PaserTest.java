/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.compiler;
import java.io.IOException;
import net.hasor.dataql.AbstractTestResource;
import org.junit.Test;

/**
 * 测试用例
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-19
 */
public class PaserTest extends AbstractTestResource {
    @Test
    public void testPaser_1() {
        try {
            CompilerHelper.queryParser("return ${a} -1");
            assert false;
        } catch (Exception e) {
            assert e.getMessage().contains("no viable alternative at input");
        }
    }

    @Test
    public void testPaser_2() throws IOException {
        CompilerHelper.queryParser("return a == b ? c : d");
        assert true;
    }

    @Test
    public void testPaser_3() throws IOException {
        CompilerHelper.queryParser("return 123,a == b ? c : d");
        assert true;
    }
}
