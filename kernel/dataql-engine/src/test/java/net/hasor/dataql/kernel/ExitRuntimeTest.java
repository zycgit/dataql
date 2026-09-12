/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel;
import net.hasor.dataql.AbstractTestResource;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.domain.HintValue;
import net.hasor.dataql.domain.ObjectModel;
import net.hasor.dataql.domain.ValueModel;
import net.hasor.dataql.host.Query;
import org.junit.Test;

public class ExitRuntimeTest extends AbstractTestResource implements HintValue {
    @Test
    public void return_1_Test() throws Exception {
        Query compilerQL = compilerQL("return 12 ,true + false;");
        QueryResult result = compilerQL.execute();
        //
        DataModel dataModel = result.getData();
        assert result.getCode() == 12;
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).asString().equals("truefalse");
        assert !result.isExit();
    }

    @Test
    public void return_2_Test() throws Exception {
        Query compilerQL = compilerQL("return 12 ,{};");
        QueryResult result = compilerQL.execute();
        //
        DataModel dataModel = result.getData();
        assert result.getCode() == 12;
        assert dataModel.isObject();
        assert ((ObjectModel) dataModel).size() == 0;
        assert !result.isExit();
    }

    @Test
    public void exit_1_Test() throws Exception {
        Query compilerQL = compilerQL("exit 12 ,true + false; exit false;");
        QueryResult result = compilerQL.execute();
        //
        DataModel dataModel = result.getData();
        assert result.getCode() == 12;
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).asString().equals("truefalse");
        assert result.isExit();
    }

    @Test
    public void exit_2_Test() throws Exception {
        Query compilerQL = compilerQL("exit 12 ,{}; exit false;");
        QueryResult result = compilerQL.execute();
        //
        DataModel dataModel = result.getData();
        assert result.getCode() == 12;
        assert dataModel.isObject();
        assert ((ObjectModel) dataModel).size() == 0;
        assert result.isExit();
    }

    @Test
    public void throw_1_Test() {
        try {
            Query compilerQL = compilerQL("throw 12 ,true + false;");
            compilerQL.execute();
            assert false;
        } catch (Exception e) {
            assert e instanceof ThrowRuntimeException;
            assert ((ThrowRuntimeException) e).getThrowCode() == 12;
            DataModel dataModel = ((ThrowRuntimeException) e).getResult();
            assert dataModel.isValue();
            assert ((ValueModel) dataModel).asString().equals("truefalse");
        }
    }

    @Test
    public void throw_2_Test() {
        try {
            Query compilerQL = compilerQL("throw 12 ,{}; exit false;");
            compilerQL.execute();
            assert false;
        } catch (Exception e) {
            assert e instanceof ThrowRuntimeException;
            assert ((ThrowRuntimeException) e).getThrowCode() == 12;
            DataModel dataModel = ((ThrowRuntimeException) e).getResult();
            assert dataModel.isObject();
            assert ((ObjectModel) dataModel).size() == 0;
        }
    }
}
