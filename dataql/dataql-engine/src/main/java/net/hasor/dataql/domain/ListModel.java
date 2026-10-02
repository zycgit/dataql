/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.domain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 集合类型结果集
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class ListModel implements DataModel {
    private final List<DataModel> dataModel = new ArrayList<>();

    public ListModel() {
    }

    public ListModel(Collection<?> dataItem) {
        if (dataItem != null) {
            for (Object object : dataItem) {
                this.add(DomainHelper.convertTo(object));
            }
        }
    }

    /** 向集合的末尾追加一个元素 */
    public void add(Object object) {
        this.dataModel.add(DomainHelper.convertTo(object));
    }

    /** 集合大小 */
    public int size() {
        return this.dataModel.size();
    }

    @Override
    public List<DataModel> asOri() {
        return this.dataModel;
    }

    @Override
    public List<Object> unwrap() {
        List<Object> unwrap = new ArrayList<>(this.dataModel.size());
        this.dataModel.forEach(dataModel -> {
            unwrap.add(dataModel.unwrap());
        });
        return unwrap;
    }

    @Override
    public boolean isBinary() {
        return false;
    }

    /** 判断是否为 ListModel 类型值 */
    public boolean isList() {
        return true;
    }

    /** 判断是否为 ValueModel 类型值 */
    public boolean isValue(int index) {
        return this.dataModel.get(index) instanceof ValueModel;
    }

    /** 判断是否为 ListModel 类型值 */
    public boolean isList(int index) {
        return this.dataModel.get(index) instanceof ListModel;
    }

    /** 判断是否为 ObjectModel 类型值 */
    public boolean isObject(int index) {
        return this.dataModel.get(index) instanceof ObjectModel;
    }

    /** 判断是否为 UdfModel 类型值 */
    public boolean isUdf(int index) {
        return this.dataModel.get(index) instanceof UdfModel;
    }

    /** 获取某一个元素 */
    public DataModel get(int index) {
        return this.dataModel.get(index);
    }

    /** 将某一个元素转换为 ValueModel */
    public ValueModel getValue(int index) {
        Object dataItem = this.dataModel.get(index);
        if (dataItem instanceof ValueModel) {
            return (ValueModel) dataItem;
        }
        throw new ClassCastException(dataItem.getClass() + " not Cast to ValueModel.");
    }

    /** 将某一个元素转换为 ListModel */
    public ListModel getList(int index) {
        Object dataItem = this.dataModel.get(index);
        if (dataItem instanceof ListModel) {
            return (ListModel) dataItem;
        }
        throw new ClassCastException(dataItem.getClass() + " not Cast to ListModel.");
    }

    /** 将某一个元素转换为 ObjectModel */
    public ObjectModel getObject(int index) {
        Object dataItem = this.dataModel.get(index);
        if (dataItem instanceof ObjectModel) {
            return (ObjectModel) dataItem;
        }
        throw new ClassCastException(dataItem.getClass() + " not Cast to ObjectModel.");
    }

    /** 将某一个元素转换为 UdfModel */
    public UdfModel getUdf(int index) {
        Object dataItem = this.dataModel.get(index);
        if (dataItem instanceof UdfModel) {
            return (UdfModel) dataItem;
        }
        throw new ClassCastException(dataItem.getClass() + " not Cast to UdfModel.");
    }
}
