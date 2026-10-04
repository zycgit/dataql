/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.domain;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class ModelBoundaryTest {
    @Test
    public void charactersRemainTextIncludingPrimitiveArrays() {
        assertEquals("a", DomainHelper.convertTo('a').unwrap());
        assertEquals(List.of("a", "中"), DomainHelper.convertTo(new char[] { 'a', '中' }).unwrap());
    }

    @Test
    public void enumConstantsWithAnImplementationRemainNames() {
        assertEquals("VALUE", DomainHelper.convertTo(ModelEnumValue.VALUE).unwrap());
    }

    @Test
    public void nonzeroNumbersDoNotBecomeFalseAfterNarrowing() {
        for (Number value : List.of(4294967296L, BigInteger.ONE.shiftLeft(64), new BigDecimal("1E-1000"), 0.5D)) {
            assertTrue(value.toString(), ((ValueModel) DomainHelper.convertTo(value)).asBoolean());
        }
        for (Number value : List.of(0, 0L, -0.0D, BigInteger.ZERO, new BigDecimal("0.00"))) {
            assertFalse(((ValueModel) DomainHelper.convertTo(value)).asBoolean());
        }
    }

    @Test
    public void absentFieldsAreDifferentFromExplicitNullAndWrongTypes() {
        ObjectModel model = new ObjectModel();
        assertNull(model.getValue("absent"));
        assertNull(model.getList("absent"));
        assertNull(model.getObject("absent"));
        assertNull(model.getUdf("absent"));
        model.put("value", null);
        assertTrue(model.getValue("value").isNull());
        assertThrows(ClassCastException.class, () -> model.getList("value"));
        assertThrows(ClassCastException.class, () -> model.getObject("value"));
        assertThrows(ClassCastException.class, () -> model.getUdf("value"));
    }
}