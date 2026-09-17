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
 * return指令
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2019-11-07
 */
public class ReturnInst extends BlockLocation implements Inst {
    private final IntegerToken returnCode;
    private final Variable     resultData;

    public ReturnInst(IntegerToken returnCode, Variable resultData) {
        this.returnCode = returnCode;
        this.resultData = resultData;
    }

    public IntegerToken getReturnCode() {
        return returnCode;
    }

    public Variable getResultData() {
        return resultData;
    }

    @Override
    public void accept(AstVisitor astVisitor) {
        astVisitor.visitInst(new InstVisitorContext(this) {
            @Override
            public void visitChildren(AstVisitor astVisitor) {
                resultData.accept(astVisitor);
            }
        });
    }

    @Override
    public void doFormat(int depth, Hints formatOption, FormatWriter writer) throws IOException {
        String fixedString = StringUtils.repeat(' ', depth * fixedLength);
        //
        if (this.returnCode.getValue() != 0) {
            writer.write(fixedString + String.format("return %s, ", this.returnCode.getValue()));
        } else {
            writer.write(fixedString + "return ");
        }
        this.resultData.doFormat(depth + 1, formatOption, writer);
        writer.write((this.resultData instanceof LambdaVariable) ? "\n" : ";\n");
    }
}
