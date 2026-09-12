/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.compiler;
import java.util.HashSet;
import java.util.Set;

/**
 * DataQL 编译参数。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-06-23
 */
public class CompilerArguments {
    /** 调试模式：编译的结果比较大，埋入的信息较多。 */
    public static final CompilerArguments DEBUG        = new CompilerArguments() {{
        setCodeLocation(CodeLocationEnum.TERM);
    }};
    /** 默认模式：一般性编译优化，不贵追求极致编译性能 */
    public static final CompilerArguments DEFAULT      = new CompilerArguments() {{
        setCodeLocation(CodeLocationEnum.TERM);
    }};
    /** 快速模式：最小化编译结果，极致的运行性能为目标 */
    public static final CompilerArguments FAST         = new CompilerArguments() {{
        setCodeLocation(CodeLocationEnum.NONE);
    }};
    //
    //
    private final       Set<String>       compilerVar  = new HashSet<>();
    private             CodeLocationEnum  codeLocation = CodeLocationEnum.LINE;

    public CompilerArguments copyAsNew() {
        CompilerArguments arguments = new CompilerArguments();
        arguments.compilerVar.addAll(this.compilerVar);
        arguments.codeLocation = this.codeLocation;
        return arguments;
    }

    public enum CodeLocationEnum {
        /** 行定位信息：不输出行列信息。 */
        NONE,
        /** 行定位信息：精确到行，忽略列的变化，并且丢弃终止信息。 */
        LINE,
        /** 行定位信息：精确到具体行列的起止位置。 */
        TERM
    }

    public CompilerArguments() {
    }

    public CompilerArguments(Set<String> varNames) {
        this.compilerVar.addAll(varNames);
    }

    public Set<String> getCompilerVar() {
        return this.compilerVar;
    }

    public CodeLocationEnum getCodeLocation() {
        return codeLocation;
    }

    public void setCodeLocation(CodeLocationEnum codeLocation) {
        this.codeLocation = codeLocation;
    }
}
