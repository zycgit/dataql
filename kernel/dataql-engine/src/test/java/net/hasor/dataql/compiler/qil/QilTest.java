/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.compiler.qil;
import net.hasor.dataql.AbstractTestResource;
import net.hasor.dataql.domain.HintValue;
import net.hasor.dataql.host.Query;
import org.junit.Test;

public class QilTest extends AbstractTestResource implements HintValue {
    @Test
    public void errorLineTest_1() {
        try {
            Query compilerQL = compilerQL("assert false;");
            compilerQL.execute();
            assert false;
        } catch (Exception e) {
            assert e.getMessage().equalsIgnoreCase("[line 1:7~1:12 ,QIL 0:18] assert test failed.");
            assert e.getLocalizedMessage().equalsIgnoreCase("assert test failed.");
        }
    }
}
