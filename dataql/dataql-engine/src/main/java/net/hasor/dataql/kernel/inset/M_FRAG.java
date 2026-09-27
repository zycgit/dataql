/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.inset;
import net.hasor.dataql.kernel.*;
import net.hasor.dataql.kernel.mem.*;
import net.hasor.dataql.parser.location.RuntimeLocation;

/**
 * M_FRAG  // 加载一个 代码执行片段的执行器。
 * - 参数说明：共1参数；参数1：片段类型
 * - 栈行为：消费0，产出1
 * - 堆行为：无
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2019-12-14
 */
class M_FRAG implements InsetProcess {
    @Override
    public int getOpcode() {
        return M_FRAG;
    }

    @Override
    public void doWork(InstSequence sequence, DataHeap dataHeap, DataStack dataStack, EnvStack envStack, InsetProcessContext context) {
        RuntimeLocation location = sequence.programLocation();
        boolean isBach = sequence.currentInst().getBoolean(0);
        String fragmentType = sequence.currentInst().getString(1);
        FragmentProcess loadObject = context.findFragmentProcess(fragmentType);
        if (loadObject == null) {
            throw new QueryRuntimeException(location, fragmentType + " fragment undefine.");
        }

        RefFragmentCall fragmentCall = new RefFragmentCall(location, isBach, fragmentType, loadObject);
        dataStack.push(new RefCall(location, true, fragmentCall));
    }
}
