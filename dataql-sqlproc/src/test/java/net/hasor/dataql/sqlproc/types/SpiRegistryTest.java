/*
 * Copyright 2015-2022 the original author or authors.
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
package net.hasor.dataql.sqlproc.types;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.concurrent.atomic.LongAccumulator;
import net.hasor.cobble.ArrayUtils;
import net.hasor.dataql.sqlproc.dynamic.rule.ArgRuleTest;
import net.hasor.dataql.sqlproc.types.array.ArrayTypeHandler;
import net.hasor.dataql.sqlproc.types.bool.BooleanTypeHandler;
import net.hasor.dataql.sqlproc.types.bytes.BytesAsBytesWrapTypeHandler;
import net.hasor.dataql.sqlproc.types.bytes.BytesTypeHandler;
import net.hasor.dataql.sqlproc.types.number.*;
import net.hasor.dataql.sqlproc.types.string.StringTypeHandler;
import net.hasor.dataql.sqlproc.types.time.*;
import org.junit.Test;

public class SpiRegistryTest {

    @Test
    public void ruleTest_1() {
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(Boolean.TRUE.getClass()) instanceof BooleanTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(new Byte((byte) 123).getClass()) instanceof ByteTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(new Short((short) 123).getClass()) instanceof ShortTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(new Integer(123).getClass()) instanceof IntegerTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(new Long(123).getClass()) instanceof LongTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(new Float(123.123f).getClass()) instanceof FloatTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(new Double(123.123f).getClass()) instanceof DoubleTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(new java.util.Date().getClass()) instanceof SqlTimestampAsDateTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(new java.sql.Date(11).getClass()) instanceof SqlDateTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(new java.sql.Timestamp(11).getClass()) instanceof SqlTimestampTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(new java.sql.Time(11).getClass()) instanceof SqlTimeTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(Instant.now().getClass()) instanceof SqlTimestampAsInstantTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(Year.of(2022).getClass()) instanceof SqlTimestampAsYearTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(Month.MAY.getClass()) instanceof SqlTimestampAsMonthTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(YearMonth.now().getClass()) instanceof SqlTimestampAsYearMonthTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(MonthDay.now().getClass()) instanceof SqlTimestampAsMonthDayTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(LocalDate.now().getClass()) instanceof LocalDateTimeAsLocalDateTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(LocalTime.now().getClass()) instanceof LocalTimeTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(LocalDateTime.now().getClass()) instanceof LocalDateTimeTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(ZonedDateTime.now().getClass()) instanceof OffsetDateTimeAsZonedDateTimeTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(OffsetDateTime.now().getClass()) instanceof OffsetDateTimeTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(OffsetTime.now().getClass()) instanceof OffsetTimeTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler("abc".getClass()) instanceof StringTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(BigInteger.valueOf(1L).getClass()) instanceof BigIntegerTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(BigDecimal.valueOf(1L).getClass()) instanceof BigDecimalTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(ArrayUtils.toObject(new byte[] { 1, 2, 3 }).getClass()) instanceof BytesAsBytesWrapTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(new byte[] { 1, 2, 3 }.getClass()) instanceof BytesTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(new Object[] { new Object(), new ArrayUtils() }.getClass()) instanceof ArrayTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(new ArgRuleTest().getClass()) instanceof UnknownTypeHandler;
        assert TypeHandlerRegistry.DEFAULT.getTypeHandler(new LongAccumulator((left, right) -> 0, 1L).getClass()) instanceof NumberTypeHandler;
    }
}
