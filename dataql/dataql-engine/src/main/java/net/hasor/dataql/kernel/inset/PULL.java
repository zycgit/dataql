/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.inset;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.hasor.cobble.convert.ConverterUtils;
import net.hasor.dataql.domain.ListModel;
import net.hasor.dataql.domain.ValueModel;
import net.hasor.dataql.kernel.InsetProcess;
import net.hasor.dataql.kernel.InsetProcessContext;
import net.hasor.dataql.kernel.InstSequence;
import net.hasor.dataql.kernel.QueryRuntimeException;
import net.hasor.dataql.kernel.mem.DataHeap;
import net.hasor.dataql.kernel.mem.DataStack;
import net.hasor.dataql.kernel.mem.EnvStack;
import static net.hasor.dataql.domain.HintNames.INDEX_OVERFLOW;
import static net.hasor.dataql.domain.HintValue.*;

/**
 * PULL    // 栈顶元素是一个集合类型，获取集合的指定索引元素。（例：PULL 123）
 * - 参数说明：共1参数；参数1：元素位置(负数表示从后向前，正数表示从前向后)
 * - 栈行为：消费1，产出1
 * - 堆行为：无
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-19
 */
class PULL implements InsetProcess {
    @Override
    public int getOpcode() {
        return PULL;
    }

    @Override
    public void doWork(InstSequence sequence, DataHeap dataHeap, DataStack dataStack, EnvStack envStack, InsetProcessContext context) throws QueryRuntimeException {
        int point = 0;
        if (sequence.currentInst().getArrays().length > 0) {
            point = sequence.currentInst().getInt(0);
        } else {
            Object pointData = dataStack.pop();
            if (pointData instanceof ValueModel) {
                point = ((ValueModel) pointData).asInt();
            } else if (pointData instanceof Number) {
                point = ((Number) pointData).intValue();
            } else {
                point = (int) ConverterUtils.convert(Integer.TYPE, pointData);
            }
        }
        Object data = dataStack.pop();

        if (data == null) {
            dataStack.push(null);
            return;
        } else if (data instanceof ListModel) {
            data = ((ListModel) data).asOri();
        } else if (data.getClass().isArray()) {
            data = Arrays.asList((Object[]) data);
        }

        String indexOverflow = context.currentHints().getOrMap(INDEX_OVERFLOW.name(), val -> {
            return (val == null) ? INDEX_OVERFLOW_NEAR : val.toString();
        });

        if (!(data instanceof Collection)) {
            throw new QueryRuntimeException(sequence.programLocation(), "output data error, target type must be Collection.");
        }
        int size = ((Collection) data).size();
        if (point < 0) {
            // Resolve negative indexes first; -size points to the first element.
            point = size + point;
        }
        if (point < 0 || point >= size) {
            if (INDEX_OVERFLOW_THROW.equalsIgnoreCase(indexOverflow)) {
                throw new ArrayIndexOutOfBoundsException(point + " out of " + size);
            }
            if (INDEX_OVERFLOW_NULL.equalsIgnoreCase(indexOverflow) || size == 0) {
                // Return null as requested, or when no nearest element exists in an empty collection.
                dataStack.push(null);
                return;
            }
            // The near policy selects the first or last element when the index is out of bounds.
            point = Math.max(0, Math.min(point, size - 1));
        }

        Object pullData = null;
        if (data instanceof List) {
            pullData = ((List) data).get(point);
        } else {
            pullData = ((Collection) data).toArray()[point];
        }

        dataStack.push(pullData);
    }
}
