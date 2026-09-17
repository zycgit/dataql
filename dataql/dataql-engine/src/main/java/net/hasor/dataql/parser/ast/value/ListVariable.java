/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.parser.ast.value;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.parser.ast.AstVisitor;
import net.hasor.dataql.parser.ast.FormatWriter;
import net.hasor.dataql.parser.ast.InstVisitorContext;
import net.hasor.dataql.parser.ast.Variable;
import net.hasor.dataql.parser.location.BlockLocation;

/**
 * 列表
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class ListVariable extends BlockLocation implements Variable {
    private final List<Variable> expressionList = new ArrayList<>();

    /** 添加元素 */
    public void addItem(Variable valueExp) {
        if (valueExp != null) {
            this.expressionList.add(valueExp);
        }
    }

    public List<Variable> getExpressionList() {
        return expressionList;
    }

    @Override
    public void accept(AstVisitor astVisitor) {
        astVisitor.visitInst(new InstVisitorContext(this) {
            @Override
            public void visitChildren(AstVisitor astVisitor) {
                for (Variable var : expressionList) {
                    var.accept(astVisitor);
                }
            }
        });
    }

    @Override
    public void doFormat(int depth, Hints formatOption, FormatWriter writer) throws IOException {
        if (this.expressionList.isEmpty()) {
            writer.write("[]");
            return;
        }
        String fixedString = StringUtils.repeat(' ', depth * fixedLength);
        boolean innerLine = this.expressionList.stream().allMatch(variable -> variable instanceof PrimitiveVariable);
        if (innerLine) {
            fixedString = "";
        } else {
            fixedString = "\n" + fixedString;
        }
        //
        writer.write("[" + fixedString);
        for (int i = 0; i < this.expressionList.size(); i++) {
            if (i > 0) {
                writer.write("," + fixedString);
            }
            Variable expr = this.expressionList.get(i);
            if (expr instanceof EnterRouteVariable) {
                writer.write(((EnterRouteVariable) expr).getSpecialType().getCode());
            } else {
                expr.doFormat(depth + 1, formatOption, writer);
            }
        }
        //
        if (innerLine) {
            writer.write("]");
        } else {
            writer.write("\n" + StringUtils.repeat(' ', (depth - 1) * fixedLength) + "]");
        }
    }
}
