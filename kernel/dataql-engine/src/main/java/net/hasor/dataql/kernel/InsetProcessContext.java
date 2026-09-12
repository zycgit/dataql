/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel;
import java.util.Map;
import java.util.Stack;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.kernel.operator.OperatorManager;
import net.hasor.dataql.kernel.operator.OperatorProcess;

/**
 * 指令执行器接口
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-14
 */
public class InsetProcessContext implements CustomizeScope {
    private final static OperatorManager opeManager = OperatorManager.defaultManager();
    private final        long            startTime  = System.currentTimeMillis();
    private final        CustomizeScope  customizeScope;
    private final        Finder          finder;
    private final        Stack<HintsSet> hintStack  = new Stack<>();

    InsetProcessContext(CustomizeScope customizeScope, Finder finder) {
        if (finder == null) {
            throw new NullPointerException("finder is null");
        }
        this.customizeScope = customizeScope;
        this.finder = finder;
        this.hintStack.push(new HintsSet());
    }

    public Hints currentHints() {
        return this.hintStack.peek();
    }

    public void createHintStack() {
        HintsSet hintsSet = new HintsSet();
        hintsSet.setHints(this.hintStack.peek());
        this.hintStack.push(hintsSet);
    }

    public void dropHintStack() {
        if (this.hintStack.size() > 1) {
            this.hintStack.pop();
        }
    }

    public long executionTime() {
        return System.currentTimeMillis() - this.startTime;
    }

    public Finder getFinder() {
        return finder;
    }

    /** 查找一元运算执行器 */
    public OperatorProcess findUnaryOperator(String unarySymbol, Class<?> fstType) {
        return opeManager.findUnaryProcess(unarySymbol, fstType);
    }

    /** 查找二元运算执行器 */
    public OperatorProcess findDyadicOperator(String dyadicSymbol, Class<?> fstType, Class<?> secType) {
        return opeManager.findDyadicProcess(dyadicSymbol, fstType, secType);
    }

    /** 获取环境数据，symbol 可能的值有：@、#、$。其中 # 为默认 */
    public Map<String, ?> findCustomizeEnvironment(String symbol) {
        if (this.customizeScope == null) {
            return null;
        }
        return this.customizeScope.findCustomizeEnvironment(symbol);
    }

    public Object loadObject(String udfType) throws ClassNotFoundException {
        return this.finder.findBean(udfType);
    }

    public FragmentProcess findFragmentProcess(String fragmentType) {
        return this.finder.findFragmentProcess(fragmentType);
    }
}
