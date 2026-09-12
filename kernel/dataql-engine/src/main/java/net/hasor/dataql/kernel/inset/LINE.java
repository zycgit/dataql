/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.inset;
import net.hasor.dataql.compiler.qil.Instruction;
import net.hasor.dataql.kernel.InsetProcess;
import net.hasor.dataql.kernel.InsetProcessContext;
import net.hasor.dataql.kernel.InstSequence;
import net.hasor.dataql.kernel.mem.DataHeap;
import net.hasor.dataql.kernel.mem.DataStack;
import net.hasor.dataql.kernel.mem.EnvStack;

/**
 * LINE    // 行号，无实际作用
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-19
 */
class LINE implements InsetProcess {
    @Override
    public int getOpcode() {
        return LINE;
    }

    @Override
    public void doWork(InstSequence sequence, DataHeap dataHeap, DataStack dataStack, EnvStack envStack, InsetProcessContext context) {
        Instruction inst = sequence.currentInst();
        Object[] arrays = inst.getArrays();
        if (arrays.length == 4) {
            sequence.updateCodeLocation(new int[] { //
                    inst.getInt(0), // startPosition - lineNumber
                    inst.getInt(1), // startPosition - columnNumber
                    inst.getInt(2), // endPosition - lineNumber
                    inst.getInt(3), // endPosition - columnNumber
            });
        } else {
            sequence.updateCodeLocation(new int[] { //
                    inst.getInt(0) // startPosition - lineNumber
            });
        }
    }
}
