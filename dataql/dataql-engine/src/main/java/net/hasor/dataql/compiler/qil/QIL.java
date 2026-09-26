/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.compiler.qil;
import java.util.Collections;
import java.util.Map;
import net.hasor.cobble.StringUtils;

/**
 * Query intermediate language 中间查询语言
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-03
 */
public final class QIL {
    private final Instruction[][]      queueSet;
    private final Map<String, Integer> compilerVar;

    public QIL(Instruction[][] queueSet, Map<String, Integer> compilerVar) {
        this.queueSet = queueSet;
        this.compilerVar = compilerVar;
    }

    public Map<String, Integer> getCompilerVar() {
        return Collections.unmodifiableMap(this.compilerVar);
    }

    @Override
    public String toString() {
        StringBuilder strBuffer = new StringBuilder();
        for (int i = 0; i < this.queueSet.length; i++) {
            Instruction[] instList = this.queueSet[i];
            printInstList(i, instList, strBuffer);
        }
        return strBuffer.toString().trim();
    }

    private static void printInstList(int name, Instruction[] instList, StringBuilder strBuffer) {
        strBuffer.append("[");
        strBuffer.append(name);
        strBuffer.append("]\n");
        int length = String.valueOf(instList.length).length();
        for (int i = 0; i < instList.length; i++) {
            Instruction inst = instList[i];
            strBuffer.append("  #");
            strBuffer.append(StringUtils.leftPad(String.valueOf(i), length, '0'));
            strBuffer.append("  ");
            strBuffer.append(inst.toString());
            strBuffer.append("\n");
        }
        strBuffer.append("\n");
    }

    /** 方法总数 */
    public int iqlPoolSize() {
        return this.queueSet.length;
    }

    /** 方法的指令序列长度 */
    public int iqlSize(int address) {
        return this.queueSet[address].length;
    }

    /** 获取指令 */
    public Instruction instOf(int address, int index) {
        return this.queueSet[address][index];
    }

    /** 获取方法指令序列的迭代器 */
    public Instruction[] iqlArrays(int address) {
        return this.queueSet[address].clone();
    }
}
