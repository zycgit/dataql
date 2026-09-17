/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.parser.ast.inst;
import java.io.IOException;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.parser.ast.*;
import net.hasor.dataql.parser.ast.token.IntegerToken;
import net.hasor.dataql.parser.ast.value.LambdaVariable;
import net.hasor.dataql.parser.location.BlockLocation;

/**
 * exit指令
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class ExitInst extends BlockLocation implements Inst {
    private final IntegerToken exitCode;
    private final Variable     exitData;

    public ExitInst(IntegerToken exitCode, Variable exitData) {
        this.exitCode = exitCode;
        this.exitData = exitData;
    }

    public IntegerToken getExitCode() {
        return exitCode;
    }

    public Variable getExitData() {
        return exitData;
    }

    @Override
    public void accept(AstVisitor astVisitor) {
        astVisitor.visitInst(new InstVisitorContext(this) {
            @Override
            public void visitChildren(AstVisitor astVisitor) {
                exitData.accept(astVisitor);
            }
        });
    }

    @Override
    public void doFormat(int depth, Hints formatOption, FormatWriter writer) throws IOException {
        String fixedString = StringUtils.repeat(' ', depth * fixedLength);
        //
        if (this.exitCode.getValue() != 0) {
            writer.write(fixedString + String.format("exit %s, ", this.exitCode.getValue()));
        } else {
            writer.write(fixedString + "exit ");
        }
        this.exitData.doFormat(depth + 1, formatOption, writer);
        writer.write((this.exitData instanceof LambdaVariable) ? "\n" : ";\n");
    }
}
