/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel;
import net.hasor.dataql.AbstractTestResource;
import net.hasor.dataql.domain.*;
import net.hasor.dataql.host.Query;
import org.junit.Test;

public class LambdaRuntimeTest extends AbstractTestResource implements HintValue {
    @Test
    public void lambda_1_Test() throws Exception {
        Query compilerQL = compilerQL("var sex_str = (sex) -> return (sex == 'F') ? '男' : '女' ; return [sex_str(${_0}),sex_str(${_1})]");
        DataModel dataModel = compilerQL.execute(new Object[] { "F", "M" }).getData();
        assert dataModel.isList();
        assert ((ListModel) dataModel).getValue(0).asString().equals("男");
        assert ((ListModel) dataModel).getValue(1).asString().equals("女");
    }

    @Test
    public void lambda_2_Test() throws Exception {
        Query compilerQL = compilerQL("var a = 10 ; var foo = () -> return a ; return foo()");
        DataModel dataModel = compilerQL.execute().getData();
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).asInt() == 10;
    }

    @Test
    public void lambda_3_Test() throws Exception {
        Query compilerQL = compilerQL("var a = 10 ; var foo = () -> { var a = 12; return a; } ; return foo()");
        DataModel dataModel = compilerQL.execute().getData();
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).asInt() == 12;
    }

    @Test
    public void lambda_4_Test() throws Exception {
        Query compilerQL = compilerQL("var a = 10 ; var foo = () -> { return a; } ; var a = 12; return foo()");
        DataModel dataModel = compilerQL.execute().getData();
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).asInt() == 12;
    }

    //    @Test
    //    public void lambda_5_Test() throws Exception {
    //        AppContext appContext = Hasor.create().build(apiBinder -> {
    //            apiBinder.bindType(DemoUdf.class).idWith(DemoUdf.class.getName());
    //        });
    //        Finder finder = new Finder() {
    //            @Override
    //            public Object findBean(Class<?> beanType) {
    //                return appContext.getInstance(beanType);
    //            }
    //        };
    //        //
    //        Query compilerQL = compilerQL("import '" + DemoUdf.class.getName() + "' as foo; return foo().name", finder);
    //        DataModel dataModel = compilerQL.execute().getData();
    //        assert dataModel.isValue();
    //        assert ((ValueModel) dataModel).asString().equals("马三");
    //    }

    //    @Test
    //    public void lambda_5_1_Test() throws Exception {
    //        AppContext appContext = Hasor.create().build(apiBinder -> {
    //        });
    //        Finder finder = new Finder() {
    //            @Override
    //            public Object findBean(Class<?> beanType) {
    //                return appContext.getInstance(beanType);
    //            }
    //        };
    //        //
    //        Query compilerQL = compilerQL("import '" + DemoUdf.class.getName() + "' as foo; return foo().name", finder);
    //        DataModel dataModel = compilerQL.execute().getData();
    //        assert dataModel.isValue();
    //        assert ((ValueModel) dataModel).asString().equals("马三");
    //    }

    @Test
    public void lambda_6_Test() throws Throwable {
        Query compilerQL = compilerQL("var a = 10 ; var foo = () -> { return a; } ; return foo");
        DataModel dataModel = compilerQL.execute().getData();
        assert dataModel.isUdf();
        DataModel dat = ((UdfModel) dataModel).call(null, () -> new Object[0]);
        assert dat.isValue();
        assert ((ValueModel) dat).asInt() == 10;
    }
}
