/*
 * Copyright 2008-2009 the original author or authors.
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
package net.hasor.dataql.host.function.basic;
import java.util.LinkedHashMap;
import java.util.Map;
import net.hasor.cobble.function.ESupplier;
import net.hasor.dataql.domain.Udf;
import net.hasor.dataql.host.function.AbstractUdfSource;
import net.hasor.dataql.kernel.Finder;

/**
 * 带有状态的集合。函数库引入 <code>import 'net.hasor.dataql.fx.basic.CollectionUdfSource' as collect; var arr = collect.newList()</code>
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2019-12-12
 */
class InnerMapStateUdfSource extends AbstractUdfSource {
    private final Map<String, Object> objectMap;
    private final Map<String, Udf>    self;

    public InnerMapStateUdfSource(Map<String, Object> initData) {
        if (initData != null) {
            objectMap = initData;
        } else {
            objectMap = new LinkedHashMap<>();
        }
        //
        this.self = this.buildUdfMap(this, this.getPredicate(this.getClass()));
    }

    @Override
    public ESupplier<Map<String, Udf>, Exception> getUdfResource(Finder finder) {
        return () -> this.self;
    }

    /** 把参数数据加到开头 */
    public Map<String, Udf> put(String key, Object dataValue) {
        this.objectMap.put(key, dataValue);
        return this.self;
    }

    /** 把参数数据加到末尾 */
    public Map<String, Udf> putAll(Map<String, Object> dataMap) {
        if (dataMap != null) {
            this.objectMap.putAll(dataMap);
        }
        return this.self;
    }

    /** 数组大小 */
    public int size() {
        return this.objectMap.size();
    }

    /**
     * 有状态集合的数据
     */
    public Map<String, Object> data() {
        return this.objectMap;
    }
}
