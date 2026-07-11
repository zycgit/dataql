/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.dataql.parser.ast.value;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.parser.ast.*;
import net.hasor.dataql.parser.ast.token.StringToken;
import net.hasor.dataql.parser.location.BlockLocation;

/**
 * 外部片段调用（@@type(params)<% body %>）
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class FragmentVariable extends BlockLocation implements Inst, Variable {
    private final StringToken         fragmentName;
    private final List<FragmentParam> paramList = new ArrayList<>();
    private final StringToken         fragmentString;
    private final boolean             batchMode;

    public FragmentVariable(StringToken fragmentName, StringToken fragmentString, boolean batchMode) {
        this.fragmentName = fragmentName;
        this.fragmentString = fragmentString;
        this.batchMode = batchMode;
    }

    public StringToken getFragmentName() {
        return fragmentName;
    }

    public StringToken getFragmentString() {
        return fragmentString;
    }

    public boolean isBatchMode() {
        return this.batchMode;
    }

    public List<FragmentParam> getParamList() {
        return paramList;
    }

    /** 参数项：name + 可选的默认值表达式 */
    public record FragmentParam(StringToken name, Expression value) {

        public boolean hasValue() {
            return value != null;
        }
    }

    @Override
    public void accept(AstVisitor astVisitor) {
        astVisitor.visitInst(new InstVisitorContext(this) {
            @Override
            public void visitChildren(AstVisitor astVisitor) {
                for (FragmentParam param : paramList) {
                    if (param.value() != null) {
                        param.value().accept(astVisitor);
                    }
                }
            }
        });
    }

    @Override
    public void doFormat(int depth, Hints formatOption, FormatWriter writer) throws IOException {
        writer.write("@@" + this.fragmentName.getValue());
        if (batchMode) {
            writer.write("[]");
        }

        writer.write("(");
        for (int i = 0; i < this.paramList.size(); i++) {
            FragmentParam p = this.paramList.get(i);
            if (i > 0) {
                writer.write(", ");
            }

            writer.write(p.name().getValue());
            if (p.hasValue()) {
                writer.write(" = ");
                p.value().doFormat(depth, formatOption, writer);
            }
        }
        writer.write(")<%" + this.fragmentString.getValue() + "%>");
    }
}
