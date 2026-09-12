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
import net.hasor.dataql.domain.Udf;
import net.hasor.dataql.domain.ValueModel;
import net.hasor.dataql.host.Query;
import org.junit.Test;

public class OptRuntimeTest extends AbstractTestResource implements HintValue {
    @Test
    public void opt_bool_1_Test() throws Exception {
        Object[] obj = new Object[] { (Udf) (readOnly, params) -> {
            return readOnly.getHint("abc");
        } };
        //
        Query compilerQL = compilerQL("hint abc = true; return ${_0}()");
        DataModel dataModel = compilerQL.execute(obj).getData();
        //
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).asBoolean();
    }

    @Test
    public void opt_bool_2_Test() throws Exception {
        Object[] obj = new Object[] { (Udf) (readOnly, params) -> {
            return readOnly.getHint("abc");
        } };
        //
        Query compilerQL = compilerQL("hint abc = false; return ${_0}()");
        DataModel dataModel = compilerQL.execute(obj).getData();
        //
        assert dataModel.isValue();
        assert !((ValueModel) dataModel).asBoolean();
    }

    @Test
    public void opt_num_1_Test() throws Exception {
        Object[] obj = new Object[] { (Udf) (readOnly, params) -> {
            return readOnly.getHint("abc");
        } };
        //
        Query compilerQL = compilerQL("hint abc = 123; return ${_0}()");
        DataModel dataModel = compilerQL.execute(obj).getData();
        //
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).isInt();
        assert ((ValueModel) dataModel).asInt() == 123;
    }

    @Test
    public void opt_num_2_Test() throws Exception {
        Object[] obj = new Object[] { (Udf) (readOnly, params) -> {
            return readOnly.getHint("abc");
        } };
        //
        Query compilerQL = compilerQL("hint MIN_INTEGER_WIDTH = 'byte'; hint abc = 123; return ${_0}()");
        DataModel dataModel = compilerQL.execute(obj).getData();
        //
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).isByte();
        assert ((ValueModel) dataModel).asByte() == 123;
    }

    @Test
    public void opt_num_3_Test() throws Exception {
        Object[] obj = new Object[] { (Udf) (readOnly, params) -> {
            return readOnly.getHint("abc");
        } };
        //
        Query compilerQL = compilerQL("hint MIN_INTEGER_WIDTH = 'byte'; hint abc = 1234; return ${_0}()");
        DataModel dataModel = compilerQL.execute(obj).getData();
        //
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).isShort();
        assert ((ValueModel) dataModel).asShort() == 1234;
    }

    @Test
    public void opt_num_4_Test() throws Exception {
        Object[] obj = new Object[] { (Udf) (readOnly, params) -> {
            return readOnly.getHint("abc");
        } };
        //
        Query compilerQL = compilerQL("hint abc = 0xabcdef; return ${_0}()");
        DataModel dataModel = compilerQL.execute(obj).getData();
        //
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).isInt();
        assert ((ValueModel) dataModel).asInt() == 11259375;
    }

    @Test
    public void opt_str_1_Test() throws Exception {
        Object[] obj = new Object[] { (Udf) (readOnly, params) -> {
            return readOnly.getHint("abc");
        } };
        //
        Query compilerQL = compilerQL("hint abc = 'abc'; return ${_0}()");
        DataModel dataModel = compilerQL.execute(obj).getData();
        //
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).isString();
        assert ((ValueModel) dataModel).asString().equals("abc");
    }

    @Test
    public void opt_null_1_Test() throws Exception {
        Object[] obj = new Object[] { (Udf) (readOnly, params) -> {
            return readOnly.getHint("abc");
        } };
        //
        Query compilerQL = compilerQL("hint abc = null; return ${_0}()");
        DataModel dataModel = compilerQL.execute(obj).getData();
        //
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).isNull();
    }

    @Test
    public void opt_declare_keeps_external_hint_Test() throws Exception {
        Object[] obj = new Object[] { (Udf) (readOnly, params) -> {
            return readOnly.getHint("abc");
        } };
        //
        Query compilerQL = compilerQL("hint abc; return ${_0}()");
        compilerQL.setHint("abc", "external");
        DataModel dataModel = compilerQL.execute(obj).getData();
        //
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).isString();
        assert ((ValueModel) dataModel).asString().equals("external");
    }

    @Test
    public void opt_null_removes_external_hint_Test() throws Exception {
        Object[] obj = new Object[] { (Udf) (readOnly, params) -> {
            return readOnly.getHint("abc");
        } };
        //
        Query compilerQL = compilerQL("hint abc = null; return ${_0}()");
        compilerQL.setHint("abc", "external");
        DataModel dataModel = compilerQL.execute(obj).getData();
        //
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).isNull();
    }
}
