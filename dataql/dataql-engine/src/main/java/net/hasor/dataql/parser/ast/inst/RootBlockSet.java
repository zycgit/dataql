/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.parser.ast.inst;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.parser.QueryModel;
import net.hasor.dataql.parser.ast.AstVisitor;
import net.hasor.dataql.parser.ast.FormatWriter;
import net.hasor.dataql.parser.ast.Inst;
import net.hasor.dataql.parser.ast.InstVisitorContext;

/**
 * 指令序列
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2019-11-07
 */
public class RootBlockSet extends InstSet implements QueryModel {
    private final List<ImportInst> importSet = new ArrayList<>();

    public RootBlockSet() {
        super(true);
    }

    /** 添加导入 */
    public void addImportInst(ImportInst inst) {
        this.importSet.add(Objects.requireNonNull(inst, "import inst npe."));
    }

    public List<ImportInst> getImportSet() {
        return importSet;
    }

    @Override
    public void accept(AstVisitor astVisitor) {
        astVisitor.visitInst(new InstVisitorContext(this) {
            @Override
            public void visitChildren(AstVisitor astVisitor) {
                for (HintInst inst : getOptionSet()) {
                    inst.accept(astVisitor);
                }
                for (ImportInst inst : importSet) {
                    inst.accept(astVisitor);
                }
                for (Inst inst : RootBlockSet.this) {
                    inst.accept(astVisitor);
                }
            }
        });
    }

    @Override
    public void toQueryString(HintsSet formatOptions, Writer writer) throws IOException {
        FormatWriter formatWriter = new FormatWriter(writer);
        formatOptions = (formatOptions == null) ? new HintsSet() : formatOptions;
        //
        for (HintInst opt : this.getOptionSet()) {
            opt.doFormat(0, formatOptions, formatWriter);
        }
        for (ImportInst opt : this.importSet) {
            opt.doFormat(0, formatOptions, formatWriter);
        }
        writer.write("\n");
        for (int i = 0; i < this.size(); i++) {
            Inst inst = this.get(i);
            inst.doFormat(0, formatOptions, formatWriter);
            if (inst instanceof InstSet) {
                writer.write("\n");
            }
        }
        writer.flush();
    }
}
