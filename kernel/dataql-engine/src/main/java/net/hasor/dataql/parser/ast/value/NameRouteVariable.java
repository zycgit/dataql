/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.parser.ast.value;
import java.io.IOException;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.parser.ast.*;
import net.hasor.dataql.parser.ast.token.StringToken;
import net.hasor.dataql.parser.location.BlockLocation;

/**
 * 函数调用 - 之所以是 Variable 是由于 FunctionCall 的最终结果是 函数调用的返回值。而返回值是属于 Variable 的
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class NameRouteVariable extends BlockLocation implements Variable, RouteVariable {
    private final RouteVariable parent;
    private final StringToken   name;

    public NameRouteVariable(RouteVariable parent, StringToken name) {
        this.parent = parent;
        this.name = name;
    }

    @Override
    public RouteVariable getParent() {
        return this.parent;
    }

    public StringToken getName() {
        return name;
    }

    @Override
    public void accept(AstVisitor astVisitor) {
        if (this.parent != null) {
            this.parent.accept(astVisitor);
        }
        astVisitor.visitInst(new InstVisitorContext(this) {
            @Override
            public void visitChildren(AstVisitor astVisitor) {
            }
        });
    }

    @Override
    public void doFormat(int depth, Hints formatOption, FormatWriter writer) throws IOException {
        EnterRouteVariable.RouteType routeType = null;
        EnterRouteVariable.SpecialType specialType = EnterRouteVariable.SpecialType.Special_A;
        if (this.parent instanceof EnterRouteVariable) {
            routeType = ((EnterRouteVariable) parent).getRouteType();
            specialType = ((EnterRouteVariable) parent).getSpecialType();
            if (specialType == null) {
                specialType = EnterRouteVariable.SpecialType.Special_A;
            }
        }
        //
        if (EnterRouteVariable.RouteType.Params == routeType) {
            writer.write(specialType.getCode() + "{");
        }
        //
        this.parent.doFormat(depth, formatOption, writer);
        if (this.parent instanceof EnterRouteVariable) {
            if (StringUtils.isBlank(this.name.getValue())) {
                EnterRouteVariable.SpecialType special = ((EnterRouteVariable) this.parent).getSpecialType();
                if (special != EnterRouteVariable.SpecialType.Special_A) {
                    writer.write(((EnterRouteVariable) this.parent).getSpecialType().getCode());
                }
            } else {
                if (EnterRouteVariable.RouteType.Params != routeType && EnterRouteVariable.SpecialType.Special_A != specialType) {
                    writer.write(specialType.getCode());
                }
                writer.write(this.name.getValue());
            }
        } else {
            if (this.parent instanceof NameRouteVariable && StringUtils.isBlank(((NameRouteVariable) this.parent).name.getValue())) {
                writer.write(this.name.getValue());
            } else {
                writer.write("." + this.name.getValue());
            }
        }
        //
        if (EnterRouteVariable.RouteType.Params == routeType) {
            writer.write("}");
        }
    }
}
