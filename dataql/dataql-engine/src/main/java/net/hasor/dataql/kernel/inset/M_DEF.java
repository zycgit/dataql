/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.inset;
import net.hasor.dataql.domain.Udf;
import net.hasor.dataql.kernel.InsetProcess;
import net.hasor.dataql.kernel.InsetProcessContext;
import net.hasor.dataql.kernel.InstSequence;
import net.hasor.dataql.kernel.QueryRuntimeException;
import net.hasor.dataql.kernel.mem.*;
import net.hasor.dataql.parser.location.RuntimeLocation;

/**
 * M_DEF   // 函数定义，将栈顶元素转换为 UDF
 * - 参数说明：共0参数；
 * - 栈行为：消费1，产出1
 * - 堆行为：无
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-19
 */
class M_DEF implements InsetProcess {
    @Override
    public int getOpcode() {
        return M_DEF;
    }

    @Override
    public void doWork(InstSequence sequence, DataHeap dataHeap, DataStack dataStack, EnvStack envStack, InsetProcessContext context) throws QueryRuntimeException {
        RuntimeLocation location = sequence.programLocation();
        Object refCall = dataStack.pop();
        if (refCall == null) {
            throw new QueryRuntimeException(location, "target is null.");
        }
        if (!(refCall instanceof Udf)) {
            throw new QueryRuntimeException(location, "target or Property is not UDF.");
        }
        boolean innerUDF = refCall instanceof RefFragmentCall || refCall instanceof RefLambdaCall;
        refCall = new RefCall(location, !innerUDF, (Udf) refCall);
        dataStack.push(refCall);
    }
}
