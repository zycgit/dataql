/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.parser.location;
/**
 * AST 和代码文本的位置关系
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-06-11
 */
public class LocationUtils {
    public static RuntimeLocation atRuntime(BlockLocation blockLocation, int methodAddress, int programAddress) {
        return new RuntimeLocation(blockLocation, methodAddress, programAddress);
    }

    public static RuntimeLocation unknownLocation() {
        BlockLocation blockLocation = new BlockLocation();
        blockLocation.setStartPosition(new CodeLocation(-1, -1));
        blockLocation.setEndPosition(new CodeLocation(-1, -1));
        return new RuntimeLocation(blockLocation, -1, -1);
    }

    public static BlockLocation atLocation(CodeLocation start, CodeLocation end) {
        BlockLocation info = new BlockLocation();
        info.setStartPosition(start == null ? new CodeLocation() : start);
        info.setEndPosition(end == null ? new CodeLocation() : end);
        return info;
    }
}
