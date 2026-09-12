/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.mem;
import java.util.Iterator;
import net.hasor.dataql.domain.DomainHelper;

/**
 * 数据迭代器
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2019-11-22
 */
public class DataIterator {
    private Iterator iterator = null;
    private Object   oriData  = null;
    private Object   data     = null;

    public DataIterator(Object oriData, Iterator iterator) {
        this.oriData = oriData;
        this.iterator = iterator;
    }

    public Object getData() {
        return data;
    }

    public boolean isNext() {
        if (this.iterator.hasNext()) {
            this.data = DomainHelper.convertTo(iterator.next());
            return true;
        }
        return false;
    }

    public Object getOriData() {
        return this.oriData;
    }
}
