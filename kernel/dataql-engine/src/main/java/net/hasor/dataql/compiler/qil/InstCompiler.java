/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.compiler.qil;
import net.hasor.dataql.compiler.CompilerArguments.CodeLocationEnum;
import net.hasor.dataql.parser.ast.Inst;
import net.hasor.dataql.parser.location.CodeLocation;
import net.hasor.dataql.parser.location.Location;

/**
 * 每一个 AST 树都会对应一个 InstCompiler
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public interface InstCompiler<T extends Inst> extends Opcodes {
    /**
     * 生成指令序列
     * @param astInst 要编译的 Inst
     * @param queue 编译输出的指令序列
     * @param compilerContext 编译上下文
     */
    void doCompiler(T astInst, InstQueue queue, CompilerContext compilerContext);

    default void instLocationFocus(InstQueue queue, Location location) {
        this.instLocation(true, queue, location);
    }

    default void instLocation(InstQueue queue, Location location) {
        this.instLocation(false, queue, location);
    }

    default void instLocation(boolean focus, InstQueue queue, Location location) {
        CodeLocationEnum locationEnum = queue.getCompilerArguments().getCodeLocation();
        if (location == null || locationEnum == null || locationEnum == CodeLocationEnum.NONE) {
            return;
        }
        //
        CodeLocation startPosition = location.getStartPosition();
        CodeLocation endPosition = location.getEndPosition();
        if (startPosition == null || endPosition == null) {
            return;
        }
        if (locationEnum == CodeLocationEnum.LINE) {
            queue.inst(LINE, focus, startPosition.lineNumber());
        } else {
            queue.inst(LINE, focus,//
                    startPosition.lineNumber(), startPosition.columnNumber(),//
                    endPosition.lineNumber(), endPosition.columnNumber());
        }
    }
}
