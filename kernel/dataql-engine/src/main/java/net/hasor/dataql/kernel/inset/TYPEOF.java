/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.inset;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.domain.DomainHelper;
import net.hasor.dataql.domain.ValueModel;
import net.hasor.dataql.kernel.InsetProcess;
import net.hasor.dataql.kernel.InsetProcessContext;
import net.hasor.dataql.kernel.InstSequence;
import net.hasor.dataql.kernel.QueryRuntimeException;
import net.hasor.dataql.kernel.mem.DataHeap;
import net.hasor.dataql.kernel.mem.DataStack;
import net.hasor.dataql.kernel.mem.EnvStack;
import static net.hasor.dataql.domain.TypeOfEnum.*;

/**
 * TYPEOF   // 计算表达式值的类型。
 * - 参数说明：共0参数；
 * - 栈行为：消费1，产出1，产出内容为：string、number、boolean、object、list、udf、null
 * - 堆行为：无
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-01-24
 */
class TYPEOF implements InsetProcess {
    @Override
    public int getOpcode() {
        return TYPEOF;
    }

    @Override
    public void doWork(InstSequence sequence, DataHeap dataHeap, DataStack dataStack, EnvStack envStack, InsetProcessContext context) {
        DataModel dataModel = DomainHelper.convertTo(dataStack.pop());
        if (dataModel.isObject()) {
            dataStack.push(Object.typeCode());
            return;
        }
        if (dataModel.isList()) {
            dataStack.push(List.typeCode());
            return;
        }
        if (dataModel.isUdf()) {
            dataStack.push(Udf.typeCode());
            return;
        }
        if (dataModel.isValue()) {
            ValueModel val = (ValueModel) dataModel;
            if (val.isNull()) {
                dataStack.push(Null.typeCode());
                return;
            }
            if (val.isNumber()) {
                dataStack.push(Number.typeCode());
                return;
            }
            if (val.isString()) {
                dataStack.push(String.typeCode());
                return;
            }
            if (val.isBoolean()) {
                dataStack.push(Boolean.typeCode());
                return;
            }
        }
        throw new QueryRuntimeException(sequence.programLocation(), "DataModel type is unknown.");
    }
}
