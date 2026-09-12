/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.mem;
import java.util.Stack;
import net.hasor.dataql.domain.DataModel;

/**
 * 栈数据
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2019-11-22
 */
public class DataStack extends Stack<Object> {
    private int       resultCode = 0;
    private DataModel result     = null;
    private ExitType  exitType   = null;

    public int getResultCode() {
        return resultCode;
    }

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    public DataModel getResult() {
        return result;
    }

    public void setResult(DataModel result) {
        this.result = result;
    }

    public ExitType getExitType() {
        return exitType;
    }

    public void setExitType(ExitType exitType) {
        this.exitType = exitType;
    }

    @Override
    public DataStack clone() {
        DataStack dataStack = new DataStack();
        dataStack.addAll(this);
        dataStack.resultCode = this.resultCode;
        dataStack.result = this.result;
        dataStack.exitType = this.exitType;
        return dataStack;
    }
}
