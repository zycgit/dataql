/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.mem;
import net.hasor.dataql.domain.*;
import net.hasor.dataql.kernel.InsetProcessContext;
import net.hasor.dataql.kernel.InstSequence;
import net.hasor.dataql.kernel.inset.OpcodesPool;

/**
 * 代理 Lambda 使其成为 UDF.
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class RefLambdaCall implements Udf {
    private final InstSequence        instSequence;
    private final DataHeap            dataHeap;
    private final EnvStack            envStack;
    private final InsetProcessContext context;

    public RefLambdaCall(InstSequence instSequence, DataHeap dataHeap, EnvStack envStack, InsetProcessContext context) {
        this.instSequence = instSequence;
        this.dataHeap = dataHeap;
        this.envStack = envStack;
        this.context = context;
    }

    @Override
    public Object call(Hints readOnly, UdfParams params) throws Throwable {
        DataStack cloneStack = new DataStack();
        cloneStack.push(new RefLambdaCallStruts(params.allParams()));

        InstSequence instSequence = this.instSequence.clone();
        OpcodesPool opcodesPool = OpcodesPool.defaultOpcodesPool();
        DataHeap dataHeap = new DataHeap(this.dataHeap);
        while (instSequence.hasNext()) {
            opcodesPool.doWork(     //
                    instSequence,   //
                    dataHeap,       //
                    cloneStack,     //
                    this.envStack,  //
                    this.context    //
            );
            instSequence.doNext(1);
        }

        DataModel result = cloneStack.getResult();
        if (cloneStack.getExitType() != ExitType.Throw) {
            return (result != null) ? result.unwrap() : DomainHelper.nullDomain();
        } else {
            throw new RefLambdaCallException(       //
                    instSequence.programLocation(), //
                    cloneStack.getResultCode(),     //
                    cloneStack.getResult()          //
            );
        }
    }
}
