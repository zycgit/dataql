/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.parser.ast.value;
import java.io.IOException;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.parser.ast.AstVisitor;
import net.hasor.dataql.parser.ast.FormatWriter;
import net.hasor.dataql.parser.ast.InstVisitorContext;
import net.hasor.dataql.parser.ast.RouteVariable;
import net.hasor.dataql.parser.location.BlockLocation;

/**
 * 路由的入口，一切路由操作都要有一个入口
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class EnterRouteVariable extends BlockLocation implements RouteVariable {
    public enum RouteType {
        /** 表达式 */
        Expr(),
        /** 程序传参 */
        Params(),
    }

    public enum SpecialType {
        Special_A("#"),  // 特殊路由1，自定义
        Special_B("$"),  // 特殊路由2，自定义
        Special_C("@"),  // 特殊路由3，自定义
        ;
        private final String code;

        SpecialType(String code) {
            this.code = code;
        }

        public String getCode() {
            return this.code;
        }
    }

    private final RouteType   routeType;
    private final SpecialType specialType;

    public EnterRouteVariable(RouteType routeType, SpecialType specialType) {
        this.routeType = routeType;
        this.specialType = specialType;
    }

    @Override
    public RouteVariable getParent() {
        return null;
    }

    public RouteType getRouteType() {
        return routeType;
    }

    public SpecialType getSpecialType() {
        return this.specialType;
    }

    @Override
    public void accept(AstVisitor astVisitor) {
        astVisitor.visitInst(new InstVisitorContext(this) {
            @Override
            public void visitChildren(AstVisitor astVisitor) {
            }
        });
    }

    @Override
    public void doFormat(int depth, Hints formatOption, FormatWriter writer) throws IOException {
    }
}
