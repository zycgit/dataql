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
import net.hasor.dataql.domain.ListModel;
import net.hasor.dataql.domain.ObjectModel;
import net.hasor.dataql.host.Query;
import org.junit.Test;

public class ObjectRuntimeTest extends AbstractTestResource implements HintValue {
    @Test
    public void obj_1_Test() throws Exception {
        Query compilerQL = compilerQL("return {'a':'abc-true','b':'bcd-false'}");
        DataModel dataModel = compilerQL.execute().getData();
        //
        assert dataModel.isObject();
        assert ((ObjectModel) dataModel).getValue("a").asString().equals("abc-true");
        assert ((ObjectModel) dataModel).getValue("b").asString().equals("bcd-false");
    }

    @Test
    public void obj_2_Test() throws Exception {
        Query compilerQL = compilerQL("return {'a':'abc-true','b': { 'c' :'bcd-false','d':true }}");
        DataModel dataModel = compilerQL.execute().getData();
        //
        assert dataModel.isObject();
        assert ((ObjectModel) dataModel).getValue("a").asString().equals("abc-true");
        assert ((ObjectModel) dataModel).isObject("b");
        assert ((ObjectModel) dataModel).getObject("b").getValue("c").asString().equals("bcd-false");
        assert ((ObjectModel) dataModel).getObject("b").getValue("d").asBoolean();
    }

    @Test
    public void list_1_Test() throws Exception {
        Query compilerQL = compilerQL("return [{},true,123,0x123]");
        DataModel dataModel = compilerQL.execute().getData();
        //
        assert dataModel.isList();
        assert ((ListModel) dataModel).isObject(0);
        assert ((ListModel) dataModel).getObject(0).size() == 0;
        assert ((ListModel) dataModel).getValue(1).asBoolean();
        assert ((ListModel) dataModel).getValue(2).asInt() == 123;
        assert ((ListModel) dataModel).getValue(3).asInt() == 0x123;
    }

    @Test
    public void list_2_Test() throws Exception {
        Query compilerQL = compilerQL("return {'a':'abc-true','b': [ {'c' :'bcd-false'},true]}");
        DataModel dataModel = compilerQL.execute().getData();
        //
        assert dataModel.isObject();
        assert ((ObjectModel) dataModel).getValue("a").asString().equals("abc-true");
        assert ((ObjectModel) dataModel).isList("b");
        assert ((ObjectModel) dataModel).getList("b").isObject(0);
        assert ((ObjectModel) dataModel).getList("b").getObject(0).getValue("c").asString().equals("bcd-false");
        assert ((ObjectModel) dataModel).getList("b").isValue(1);
        assert ((ObjectModel) dataModel).getList("b").getValue(1).asBoolean();
    }
}
