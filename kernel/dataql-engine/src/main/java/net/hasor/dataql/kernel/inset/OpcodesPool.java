/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.inset;
import java.util.function.Supplier;
import net.hasor.cobble.provider.SingleProvider;
import net.hasor.dataql.kernel.InsetProcess;
import net.hasor.dataql.kernel.InsetProcessContext;
import net.hasor.dataql.kernel.InstSequence;
import net.hasor.dataql.kernel.QueryRuntimeException;
import net.hasor.dataql.kernel.mem.DataHeap;
import net.hasor.dataql.kernel.mem.DataStack;
import net.hasor.dataql.kernel.mem.EnvStack;
import net.hasor.dataql.parser.location.RuntimeLocation;

/**
 * 指令池
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-19
 */
public class OpcodesPool {
    private final InsetProcess[] processes = new InsetProcess[255];

    private void addInsetProcess(InsetProcess inst) {
        this.processes[inst.getOpcode()] = inst;
    }

    public void doWork(InstSequence sequence, DataHeap dataHeap, DataStack dataStack, EnvStack envStack, InsetProcessContext context) throws QueryRuntimeException {
        RuntimeLocation location = sequence.programLocation();
        try {
            InsetProcess process = this.processes[sequence.currentInst().getInstCode()];
            process.doWork(sequence, dataHeap, dataStack, envStack, context);
        } catch (Exception e) {
            QueryRuntimeException ire = null;
            if (e instanceof QueryRuntimeException) {
                ire = (QueryRuntimeException) e;
            } else {
                ire = new QueryRuntimeException(location, e);
            }
            throw ire;
        }
    }

    private static final Supplier<OpcodesPool> operatorManager = new SingleProvider<>(OpcodesPool::initPool);

    public static OpcodesPool defaultOpcodesPool() {
        return operatorManager.get();
    }

    private static OpcodesPool initPool() {
        OpcodesPool pool = new OpcodesPool();
        //
        pool.addInsetProcess(new LDC_B());
        pool.addInsetProcess(new LDC_D());
        pool.addInsetProcess(new LDC_S());
        pool.addInsetProcess(new LDC_N());
        pool.addInsetProcess(new NEW_O());
        pool.addInsetProcess(new NEW_A());
        //
        pool.addInsetProcess(new LOAD());
        pool.addInsetProcess(new STORE());
        pool.addInsetProcess(new GET());
        pool.addInsetProcess(new PUT());
        pool.addInsetProcess(new PULL());
        pool.addInsetProcess(new PUSH());
        pool.addInsetProcess(new COPY());
        //
        pool.addInsetProcess(new RETURN());
        pool.addInsetProcess(new EXIT());
        pool.addInsetProcess(new THROW());
        //
        pool.addInsetProcess(new UO());
        pool.addInsetProcess(new DO());
        pool.addInsetProcess(new TYPEOF());
        //
        pool.addInsetProcess(new IF());
        pool.addInsetProcess(new GOTO());
        pool.addInsetProcess(new HINT());
        pool.addInsetProcess(new HINT_D());
        pool.addInsetProcess(new HINT_S());
        pool.addInsetProcess(new POP());
        pool.addInsetProcess(new LOAD_C());
        pool.addInsetProcess(new E_PUSH());
        pool.addInsetProcess(new E_POP());
        pool.addInsetProcess(new E_LOAD());
        pool.addInsetProcess(new CAST_I());
        pool.addInsetProcess(new CAST_O());
        //
        pool.addInsetProcess(new CALL());
        pool.addInsetProcess(new M_REF());
        pool.addInsetProcess(new M_DEF());
        pool.addInsetProcess(new M_TYP());
        pool.addInsetProcess(new M_FRAG());
        pool.addInsetProcess(new LOCAL());
        //
        pool.addInsetProcess(new LABEL());
        pool.addInsetProcess(new LINE());
        return pool;
    }
}
