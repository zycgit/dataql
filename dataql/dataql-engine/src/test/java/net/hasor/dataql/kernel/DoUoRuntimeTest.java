/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel;
import java.util.HashMap;
import java.util.Map;
import net.hasor.cobble.CollectionUtils;
import net.hasor.dataql.AbstractTestResource;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.domain.DomainHelper;
import net.hasor.dataql.domain.HintValue;
import net.hasor.dataql.domain.ValueModel;
import net.hasor.dataql.host.Query;
import org.junit.Test;

public class DoUoRuntimeTest extends AbstractTestResource implements HintValue {
    @Test
    public void do_plus_1_Test() throws Exception {
        Query compilerQL = compilerQL("return true + false;");
        DataModel dataModel = compilerQL.execute().getData();
        //
        assert dataModel.isValue();
        assert !((ValueModel) dataModel).isBoolean();
        assert ((ValueModel) dataModel).isString();
        assert ((ValueModel) dataModel).asString().equals("truefalse");
    }

    @Test
    public void do_plus_2_Test() throws Exception {
        Query compilerQL = compilerQL("return 12 + 12;");
        DataModel dataModel = compilerQL.execute().getData();
        //
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).isNumber();
        assert ((ValueModel) dataModel).asInt() == 24;
    }

    @Test
    public void do_plus_3_Test() throws Exception {
        Query compilerQL = compilerQL("return 2 + 3 * 4;");
        compilerQL.setHint(MIN_INTEGER_WIDTH, MIN_INTEGER_WIDTH_BYTE);
        DataModel dataModel = compilerQL.execute().getData();
        //
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).isByte();
        assert ((ValueModel) dataModel).asByte() == 14;
    }

    @Test
    public void do_plus_4_Test() throws Exception {
        Query compilerQL = compilerQL("return (2 + 3) * 4;");
        DataModel dataModel = compilerQL.execute().getData();
        //
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).isNumber();
        assert ((ValueModel) dataModel).asInt() == 20;
    }

    @Test
    public void do_minus_1_Test() throws Exception {
        Query compilerQL = compilerQL("return (2 + 3) - 0xF;"); // 0xF = 10
        DataModel dataModel = compilerQL.execute().getData();
        //
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).isNumber();
        assert ((ValueModel) dataModel).asInt() == -10;
    }

    @Test
    public void do_minus_2_Test() throws Exception {
        Map<String, Object> objectMap = new HashMap<String, Object>() {{
            put("a", DomainHelper.convertTo(12));
            put("b", DomainHelper.convertTo(6));
        }};
        //
        Query compilerQL = compilerQL("return ${a}-${b};");
        DataModel dataModel = compilerQL.execute(objectMap).getData();
        //
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).isNumber();
        assert ((ValueModel) dataModel).asInt() == 6;
    }

    @Test
    public void uo_minus_1_Test() throws Exception {
        Query compilerQL = compilerQL("return - 1;");
        DataModel dataModel = compilerQL.execute().getData();
        //
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).isNumber();
        assert ((ValueModel) dataModel).asInt() == -1;
    }

    @Test
    public void uo_minus_2_Test() throws Exception {
        Query compilerQL = compilerQL("var a=123 return -a;");
        DataModel dataModel = compilerQL.execute().getData();
        //
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).isNumber();
        assert ((ValueModel) dataModel).asInt() == -123;
    }

    @Test
    public void uo_not_1_Test() throws Exception {
        Query compilerQL = compilerQL("return !true;");
        DataModel dataModel = compilerQL.execute().getData();
        //
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).isBoolean();
        assert !((ValueModel) dataModel).asBoolean();
    }

    @Test
    public void uo_not_2_Test() throws Exception {
        Query compilerQL = compilerQL("var a=true return !a;");
        DataModel dataModel = compilerQL.execute().getData();
        //
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).isBoolean();
        assert !((ValueModel) dataModel).asBoolean();
    }

    @Test
    public void uo_not_3_Test() throws Exception {
        Map<String, Object> objectMap = new HashMap<String, Object>() {{
            put("a", DomainHelper.convertTo(true));
        }};
        //
        Query compilerQL = compilerQL("return !${a};");
        DataModel dataModel = compilerQL.execute(objectMap).getData();
        //
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).isBoolean();
        assert !((ValueModel) dataModel).asBoolean();
    }

    @Test
    public void do_div_1_Test() throws Exception {
        Query compilerQL = compilerQL("hint MIN_DECIMAL_WIDTH ='big' ; hint MIN_INTEGER_WIDTH = 'big' ;return 0.0 / 0;");
        DataModel dataModel = compilerQL.execute().getData();
        //
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).isNumber();
        assert ((ValueModel) dataModel).asString().equals("Infinity");
    }

    @Test
    public void do_ternaryExpr_1_Test() throws Exception {
        Query compilerQL = compilerQL("return ${currentPage} == \"\" ? 2 : ${currentPage};");
        DataModel model1 = compilerQL.execute(CollectionUtils.asMap("currentPage", "")).getData();
        DataModel model2 = compilerQL.execute(CollectionUtils.asMap("currentPage", 22)).getData();

        assert model1.isValue() && ((ValueModel) model1).isNumber() && ((ValueModel) model1).asString().equals("2");
        assert model2.isValue() && ((ValueModel) model2).isNumber() && ((ValueModel) model2).asString().equals("22");
    }

}
