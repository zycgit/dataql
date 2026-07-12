/*
 * Copyright 2015-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.dataql.sqlproc.types;
import java.util.Objects;

/**
 * 代表一个动态 SQL Build 之后的具体 SQL 和其参数
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-06-05
 */
public class SqlArg {
    private String      name;         // mappingTo #{xxx}
    private String      asName;       // mappingTo #{xxx,name=asName}
    private Object      value;
    private Integer     jdbcType;     // sqlMode = in/out/inout
    private TypeHandler typeHandler;  // sqlMode = in/out/inout
    // for procedure
    private SqlMode     sqlMode;
    private String      jdbcTypeName; // sqlMode = out/inout
    private Integer     scale;        // sqlMode = out/inout

    public SqlArg(Object value, Integer jdbcType, TypeHandler typeHandler) {
        this.value = value;
        this.typeHandler = typeHandler;
        this.jdbcType = jdbcType;
    }

    public SqlArg(String name, Object value, SqlMode sqlMode, Integer jdbcType, TypeHandler typeHandler) {
        this(value, jdbcType, typeHandler);
        this.name = name;
        this.sqlMode = sqlMode;
    }

    public static SqlArg valueOf(Object obj) {
        return new SqlArg(null, obj, SqlMode.In, null, null);
    }

    //    public static SqlArg valueOf(Object obj, String typeName) {
    //        if (StringUtils.isBlank(typeName)) {
    //            return new SqlArg(null, obj, SqlMode.In, null, null);
    //        } else {
    //            int jdbcType = TypeHandlerRegistry.toSqlType(typeName);
    //            TypeHandler typeHandler = TypeHandlerRegistry.DEFAULT.getTypeHandler(typeName);
    //            return new SqlArg(null, obj, SqlMode.In, jdbcType, typeHandler);
    //        }
    //    }

    public static SqlArg valueOf(Object obj, TypeHandler typeHandler) {
        return new SqlArg(null, obj, SqlMode.In, null, typeHandler);
    }

    public static SqlArg valueOf(Object obj, int jdbcType) {
        return new SqlArg(null, obj, SqlMode.In, jdbcType, null);
    }

    //    public static SqlArg asOut(String name, String typeName) {
    //        if (StringUtils.isBlank(typeName)) {
    //            return new SqlArg(name, null, SqlMode.Out, null, null);
    //        } else {
    //            int jdbcType = TypeHandlerRegistry.toSqlType(typeName);
    //            TypeHandler typeHandler = TypeHandlerRegistry.DEFAULT.getTypeHandler(typeName);
    //            return new SqlArg(name, null, SqlMode.Out, jdbcType, typeHandler);
    //        }
    //    }

    //    public static SqlArg asOut(String name, int jdbcType) {
    //        TypeHandler typeHandler = TypeHandlerRegistry.DEFAULT.getTypeHandler(jdbcType);
    //        return new SqlArg(name, null, SqlMode.Out, jdbcType, typeHandler);
    //    }

    public static SqlArg asOut(String name, int jdbcType, TypeHandler typeHandler) {
        return new SqlArg(name, null, SqlMode.Out, jdbcType, typeHandler);
    }

    //    public static SqlArg asInOut(String name, Object value, String typeName) {
    //        int jdbcType = TypeHandlerRegistry.toSqlType(typeName);
    //        TypeHandler typeHandler = TypeHandlerRegistry.DEFAULT.getTypeHandler(typeName);
    //        return new SqlArg(name, value, SqlMode.InOut, jdbcType, typeHandler);
    //    }

    //    public static SqlArg asInOut(String name, Object value, int jdbcType) {
    //        TypeHandler typeHandler = TypeHandlerRegistry.DEFAULT.getTypeHandler(jdbcType);
    //        return new SqlArg(name, value, SqlMode.InOut, jdbcType, typeHandler);
    //    }

    public static SqlArg asInOut(String name, Object value, int jdbcType, TypeHandler typeHandler) {
        return new SqlArg(name, value, SqlMode.InOut, jdbcType, typeHandler);
    }

    @Override
    public boolean equals(Object o) {
        return Objects.equals(o, this.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.value);
    }

    @Override
    public String toString() {
        return "SqlArg{" + "value=" + this.getValue() + '}';
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAsName() {
        return this.asName;
    }

    public void setAsName(String asName) {
        this.asName = asName;
    }

    public Object getValue() {
        return this.value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Integer getJdbcType() {
        return this.jdbcType;
    }

    public void setJdbcType(Integer jdbcType) {
        this.jdbcType = jdbcType;
    }

    public TypeHandler getTypeHandler() {
        return this.typeHandler;
    }

    public void setTypeHandler(TypeHandler typeHandler) {
        this.typeHandler = typeHandler;
    }

    public SqlMode getSqlMode() {
        return this.sqlMode;
    }

    public void setSqlMode(SqlMode sqlMode) {
        this.sqlMode = sqlMode;
    }

    public String getJdbcTypeName() {
        return this.jdbcTypeName;
    }

    public void setJdbcTypeName(String jdbcTypeName) {
        this.jdbcTypeName = jdbcTypeName;
    }

    public Integer getScale() {
        return this.scale;
    }

    public void setScale(Integer scale) {
        this.scale = scale;
    }
}
