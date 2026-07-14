package net.hasor.dataql.sqlproc.types;

import java.sql.Types;
import java.util.Date;
import net.hasor.dataql.sqlproc.types.bool.BooleanTypeHandler;
import net.hasor.dataql.sqlproc.types.number.IntegerTypeHandler;
import net.hasor.dataql.sqlproc.types.string.StringTypeHandler;
import org.junit.Test;

public class TypeHandlerSpiRegistryCoverageTest {

    @Test
    public void testDefaults() {
        TypeHandlerRegistry registry = TypeHandlerRegistry.DEFAULT;

        assert registry.hasTypeHandler(String.class);
        assert registry.hasTypeHandler(Integer.class);
        assert registry.hasTypeHandler(Date.class);

        // Enum support
        assert registry.hasTypeHandler(MyEnum.class);

        // JDBC type mappings
        assert TypeHandlerRegistry.toSqlType(String.class) == Types.VARCHAR;
        assert TypeHandlerRegistry.toSqlType(Integer.class) == Types.INTEGER;
        assert TypeHandlerRegistry.toSqlType(String.class.getName()) == Types.VARCHAR;
        assert TypeHandlerRegistry.toSqlType(System.class) == Types.OTHER;
    }

    @Test
    public void testRegisterJavaType() {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        // Register Java Type
        registry.register(MyType.class, new StringTypeHandler());
        assert registry.getTypeHandler(MyType.class) instanceof StringTypeHandler;
        assert registry.hasTypeHandler(MyType.class);
    }

    @Test
    public void testRegisterJdbcType() {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        // Register JDBC Type
        registry.register(9001, new IntegerTypeHandler());
        assert registry.getTypeHandler(9001) instanceof IntegerTypeHandler;
    }

    @Test
    public void testRegisterCrossType() {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();

        // Register Java Type for fallback testing
        registry.register(MyType.class, new StringTypeHandler());

        // Register Cross Type (MyType + 9002)
        registry.register(9002, MyType.class, new BooleanTypeHandler());
        assert registry.getTypeHandler(MyType.class, 9002) instanceof BooleanTypeHandler;

        // Fallback check: MyType + 9999 (not registered) -> should fallback to MyType handler (StringTypeHandler)
        assert registry.getTypeHandler(MyType.class, 9999) instanceof StringTypeHandler;
    }

    @Test
    public void testRegisterAbstractType() {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        // Register Abstract Type
        registry.register(AbstractType.class, new IntegerTypeHandler());
        assert registry.getTypeHandler(ConcreteType.class) instanceof IntegerTypeHandler; // subclass lookup
    }

    @Test
    public void testRegisterAbstractCrossType() {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        // Register Abstract Cross Type
        registry.register(Types.FLOAT, AbstractType.class, new BooleanTypeHandler());
        // ConcreteType extends AbstractType.
        // Should hit abstract cross type mapping.
        assert registry.getTypeHandler(ConcreteType.class, Types.FLOAT) instanceof BooleanTypeHandler;

        // Check standard types don't get messed up (sanity check)
        assert registry.getTypeHandler(String.class) instanceof StringTypeHandler;
    }

    @Test
    public void testGetHandlerFallbacks() {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();

        // 1. Null Class - expecting NPE as per implementation
        try {
            registry.getTypeHandler((Class<?>) null);
            assert false : "Should throw NPE";
        } catch (NullPointerException e) {
            assert "typeClass is null.".equals(e.getMessage());
        }

        // 2. Unregistered Class -> default to UnknownTypeHandler
        assert registry.getTypeHandler(MyType.class) instanceof UnknownTypeHandler;

        // 3. JDBC Type lookup
        // Types.VARCHAR IS registered by default to StringTypeHandler
        assert registry.getTypeHandler(Types.VARCHAR) instanceof StringTypeHandler;

        // 4. Cross Type lookup fallback with null class -> UnknownTypeHandler (default)
        assert registry.getTypeHandler(null, Types.VARCHAR) instanceof UnknownTypeHandler;
    }

    private static class MyType {
    }

    private enum MyEnum {
        A
    }

    private static abstract class AbstractType {
    }

    private static class ConcreteType extends AbstractType {
    }
}
