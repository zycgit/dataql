/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.domain;
import java.util.*;
import net.hasor.cobble.ArrayUtils;
import net.hasor.cobble.ref.BeanMap;

public class DomainHelper {
    public static ValueModel nullDomain() {
        return ValueModel.NULL;
    }

    public static ListModel newList() {
        return new ListModel();
    }

    public static ObjectModel newObject() {
        return new ObjectModel();
    }

    public static DataModel convertTo(Object object) {
        if (object instanceof DataModel) {
            // 已经是 DataModel
            return (DataModel) object;
        } else if (object == null) {
            // 基础类型：空
            return ValueModel.NULL;
        } else if (object instanceof Boolean) {
            // 基础类型：boolean
            if ((boolean) object) {
                return ValueModel.TRUE;
            } else {
                return ValueModel.FALSE;
            }
        } else if (object instanceof CharSequence || object instanceof Character) {
            // 基础类型：字符串
            return new ValueModel(String.valueOf(object));
        } else if (object instanceof Number) {
            // 基础类型：数字
            return new ValueModel(object);
        } else if (object instanceof Date) {
            // 外部类型：时间 -> Long
            return new ValueModel(((Date) object).getTime());
        } else if (object instanceof UUID) {
            // 外部类型：UUID -> String
            return new ValueModel(object.toString());
        } else if (object instanceof Enum<?>) {
            // 外部类型：枚举 -> ValueModel（字符串）
            return new ValueModel(((Enum<?>) object).name());
        } else if (object instanceof Map mapData) {
            // 外部类型：Map -> ObjectModel
            Set entrySet = mapData.entrySet();
            ObjectModel objectModel = new ObjectModel();
            for (Object entry : entrySet) {
                if (entry instanceof Map.Entry) {
                    Object key = ((Map.Entry) entry).getKey();
                    Object val = ((Map.Entry) entry).getValue();
                    objectModel.put(key.toString(), convertTo(val));
                }
            }
            return objectModel;
        } else if (object.getClass().isArray()) {
            // 外部类型：数组 -> ListModel
            Class<?> componentType = object.getClass().getComponentType();
            Object[] objectArrays = null;
            if (componentType.isPrimitive()) {
                /**  */if (Boolean.TYPE == componentType) {
                    objectArrays = ArrayUtils.toObject((boolean[]) object);
                } else if (Byte.TYPE == componentType) {
                    objectArrays = ArrayUtils.toObject((byte[]) object);
                } else if (Short.TYPE == componentType) {
                    objectArrays = ArrayUtils.toObject((short[]) object);
                } else if (Integer.TYPE == componentType) {
                    objectArrays = ArrayUtils.toObject((int[]) object);
                } else if (Long.TYPE == componentType) {
                    objectArrays = ArrayUtils.toObject((long[]) object);
                } else if (Character.TYPE == componentType) {
                    objectArrays = ArrayUtils.toObject((char[]) object);
                } else if (Float.TYPE == componentType) {
                    objectArrays = ArrayUtils.toObject((float[]) object);
                } else if (Double.TYPE == componentType) {
                    objectArrays = ArrayUtils.toObject((double[]) object);
                } else {
                    objectArrays = (Object[]) object;
                }
            } else {
                objectArrays = (Object[]) object;
            }
            return new ListModel(Arrays.asList(objectArrays));
        } else if (object instanceof Collection) {
            // 外部类型：集合 -> ListModel
            return new ListModel((Collection<?>) object);
        } else if (object instanceof Udf) {
            // 外部类型：UDF -> CallModel
            return new UdfModel((Udf) object);
        } else {
            // 外部类型：Bean -> ObjectModel
            BeanMap beanMap = new BeanMap(object);
            ObjectModel objectModel = new ObjectModel();
            for (String entryKey : beanMap.keySet()) {
                if ("class".equals(entryKey)) {
                    //objectModel.put(entryKey, convertTo(beanMap.getBean().getClass().getName()));
                } else {
                    objectModel.put(entryKey, convertTo(beanMap.get(entryKey)));
                }
            }
            return objectModel;
        }
    }
}
