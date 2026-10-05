/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.types;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.hasor.cobble.ClassUtils;
import net.hasor.cobble.ResourcesUtils;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.reflect.resolvable.ResolvableType;
import net.hasor.dataql.domain.BinaryModel;
import net.hasor.dataql.sqlproc.dialect.JdbcHelper;
import net.hasor.dataql.sqlproc.types.array.ArrayTypeHandler;
import net.hasor.dataql.sqlproc.types.bool.BooleanTypeHandler;
import net.hasor.dataql.sqlproc.types.bytes.BlobAsBytesTypeHandler;
import net.hasor.dataql.sqlproc.types.bytes.BytesTypeHandler;
import net.hasor.dataql.sqlproc.types.number.*;
import net.hasor.dataql.sqlproc.types.string.*;
import net.hasor.dataql.sqlproc.types.time.*;

/**
 * Maps script values and JDBC column types to SQL type handlers.
 * @author 赵永春 (zyc@hasor.net)
 * @version 2020-10-31
 */
public final class TypeHandlerRegistry {
    private static final Map<String, Integer>  javaTypeToJdbcTypeMap = new ConcurrentHashMap<>();
    private static final Map<String, Class<?>> typeHandlerTypeCache  = new ConcurrentHashMap<>();
    public static final  TypeHandlerRegistry   DEFAULT               = new TypeHandlerRegistry();

    private final UnknownTypeHandler                       defaultTypeHandler        = new UnknownTypeHandler(this);
    private final Map<String, TypeHandler>                 cachedByHandlerType       = new ConcurrentHashMap<>();
    private final Map<String, TypeHandler>                 cachedByJavaType          = new ConcurrentHashMap<>();
    private final Map<Integer, TypeHandler>                cachedByJdbcType          = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, TypeHandler>>   cachedByCrossType         = new ConcurrentHashMap<>();
    private final Map<Class<?>, TypeHandler>               abstractCachedByJavaType  = new ConcurrentHashMap<>();
    private final Map<Class<?>, Map<Integer, TypeHandler>> abstractCachedByCrossType = new ConcurrentHashMap<>();

    static {
        // primitive and wrapper
        javaTypeToJdbcTypeMap.put(Boolean.class.getName(), Types.BIT);
        javaTypeToJdbcTypeMap.put(boolean.class.getName(), Types.BIT);
        javaTypeToJdbcTypeMap.put(Byte.class.getName(), Types.TINYINT);
        javaTypeToJdbcTypeMap.put(byte.class.getName(), Types.TINYINT);
        javaTypeToJdbcTypeMap.put(Short.class.getName(), Types.SMALLINT);
        javaTypeToJdbcTypeMap.put(short.class.getName(), Types.SMALLINT);
        javaTypeToJdbcTypeMap.put(Integer.class.getName(), Types.INTEGER);
        javaTypeToJdbcTypeMap.put(int.class.getName(), Types.INTEGER);
        javaTypeToJdbcTypeMap.put(Long.class.getName(), Types.BIGINT);
        javaTypeToJdbcTypeMap.put(long.class.getName(), Types.BIGINT);
        javaTypeToJdbcTypeMap.put(Float.class.getName(), Types.FLOAT);
        javaTypeToJdbcTypeMap.put(float.class.getName(), Types.FLOAT);
        javaTypeToJdbcTypeMap.put(Double.class.getName(), Types.DOUBLE);
        javaTypeToJdbcTypeMap.put(double.class.getName(), Types.DOUBLE);
        // java time
        javaTypeToJdbcTypeMap.put(Date.class.getName(), Types.TIMESTAMP);
        javaTypeToJdbcTypeMap.put(java.sql.Date.class.getName(), Types.DATE);
        javaTypeToJdbcTypeMap.put(Timestamp.class.getName(), Types.TIMESTAMP);
        javaTypeToJdbcTypeMap.put(Time.class.getName(), Types.TIME);
        // java extensions Types
        javaTypeToJdbcTypeMap.put(String.class.getName(), Types.VARCHAR);
        javaTypeToJdbcTypeMap.put(BigInteger.class.getName(), Types.BIGINT);
        javaTypeToJdbcTypeMap.put(BigDecimal.class.getName(), Types.DECIMAL);
        javaTypeToJdbcTypeMap.put(byte[].class.getName(), Types.VARBINARY);
        javaTypeToJdbcTypeMap.put(Object[].class.getName(), Types.ARRAY);
        javaTypeToJdbcTypeMap.put(Object.class.getName(), Types.JAVA_OBJECT);
        // oracle types
        javaTypeToJdbcTypeMap.put("oracle.jdbc.OracleBlob", Types.BLOB);
        javaTypeToJdbcTypeMap.put("oracle.jdbc.OracleClob", Types.CLOB);
        javaTypeToJdbcTypeMap.put("oracle.jdbc.OracleNClob", Types.NCLOB);
        javaTypeToJdbcTypeMap.put("oracle.sql.DATE", Types.DATE);
        javaTypeToJdbcTypeMap.put("oracle.sql.TIMESTAMP", Types.TIMESTAMP);
        javaTypeToJdbcTypeMap.put("oracle.sql.TIMESTAMPTZ", Types.TIMESTAMP_WITH_TIMEZONE);
        javaTypeToJdbcTypeMap.put("oracle.sql.TIMESTAMPLTZ", Types.TIMESTAMP_WITH_TIMEZONE);
    }

    public TypeHandlerRegistry() {
        this.register(Boolean.class, this.createTypeHandler(BooleanTypeHandler.class));
        this.register(boolean.class, this.createTypeHandler(BooleanTypeHandler.class));
        this.register(Byte.class, this.createTypeHandler(ByteTypeHandler.class));
        this.register(byte.class, this.createTypeHandler(ByteTypeHandler.class));
        this.register(Short.class, this.createTypeHandler(ShortTypeHandler.class));
        this.register(short.class, this.createTypeHandler(ShortTypeHandler.class));
        this.register(Integer.class, this.createTypeHandler(IntegerTypeHandler.class));
        this.register(int.class, this.createTypeHandler(IntegerTypeHandler.class));
        this.register(Long.class, this.createTypeHandler(LongTypeHandler.class));
        this.register(long.class, this.createTypeHandler(LongTypeHandler.class));
        this.register(Float.class, this.createTypeHandler(FloatTypeHandler.class));
        this.register(float.class, this.createTypeHandler(FloatTypeHandler.class));
        this.register(Double.class, this.createTypeHandler(DoubleTypeHandler.class));
        this.register(double.class, this.createTypeHandler(DoubleTypeHandler.class));
        this.register(String.class, this.createTypeHandler(StringTypeHandler.class));
        this.register(BigInteger.class, this.createTypeHandler(BigIntegerTypeHandler.class));
        this.register(BigDecimal.class, this.createTypeHandler(BigDecimalTypeHandler.class));
        this.register(Number.class, this.createTypeHandler(NumberTypeHandler.class));
        this.register(Date.class, this.createTypeHandler(SqlTimestampTypeHandler.class));
        this.register(java.sql.Date.class, this.createTypeHandler(SqlDateTypeHandler.class));
        this.register(Time.class, this.createTypeHandler(SqlTimeTypeHandler.class));
        this.register(Timestamp.class, this.createTypeHandler(SqlTimestampTypeHandler.class));
        this.register(byte[].class, this.createTypeHandler(BytesTypeHandler.class));
        this.register(BinaryModel.class, this.createTypeHandler(BlobAsBytesTypeHandler.class));
        this.register(Collection.class, this.createTypeHandler(ArrayTypeHandler.class));
        this.register(Object[].class, this.createTypeHandler(ArrayTypeHandler.class));
        this.register(Array.class, this.createTypeHandler(ArrayTypeHandler.class));
        this.register(Object.class, this.createTypeHandler(UnknownTypeHandler.class));

        // Result columns and typed nulls follow JDBC semantics.
        this.register(Types.BIT, this.createTypeHandler(BooleanTypeHandler.class));
        this.register(Types.BOOLEAN, this.createTypeHandler(BooleanTypeHandler.class));
        this.register(Types.TINYINT, this.createTypeHandler(ByteTypeHandler.class));
        this.register(Types.SMALLINT, this.createTypeHandler(ShortTypeHandler.class));
        this.register(Types.INTEGER, this.createTypeHandler(IntegerTypeHandler.class));
        this.register(Types.BIGINT, this.createTypeHandler(LongTypeHandler.class));
        this.register(Types.FLOAT, this.createTypeHandler(FloatTypeHandler.class));
        this.register(Types.REAL, this.createTypeHandler(FloatTypeHandler.class));
        this.register(Types.DOUBLE, this.createTypeHandler(DoubleTypeHandler.class));
        this.register(Types.NUMERIC, this.createTypeHandler(BigDecimalTypeHandler.class));
        this.register(Types.DECIMAL, this.createTypeHandler(BigDecimalTypeHandler.class));
        this.register(Types.CHAR, this.createTypeHandler(StringTypeHandler.class));
        this.register(Types.VARCHAR, this.createTypeHandler(StringTypeHandler.class));
        this.register(Types.LONGVARCHAR, this.createTypeHandler(StringTypeHandler.class));
        this.register(Types.DATALINK, this.createTypeHandler(StringTypeHandler.class));
        this.register(Types.ROWID, this.createTypeHandler(StringTypeHandler.class));
        this.register(Types.NCHAR, this.createTypeHandler(NStringTypeHandler.class));
        this.register(Types.NVARCHAR, this.createTypeHandler(NStringTypeHandler.class));
        this.register(Types.LONGNVARCHAR, this.createTypeHandler(NStringTypeHandler.class));
        this.register(Types.CLOB, this.createTypeHandler(ClobAsStringTypeHandler.class));
        this.register(Types.NCLOB, this.createTypeHandler(NClobAsStringTypeHandler.class));
        this.register(Types.DATE, this.createTypeHandler(SqlDateTypeHandler.class));
        this.register(Types.TIME, this.createTypeHandler(SqlTimeTypeHandler.class));
        this.register(Types.TIMESTAMP, this.createTypeHandler(SqlTimestampTypeHandler.class));
        this.register(Types.TIME_WITH_TIMEZONE, this.createTypeHandler(OffsetTimeTypeHandler.class));
        this.register(Types.TIMESTAMP_WITH_TIMEZONE, this.createTypeHandler(OffsetDateTimeTypeHandler.class));
        this.register(Types.SQLXML, this.createTypeHandler(SqlXmlTypeHandler.class));
        this.register(Types.BINARY, this.createTypeHandler(BytesTypeHandler.class));
        this.register(Types.VARBINARY, this.createTypeHandler(BytesTypeHandler.class));
        this.register(Types.LONGVARBINARY, this.createTypeHandler(BytesTypeHandler.class));
        this.register(Types.BLOB, this.createTypeHandler(BlobAsBytesTypeHandler.class));
        this.register(Types.ARRAY, this.createTypeHandler(ArrayTypeHandler.class));
        this.register(Types.JAVA_OBJECT, this.createTypeHandler(ObjectTypeHandler.class));
        this.register(Types.OTHER, this.createTypeHandler(UnknownTypeHandler.class));

        this.registerCrossChars(String.class, this.createTypeHandler(StringTypeHandler.class));
        this.registerCrossNChars(String.class, this.createTypeHandler(NStringTypeHandler.class));
        this.register(Types.CLOB, String.class, this.createTypeHandler(ClobAsStringTypeHandler.class));
        this.register(Types.NCLOB, String.class, this.createTypeHandler(NClobAsStringTypeHandler.class));
        this.register(Types.SQLXML, String.class, this.createTypeHandler(SqlXmlTypeHandler.class));

        // Scripts supply epoch milliseconds or text, rather than Java temporal objects.
        for (int jdbcType : new int[] { Types.DATE, Types.TIME, Types.TIMESTAMP, Types.TIME_WITH_TIMEZONE, Types.TIMESTAMP_WITH_TIMEZONE }) {
            TypeHandler handler = this.getTypeHandler(jdbcType);
            this.register(jdbcType, String.class, handler);
            this.register(jdbcType, Number.class, handler);
            this.register(jdbcType, Date.class, handler);
        }

        for (int jdbcType : new int[] { Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB }) {
            TypeHandler handler = this.getTypeHandler(jdbcType);
            this.register(jdbcType, BinaryModel.class, handler);
            this.register(jdbcType, byte[].class, handler);
        }
        this.register(Types.ARRAY, Collection.class, this.createTypeHandler(ArrayTypeHandler.class));
        this.register(Types.ARRAY, Object[].class, this.createTypeHandler(ArrayTypeHandler.class));
    }

    private static void registerTypeHandlerType(TypeHandler typeHandler) {
        if (typeHandler != null) {
            registerTypeHandlerType(typeHandler.getClass());
        }
    }

    private static void registerTypeHandlerType(Class<?> typeHandler) {
        String name = typeHandler.getName();
        if (!typeHandlerTypeCache.containsKey(name) && !typeHandler.isAnnotationPresent(NoCache.class)) {
            typeHandlerTypeCache.put(name, typeHandler);
        }
    }

    public TypeHandler getHandlerByHandlerType(String handlerType) {
        return this.cachedByHandlerType.getOrDefault(handlerType, null);
    }

    public TypeHandler getHandlerByHandlerType(Class<?> handlerType) {
        return this.cachedByHandlerType.getOrDefault(handlerType.getName(), null);
    }

    public TypeHandler createTypeHandler(Class<?> typeHandler) {
        return this.createTypeHandler(typeHandler, (ResolvableType) null);
    }

    public TypeHandler createTypeHandler(Class<?> typeHandler, Class<?> argType) {
        return this.createTypeHandler(typeHandler, ResolvableType.forType(argType));
    }

    public TypeHandler createTypeHandler(Class<?> typeHandler, ResolvableType argType) {
        return this.createTypeHandler(typeHandler, argType, type -> {
            try {
                Constructor<?> constructor = typeHandler.getConstructor(ResolvableType.class);
                return this.createByConstructor(constructor, type);
            } catch (NoSuchMethodException e1) {
                try {
                    Constructor<?> constructor = typeHandler.getConstructor(Class.class);
                    Class<?> rawClass = type == null ? Object.class : type.getRawClass();
                    return this.createByConstructor(constructor, rawClass);
                } catch (NoSuchMethodException e2) {
                    return this.createByClass(typeHandler, null);
                }
            }
        });
    }

    private TypeHandler createByClass(Class<?> typeHandlerClass, Class<?> argType) {
        return ClassUtils.newInstance(typeHandlerClass.asSubclass(TypeHandler.class));
    }

    private TypeHandler createByConstructor(Constructor<?> typeHandlerConstructor, Object argType) {
        try {
            return (TypeHandler) typeHandlerConstructor.newInstance(argType);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public TypeHandler createTypeHandler(Class<?> typeHandler, Class<?> argType, Function<Class<?>, TypeHandler> supplier) {
        return this.createTypeHandler(typeHandler, ResolvableType.forType(argType), r -> supplier.apply(r.getRawClass()));
    }

    public TypeHandler createTypeHandler(Class<?> typeHandler, ResolvableType argType, Function<ResolvableType, TypeHandler> supplier) {
        if (!TypeHandler.class.isAssignableFrom(typeHandler)) {
            throw new ClassCastException(typeHandler.getName() + " is not a subclass of " + TypeHandler.class.getName());
        }

        if (typeHandler.isAnnotationPresent(NoCache.class)) {
            if (typeHandler == UnknownTypeHandler.class) {
                return this.defaultTypeHandler;
            } else {
                TypeHandler handler = supplier.apply(argType);
                if (handler == null) {
                    return this.defaultTypeHandler;
                } else {
                    return handler;
                }
            }
        } else {
            registerTypeHandlerType(typeHandler);
            String cacheName = typeHandler.getName() + (argType == null ? "" : ("," + argType.getType().toString()));
            return this.cachedByHandlerType.computeIfAbsent(cacheName, type -> {
                if (typeHandler == UnknownTypeHandler.class) {
                    return this.defaultTypeHandler;
                } else {
                    TypeHandler handler = supplier.apply(argType);
                    if (handler == null) {
                        return this.defaultTypeHandler;
                    } else {
                        return handler;
                    }
                }
            });
        }
    }

    /**
     * 注册 {@link TypeHandler} 到指定的 JDBC 类型
     * @param jdbcType JDBC 类型代码
     * @param typeHandler 类型处理器实例
     */
    public void register(int jdbcType, TypeHandler typeHandler) {
        this.cachedByJdbcType.put(jdbcType, typeHandler);
        registerTypeHandlerType(typeHandler);
    }

    /**
     * 注册 {@link TypeHandler} 到指定的 Java 类型
     * @param javaType Java 类型
     * @param typeHandler 类型处理器实例
     */
    public void register(Class<?> javaType, TypeHandler typeHandler) {
        if (isAbstract(javaType)) {
            this.abstractCachedByJavaType.put(javaType, typeHandler);
        } else {
            this.cachedByJavaType.put(javaType.getName(), typeHandler);
        }
        registerTypeHandlerType(typeHandler);
    }

    /**
     * 注册 {@link TypeHandler} 到指定的 JDBC 类型和 Java 类型的组合
     * @param jdbcType JDBC 类型代码
     * @param javaType Java 类型
     * @param typeHandler 类型处理器实例
     */
    public void register(int jdbcType, Class<?> javaType, TypeHandler typeHandler) {
        if (isAbstract(javaType)) {
            this.abstractCachedByCrossType.computeIfAbsent(javaType, k -> {
                return new LinkedHashMap<>();
            }).put(jdbcType, typeHandler);
        } else {
            this.cachedByCrossType.computeIfAbsent(javaType.getName(), k -> {
                return new ConcurrentHashMap<>();
            }).put(jdbcType, typeHandler);
        }

        registerTypeHandlerType(typeHandler);
    }

    private void registerCrossChars(Class<?> jdbcType, TypeHandler typeHandler) {
        register(Types.CHAR, jdbcType, typeHandler);
        register(Types.VARCHAR, jdbcType, typeHandler);
        register(Types.LONGVARCHAR, jdbcType, typeHandler);
    }

    private void registerCrossNChars(Class<?> jdbcType, TypeHandler typeHandler) {
        register(Types.NCHAR, jdbcType, typeHandler);
        register(Types.NVARCHAR, jdbcType, typeHandler);
        register(Types.LONGNVARCHAR, jdbcType, typeHandler);
    }

    public Collection<TypeHandler> getTypeHandlers() {
        return Collections.unmodifiableCollection(this.cachedByJavaType.values());
    }

    public Collection<String> getHandlerJavaTypes() {
        return Collections.unmodifiableCollection(this.cachedByJavaType.keySet());
    }

    /**
     * 根据Java类型名称获取默认的 JDBC 类型
     * @param javaType Java 类型名称
     * @return 对应的 JDBC 类型代码，如果未找到则返回 Types.OTHER
     */
    public static int toSqlType(final String javaType) {
        Integer jdbcType = javaTypeToJdbcTypeMap.get(javaType);
        if (jdbcType != null) {
            return jdbcType;
        }
        return Types.OTHER;
    }

    /**
     * 根据 Java 类型获取默认的 JDBC 类型
     * @param javaType Java 类型
     * @return 对应的 JDBC 类型代码，如果未找到则返回 Types.OTHER
     */
    public static int toSqlType(final Class<?> javaType) {
        if (BinaryModel.class.isAssignableFrom(javaType)) {
            return Types.BLOB;
        }
        if (Collection.class.isAssignableFrom(javaType) || (javaType.isArray() && javaType != byte[].class)) {
            return Types.ARRAY;
        }
        Integer jdbcType = javaTypeToJdbcTypeMap.get(javaType.getName());
        if (jdbcType != null) {
            return jdbcType;
        }
        return Types.OTHER;
    }

    /**
     * 检查是否包含指定 Java 类型的类型处理器
     * @param typeClass Java 类型
     * @return 如果存在对应的类型处理器则返回 true
     */
    public boolean hasTypeHandler(Class<?> typeClass) {
        Objects.requireNonNull(typeClass, "typeClass is null.");
        if (this.cachedByJavaType.containsKey(typeClass.getName())) {
            return true;
        }

        for (Class<?> abstractType : this.abstractCachedByJavaType.keySet()) {
            if (abstractType.isAssignableFrom(typeClass) || abstractType == typeClass) {
                return true;
            }
        }
        return false;
    }

    public boolean hasTypeHandler(String typeName) {
        Objects.requireNonNull(typeName, "typeName is null.");
        return this.cachedByJavaType.containsKey(typeName);
    }

    public boolean hasTypeHandler(int jdbcType) {
        return this.cachedByJdbcType.containsKey(jdbcType);
    }

    public boolean hasTypeHandler(Class<?> typeClass, int jdbcType) {
        Objects.requireNonNull(typeClass, "typeClass is null.");
        Map<Integer, TypeHandler> jdbcHandlerMap = this.cachedByCrossType.get(typeClass.getName());
        if (jdbcHandlerMap != null) {
            if (jdbcHandlerMap.containsKey(jdbcType)) {
                return true;
            }
        }

        for (Class<?> abstractType : this.abstractCachedByCrossType.keySet()) {
            if (abstractType.isAssignableFrom(typeClass) && this.abstractCachedByCrossType.get(abstractType).containsKey(jdbcType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据 Java 类型获取类型处理器
     * @param typeClass Java 类型
     * @return 对应的类型处理器，如果未找到则返回默认类型处理器
     */
    public TypeHandler getTypeHandler(Class<?> typeClass) {
        Objects.requireNonNull(typeClass, "typeClass is null.");
        String typeClassName = typeClass.getName();
        TypeHandler typeHandler = this.cachedByJavaType.get(typeClassName);
        if (typeHandler != null) {
            return typeHandler;
        }

        // maybe classType is array
        if (typeClass.isArray()) {
            typeHandler = this.cachedByJavaType.get(Object[].class.getName());
            if (typeHandler != null) {
                this.cachedByJavaType.put(typeClassName, typeHandler);
                return typeHandler;
            }
        }

        // maybe classType is abstract
        Class<?> bestMatch = null;
        for (Class<?> abstractType : this.abstractCachedByJavaType.keySet()) {
            if (abstractType.isAssignableFrom(typeClass) || abstractType == typeClass) {
                if (bestMatch == null || bestMatch.isAssignableFrom(abstractType)) {
                    bestMatch = abstractType;
                }
            }
        }
        if (bestMatch != null) {
            typeHandler = this.abstractCachedByJavaType.get(bestMatch);
        }

        // A fallback lookup does not register the Java type as a scalar type.
        if (typeHandler == null) {
            return this.defaultTypeHandler;
        }
        this.cachedByJavaType.put(typeClassName, typeHandler);
        return typeHandler;
    }

    /**
     * 根据 JDBC 类型获取类型处理器
     * @param jdbcType JDBC 类型代码
     * @return 对应的类型处理器，如果未找到则返回默认类型处理器
     */
    public TypeHandler getTypeHandler(int jdbcType) {
        TypeHandler typeHandler = this.cachedByJdbcType.get(jdbcType);
        return (typeHandler != null) ? typeHandler : this.defaultTypeHandler;
    }

    /**
     * 根据 typeClass 和 jdbcType 的映射关系查找对应的 TypeHandler。
     * - 如果不存在对应的 TypeHandler，那么通过 typeClass 单独查找。
     * - 如果 typeClass 也没有注册那么返回 {@link #getDefaultTypeHandler()}
     */
    public TypeHandler getTypeHandler(Class<?> typeClass, int jdbcType) {
        if (typeClass == null) {
            return this.defaultTypeHandler;
        }

        // find by classType and jdbcType
        String typeClassName = typeClass.getName();
        Map<Integer, TypeHandler> handlerMap = this.cachedByCrossType.get(typeClassName);
        if (handlerMap != null) {
            TypeHandler typeHandler = handlerMap.get(jdbcType);
            if (typeHandler != null) {
                return typeHandler;
            }
        }

        TypeHandler typeHandler = null;

        // maybe classType is abstract
        Class<?> bestMatch = null;
        for (Class<?> abstractType : this.abstractCachedByCrossType.keySet()) {
            if (abstractType.isAssignableFrom(typeClass) || abstractType == typeClass) {
                Map<Integer, TypeHandler> typeHandlerMap = this.abstractCachedByCrossType.get(abstractType);
                if (typeHandlerMap != null && typeHandlerMap.containsKey(jdbcType)) {
                    if (bestMatch == null || bestMatch.isAssignableFrom(abstractType)) {
                        bestMatch = abstractType;
                    }
                }
            }
        }
        if (bestMatch != null) {
            typeHandler = this.abstractCachedByCrossType.get(bestMatch).get(jdbcType);
        }

        // Java defaults are considered only after explicit cross-type mappings.
        if (typeHandler == null) {
            return this.getTypeHandler(typeClass);
        }
        register(jdbcType, typeClass, typeHandler);
        return typeHandler;
    }

    public TypeHandler getResultSetTypeHandler(ResultSet rs, int columnIndex, Class<?> targetType) throws SQLException {
        int jdbcType = rs.getMetaData().getColumnType(columnIndex);
        String columnTypeName = rs.getMetaData().getColumnTypeName(columnIndex);
        String columnClassName = rs.getMetaData().getColumnClassName(columnIndex);

        if ("YEAR".equalsIgnoreCase(columnTypeName)) {
            // TODO with mysql `YEAR` type, columnType is DATE. but getDate() throw Long cast Date failed.
            return this.getTypeHandler(Types.INTEGER);
        } else if (StringUtils.isNotBlank(columnClassName) && columnClassName.startsWith("oracle.")) {
            // TODO with oracle columnClassName is specifically customizes standard types, it specializes process.
            jdbcType = TypeHandlerRegistry.toSqlType(columnClassName);
            if (targetType != null) {
                return this.getTypeHandler(targetType, jdbcType);
            } else {
                return this.getTypeHandler(jdbcType);
            }
        }

        Class<?> columnTypeClass = targetType;
        if (columnTypeClass == null && StringUtils.isNotBlank(columnClassName)) {
            try {
                columnTypeClass = ResourcesUtils.classForName(columnClassName);
            } catch (ClassNotFoundException e) {
                /**/
            }
        }

        // Fall back to the JDBC type when the driver cannot report a Java class.
        if (columnTypeClass == null) {
            TypeHandler typeHandler = this.getTypeHandler(jdbcType);
            if (typeHandler == null) {
                typeHandler = this.defaultTypeHandler;
            }
            return typeHandler;
        }

        TypeHandler handler = null;
        if (this.hasTypeHandler(columnTypeClass, jdbcType)) {
            handler = this.getTypeHandler(columnTypeClass, jdbcType);
        } else if (this.hasTypeHandler(columnTypeClass)) {
            handler = this.getTypeHandler(columnTypeClass);
        }
        // A driver reporting Object must not hide known JDBC types such as BLOB or SQLXML.
        return handler == null || handler instanceof UnknownTypeHandler ? this.getTypeHandler(jdbcType) : handler;
    }

    /** 获取默认类型处理器 */
    public UnknownTypeHandler getDefaultTypeHandler() {
        return this.defaultTypeHandler;
    }

    public void setParameterValue(final PreparedStatement ps, final int parameterPosition, final Object value) throws SQLException {
        Object parameter = value;
        Integer jdbcType = null;
        TypeHandler handler = null;
        if (value instanceof SqlArg) {
            SqlArg arg = (SqlArg) value;
            parameter = arg.getValue();
            jdbcType = arg.getJdbcType();
            handler = arg.getTypeHandler();
        }
        if (handler == null && parameter != null) {
            Class<?> javaType = parameter.getClass();
            handler = jdbcType == null ? this.getTypeHandler(javaType) : this.getTypeHandler(javaType, jdbcType);
        }
        if (handler == null && jdbcType != null) {
            handler = this.getTypeHandler(jdbcType);
        }
        if (handler == null) {
            ps.setObject(parameterPosition, parameter);
            return;
        }
        if (jdbcType == null && parameter != null) {
            jdbcType = toSqlType(parameter.getClass());
        }
        handler.setParameter(ps, parameterPosition, parameter, jdbcType);
    }

    public void setParameterValue(final CallableStatement cs, final int parameterPosition, final Object value) throws SQLException {
        SqlMode sqlMode;
        Integer jdbcType;
        String typeName;
        Integer scale;
        if (value instanceof SqlArg) {
            sqlMode = ((SqlArg) value).getSqlMode();
            sqlMode = sqlMode == null ? SqlMode.In : sqlMode;
            jdbcType = ((SqlArg) value).getJdbcType();
            typeName = ((SqlArg) value).getJdbcTypeName();
            scale = ((SqlArg) value).getScale();
        } else {
            sqlMode = SqlMode.In;
            jdbcType = null;
            typeName = null;
            scale = null;
        }

        if (sqlMode.isIn()) {
            this.setParameterValue((PreparedStatement) cs, parameterPosition, value);
        }

        if (sqlMode.isOut()) {
            if (sqlMode == SqlMode.Cursor) {
                jdbcType = JdbcHelper.getCursorJdbcType(JdbcHelper.getDbType(cs));
                cs.registerOutParameter(parameterPosition, jdbcType);
            } else {
                if (jdbcType == null) {
                    throw new SQLException("jdbcType must not be null");
                }

                if (typeName != null) {
                    cs.registerOutParameter(parameterPosition, jdbcType, typeName);
                } else if (scale != null) {
                    cs.registerOutParameter(parameterPosition, jdbcType, scale);
                } else {
                    cs.registerOutParameter(parameterPosition, jdbcType);
                }
            }
        }
    }

    private static boolean isAbstract(Class<?> javaType) {
        if (javaType.isArray()) {
            javaType = javaType.getComponentType();
        }
        if (javaType.isPrimitive()) {
            return false;
        }

        int modifiers = javaType.getModifiers();
        if (javaType.isInterface() || Modifier.isAbstract(modifiers)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 从 {@link CallableStatement} 获取输出参数值
     * @param cs {@link CallableStatement} 对象
     * @param i 参数位置
     * @param arg 参数配置
     * @return 参数值
     */
    public Object getParameterValue(CallableStatement cs, int i, SqlArg arg) throws SQLException {
        TypeHandler argHandler = arg.getTypeHandler();
        Integer argJdbcType = arg.getJdbcType();

        if (argHandler == null) {
            if (argJdbcType != null && this.hasTypeHandler(argJdbcType)) {
                argHandler = this.getTypeHandler(argJdbcType);
            } else {
                argHandler = this.getDefaultTypeHandler();
            }
        }

        return argHandler.getResult(cs, i);
    }
}
