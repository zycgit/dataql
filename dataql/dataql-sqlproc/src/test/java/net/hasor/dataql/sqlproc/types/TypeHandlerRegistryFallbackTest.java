/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.types;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Types;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class TypeHandlerRegistryFallbackTest {
    @Test
    public void unknownLookupsDoNotRegisterJavaJdbcOrCrossTypes() {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        int unknownJdbcType = Integer.MIN_VALUE;
        for (int attempt = 0; attempt < 2; attempt++) {
            assertUnknown(registry, unknownJdbcType);
            assertSame(registry.getDefaultTypeHandler(), registry.getTypeHandler(PlainBean.class));
            assertUnknown(registry, unknownJdbcType);
            assertSame(registry.getDefaultTypeHandler(), registry.getTypeHandler(PlainBean.class, unknownJdbcType));
            assertUnknown(registry, unknownJdbcType);
            assertSame(registry.getDefaultTypeHandler(), registry.getTypeHandler(PlainBean.class));
            assertUnknown(registry, unknownJdbcType);
            assertSame(registry.getDefaultTypeHandler(), registry.getTypeHandler(unknownJdbcType));
            assertUnknown(registry, unknownJdbcType);
        }
    }

    @Test
    public void explicitFallbackRegistrationStillCountsAsAHandler() {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        TypeHandler fallback = registry.getDefaultTypeHandler();
        registry.getTypeHandler(PlainBean.class);
        registry.getTypeHandler(PlainBean.class, Types.OTHER);
        registry.register(Types.OTHER, PlainBean.class, fallback);
        assertTrue(registry.hasTypeHandler(PlainBean.class, Types.OTHER));
        assertFalse(registry.hasTypeHandler(PlainBean.class));
        assertSame(fallback, registry.getTypeHandler(PlainBean.class, Types.OTHER));
        registry.register(PlainBean.class, fallback);
        assertTrue(registry.hasTypeHandler(PlainBean.class));
        assertTrue(registry.hasTypeHandler(PlainBean.class.getName()));
        assertSame(fallback, registry.getTypeHandler(PlainBean.class));
        assertSame(fallback, registry.getTypeHandler(PlainBean.class));
        registry.register(Integer.MIN_VALUE, fallback);
        assertTrue(registry.hasTypeHandler(Integer.MIN_VALUE));
        assertSame(fallback, registry.getTypeHandler(Integer.MIN_VALUE));
    }

    @Test
    public void explicitHandlersAfterFallbackLookupsRetainCrossTypePrecedence() {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        registry.getTypeHandler(PlainBean.class);
        registry.getTypeHandler(PlainBean.class, Types.OTHER);
        TypeHandler javaHandler = mock(TypeHandler.class);
        TypeHandler crossHandler = mock(TypeHandler.class);
        registry.register(PlainBean.class, javaHandler);
        registry.register(Types.OTHER, PlainBean.class, crossHandler);
        assertSame(javaHandler, registry.getTypeHandler(PlainBean.class));
        assertSame(javaHandler, registry.getTypeHandler(PlainBean.class));
        assertSame(javaHandler, registry.getTypeHandler(PlainBean.class, Types.VARCHAR));
        assertSame(crossHandler, registry.getTypeHandler(PlainBean.class, Types.OTHER));
        assertTrue(registry.hasTypeHandler(PlainBean.class));
        assertTrue(registry.hasTypeHandler(PlainBean.class, Types.OTHER));
        assertFalse(registry.hasTypeHandler(PlainBean.class, Types.VARCHAR));
    }

    private static void assertUnknown(TypeHandlerRegistry registry, int jdbcType) {
        assertFalse(registry.hasTypeHandler(PlainBean.class));
        assertFalse(registry.hasTypeHandler(PlainBean.class.getName()));
        assertFalse(registry.hasTypeHandler(PlainBean.class, jdbcType));
        assertFalse(registry.hasTypeHandler(jdbcType));
        assertFalse(registry.getHandlerJavaTypes().contains(PlainBean.class.getName()));
    }

    public static class PlainBean {
        private Integer id;
        private String  name;

        public Integer getId() {
            return this.id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
