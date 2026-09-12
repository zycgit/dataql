/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.parser.location;
/**
 * 带有运行时数据的BlockLocation
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-06-11
 */
public class RuntimeLocation extends BlockLocation {
    private int methodAddress  = -1; // 方法地址
    private int programAddress = -1; // 执行指针

    RuntimeLocation(BlockLocation blockLocation, int methodAddress, int programAddress) {
        setStartPosition(blockLocation.getStartPosition());
        setEndPosition(blockLocation.getEndPosition());
        this.methodAddress = methodAddress;
        this.programAddress = programAddress;
    }

    public int getMethodAddress() {
        return this.methodAddress;
    }

    public int getProgramAddress() {
        return this.programAddress;
    }

    @Override
    public String toString() {
        return super.toString() + " ,QIL " + this.methodAddress + ":" + this.programAddress;
    }
}
