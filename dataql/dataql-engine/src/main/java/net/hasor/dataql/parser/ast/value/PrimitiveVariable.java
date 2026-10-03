/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.parser.ast.value;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import net.hasor.cobble.text.StringEscapeUtils;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.parser.ast.*;
import net.hasor.dataql.parser.location.BlockLocation;
import static net.hasor.cobble.NumberUtils.*;

/**
 * 基础类型值，用于表示【String、Number、Null、Boolean】四种基本类型
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class PrimitiveVariable extends BlockLocation implements Variable, Inst {
    public enum ValueType {
        Boolean,
        Number,
        String,
        Null
    }

    private final Object    value;
    private final ValueType valueType;
    private final int       radix;

    public PrimitiveVariable(Object value, ValueType valueType) {
        this.value = value;
        this.valueType = valueType;
        this.radix = 10;
    }

    public PrimitiveVariable(Object value, ValueType valueType, int radix) {
        this.value = value;
        this.valueType = valueType;
        this.radix = radix;
    }

    public Object getValue() {
        return value;
    }

    public ValueType getValueType() {
        return valueType;
    }

    @Override
    public String toString() {
        return "Primitive - '" + this.value + "'";
    }

    @Override
    public void accept(AstVisitor astVisitor) {
        astVisitor.visitInst(new InstVisitorContext(this) {
            @Override
            public void visitChildren(AstVisitor astVisitor) {
            }
        });
    }

    private String radix2String(int radix) {
        if (radix == 2) {
            return "0b";
        }
        if (radix == 8) {
            return "0o";
        }
        if (radix == 10) {
            return "";
        }
        if (radix == 16) {
            return "0x";
        }
        throw new RuntimeException("radix not support.");
    }

    @Override
    public void doFormat(int depth, Hints formatOption, FormatWriter writer) throws IOException {
        if (this.valueType == ValueType.Null) {
            writer.write("null");
        } else if (this.valueType == ValueType.String) {
            String newValue = StringEscapeUtils.escapeJava(this.value.toString());
            writer.write(quoteChar + newValue + quoteChar);
        } else if (this.value instanceof Number number) {
            if (isByteType(number.getClass()) || isShortType(number.getClass()) || isIntType(number.getClass()) || isLongType(number.getClass())) {
                long longValue = number.longValue();
                int beginSub = longValue < 0 ? 1 : 0;
                String string = radix2String(this.radix) + Long.toString(longValue, this.radix).substring(beginSub);
                if (beginSub > 0) {
                    writer.write("-" + string);
                } else {
                    writer.write(string);
                }
            } else if (number instanceof BigInteger bigInteger) {
                int beginSub = bigInteger.compareTo(BigInteger.ZERO) < 0 ? 1 : 0;
                String string = radix2String(this.radix) + bigInteger.toString(this.radix).substring(beginSub);
                if (beginSub > 0) {
                    writer.write("-" + string);
                } else {
                    writer.write(string);
                }
            } else if (isFloatType(number.getClass()) || isDoubleType(number.getClass())) {
                writer.write(Double.toString(number.doubleValue()));
            } else if (number instanceof BigDecimal) {
                writer.write(number.toString());
            } else {
                throw new RuntimeException("not support number type.");
            }
        } else {
            writer.write(value.toString());
        }
    }
}
