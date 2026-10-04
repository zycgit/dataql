---
id: collect
title: 7.2 集合函数
---

:::info 依赖模块
本库由 `net.hasor:dataql-engine` 提供。将该依赖加入应用后，在 DataQL 脚本中导入即可使用。
:::

```javascript
import 'net.hasor.dataql.host.function.basic.CollectionUdfSource' as collect;
return collect.size([{'id':1},{'id':2}]);
// 2
```

以下示例均为 DataQL 语法；执行单个示例时，先加入上面的 `import`。`values` 表示列表，`object` 表示字段名与字段值组成的对象；签名中的方括号表示可选参数，调用时不写方括号。回调使用 `(参数) -> { return 结果; }`，其参数顺序见各函数说明。

## isEmpty

`boolean isEmpty(value)`：value 为列表或对象时，判断是否没有元素或字段；null 和不支持的基本类型返回 false。

```javascript
return [collect.isEmpty([]), collect.isEmpty({}), collect.isEmpty(null)];
// [true,true,false]
```

## size

`int size(value)`：返回 value 的列表元素数或对象字段数；null 返回 0，其他单值按一个元素计算，不用于统计字符串字符数。

```javascript
return [collect.size([1,2]), collect.size({'name':'Alice'}), collect.size(null)];
// [2,1,0]
```

## merge

`List merge(value...)`：接收任意多个列表或单值，将列表展开一层，按参数顺序组成新列表；null 不添加元素，嵌套列表保留。

```javascript
return collect.merge([1,2], 3, [4,[5]], null);
// [1,2,3,4,[5]]
```

## mergeMap

`Map mergeMap(object...)`：接收任意多个对象，将字段合并到新对象；同名字段使用后面的值，不递归合并嵌套对象。null、列表等非对象输入抛出异常。

```javascript
return collect.mergeMap({'name':'Alice','age':18}, {'age':20});
// {"name":"Alice","age":20}
```

## filter

`List filter(values[, predicate])`：values 是待过滤列表；predicate(value) 对每个元素返回布尔值，保留 true 对应的元素及原顺序。null 或空列表返回 null；有数据但全部被过滤时返回空列表。predicate 省略或为 null 时保留原列表。

```javascript
return collect.filter([1,2,3], (value) -> { return value > 1; });
// [2,3]
```

## filterMap

`Map filterMap(object[, predicate])`：predicate(key) 接收字段名并返回布尔值，只保留 true 对应的字段及原值；回调省略或为 null 时保留原对象。传入回调时 object 必须是非 null 对象；空对象返回空对象。

```javascript
return collect.filterMap({'name':'Alice','age':18}, (key) -> { return key == 'name'; });
// {"name":"Alice"}
```

## limit

`List limit(values, start, count)`：从 values 的 start 位置开始，最多取 count 个元素。start 从 0 开始，负数按 0 处理；count 小于等于 0 时保留后续全部元素。null 或空列表返回 null；非空列表的 start 超出末尾时返回空列表。

```javascript
return collect.limit([0,1,2,3,4], 1, 2);
// [1,2]
```

## newList

`Map newList([initial])`：创建保存列表状态的操作对象；initial 可以是列表或单值，省略或为 null 时创建空列表。返回值包含操作函数，通过 data() 取得实际列表。

| 调用 | 参数与返回效果 |
| --- | --- |
| `values.addFirst(value)` | 将单值或展开一层后的列表加入开头，保留列表内部顺序。 |
| `values.addLast(value)` | 将单值或展开一层后的列表加入末尾。 |
| `values.size()` | 返回当前元素数。 |
| `values.data()` | 返回当前列表数据。 |

两个添加方法都返回同一操作对象，可连续调用；直接传 `null` 不添加元素，传 `[null]` 则会添加一个空元素。

```javascript
var values = collect.newList([2,3]);
run values.addFirst([0,1]);
run values.addLast(4);
run values.addLast(null);
return {'size':values.size(), 'data':values.data()};
// {"size":5,"data":[0,1,2,3,4]}
```

## newMap

`Map newMap([initial])`：创建保存对象状态的操作对象；initial 为初始对象，省略或为 null 时创建空对象。返回值包含操作函数，通过 data() 取得实际数据。

| 调用 | 参数与返回效果 |
| --- | --- |
| `values.put(key,value)` | key 为字段名；同名字段覆盖旧值，value 可以为 null。 |
| `values.putAll(object)` | 合并对象字段，同名字段使用新值；null 不产生修改。 |
| `values.size()` | 返回当前字段数。 |
| `values.data()` | 返回当前对象数据。 |

两个写入方法都返回同一操作对象，可连续调用。

```javascript
var values = collect.newMap({'name':'Alice','age':18});
run values.put('age',20);
run values.putAll({'city':'Shanghai'});
return {'size':values.size(), 'data':values.data()};
// {"size":3,"data":{"name":"Alice","age":20,"city":"Shanghai"}}
```

## mapJoin

`List mapJoin(left, right, fields)`：left、right 为对象列表；fields 的字段名指定左记录字段，字段值指定右记录字段，例如 `{'id':'owner'}`，多个字段同时参与匹配。按左列表顺序，为每条记录返回包含 data1（左记录）、data2（匹配右记录）的对象。右侧同键多条记录取最后一条，无匹配时 data2 为 null，不展开一对多结果。

```javascript
return collect.mapJoin([{'id':1},{'id':2}], [{'owner':1,'name':'Alice'}], {'id':'owner'});
// [{"data1":{"id":1},"data2":{"owner":1,"name":"Alice"}},{"data1":{"id":2},"data2":null}]
```

关联字段应存在。当前实现把字段值转为字符串组成关联键，因此数字 `1` 与字符串 `"1"` 可匹配，两个 `null` 也可匹配；需要区分类型时应先规范化关联字段。

## mapKeyToLowerCase

`Map mapKeyToLowerCase(object)`：将 object 的所有字段名转小写，字段值不变；转换后重名使用后值。返回新对象，null 返回空对象。

```javascript
return collect.mapKeyToLowerCase({'USER_ID':1});
// {"user_id":1}
```

## mapKeyToUpperCase

`Map mapKeyToUpperCase(object)`：将 object 的所有字段名转大写，字段值不变；转换后重名使用后值，null 返回空对象。

```javascript
return collect.mapKeyToUpperCase({'user_id':1});
// {"USER_ID":1}
```

## mapKeyToHumpCase

`Map mapKeyToHumpCase(object)`：先将字段名转小写，再将下划线命名转换为小驼峰，字段值不变。因此已有驼峰 userName 会变为 username；转换后重名使用后值，null 返回空对象。

```javascript
return collect.mapKeyToHumpCase({'USER_ID':1});
// {"userId":1}
```

## mapKeys

`List mapKeys(object)`：按 object 的字段遍历顺序返回字段名列表，null 或空对象返回空列表。

```javascript
return collect.mapKeys({'name':'Alice','age':18});
// ["name","age"]
```

## mapValues

`List mapValues(object)`：按 object 的字段遍历顺序返回字段值列表，与 mapKeys 的位置一一对应；null 或空对象返回空列表。

```javascript
return collect.mapValues({'name':'Alice','age':18});
// ["Alice",18]
```

## mapKeyReplace

`Map mapKeyReplace(object[, callback])`：callback(key,value) 接收原字段名和值，返回新字段名，结果转为字符串；保留原字段值，重名使用后值。回调省略或为 null 时保留原对象；null 或空对象保持原值。

```javascript
return collect.mapKeyReplace({'name':'Alice'}, (key,value) -> { return 'user_' + key; });
// {"user_name":"Alice"}
```

## mapValueReplace

`Map mapValueReplace(object[, callback])`：callback(key,value) 接收原字段名和值，返回替换后的字段值（可以是基本值、对象、列表或 null），字段名不变；回调省略或为 null 时保留原对象，null 或空对象保持原值。

```javascript
return collect.mapValueReplace({'a':1,'b':2}, (key,value) -> { return value * 10; });
// {"a":10,"b":20}
```

## list2map

`Map list2map(values, key[, convert])`：将 values 列表转换为对象。key 可以是字段名（各行必须是对象且该字段为基本值），也可以是返回基本值的 callback(index,value)。index 从 0 开始，value 是当前元素；convert(index,value) 可转换每个输出值，省略或为 null 时保留原元素。键统一转字符串，重复键后值覆盖前值。

```javascript
return collect.list2map([{'id':1,'name':'Alice'},{'id':2,'name':'Bob'}], 'id', (index,row) -> { return row.name; });
// {"1":"Alice","2":"Bob"}
```

```javascript
return collect.list2map(['Alice','Bob'], (index,value) -> { return 'user_' + index; });
// {"user_0":"Alice","user_1":"Bob"}
```

单行提取键或转换失败会收集到返回对象的 `errorData.idx_序号`，包含 `errorMsg` 和 `errorData`。若名称已占用，使用 `errorData_1` 等名称。`exit` 仍会结束整个查询。key 为 null 或不是字符串/函数时会直接报错；提供有效 key 后，null 或空列表返回空对象。

## map2list

`List map2list(object[, callback])`：按对象字段顺序生成列表；省略 callback 时，每个元素为 `{key,value}` 对象；提供 callback(key,value) 时，每个元素为回调返回值。null 或空对象返回空列表。

```javascript
return collect.map2list({'a':1,'b':2}, (key,value) -> { return key + ':' + value; });
// ["a:1","b:2"]
```

```javascript
return collect.map2list({'a':1,'b':2});
// [{"key":"a","value":1},{"key":"b","value":2}]
```

## map2string

`String map2string(object, separator, callback)`：callback(key,value) 接收字段名和值，生成该字段的文本；按字段顺序用 separator 连接，末尾不附加分隔符。非空对象需要非 null 的 separator 和 callback；null 或空对象返回空字符串。

```javascript
return collect.map2string({'a':1,'b':2}, '&', (key,value) -> { return key + '=' + value; });
// a=1&b=2
```

## mapSort

`Map mapSort(object[, comparator])`：按字段名排序并返回新对象，字段值不变。comparator(leftKey,rightKey) 返回负数、0 或正数，分别表示左字段名排在前面、顺序相等、排在后面；省略或为 null 时按 hashCode 排序，不是自然顺序。null 返回空对象。

```javascript
import 'net.hasor.dataql.host.function.basic.CollectionUdfSource' as collect;
import 'net.hasor.dataql.host.function.basic.StringUdfSource' as string;
return collect.mapSort({'b':2,'a':1}, (left,right) -> { return string.compareString(left,right); });
// {"a":1,"b":2}
```

## listSort

`List listSort(values[, comparator])`：comparator(left,right) 接收两个元素，返回负数、0 或正数，分别表示 left 排在前面、顺序相等、排在后面；省略或为 null 时按 hashCode 排序，其中 null 的 hashCode 按 0 处理。返回排序后的列表，null 输入返回空列表；脚本中应使用返回值取得排序结果。

```javascript
return collect.listSort([3,1,2], (left,right) -> { return left < right ? -1 : (left == right ? 0 : 1); });
// [1,2,3]
```

## groupBy

`Map groupBy(values, fieldName)`：values 为对象列表，fieldName 为每行都存在的基本值字段名。按该字段分组，每个键对应一个保留原行顺序的列表；结果键转为字符串，字段值 null 对应键 "null"。null 或空列表返回空对象。

```javascript
return collect.groupBy([{'id':1,'team':'A'},{'id':2,'team':'A'}], 'team');
// {"A":[{"id":1,"team":"A"},{"id":2,"team":"A"}]}
```

同一分组字段应使用一致的值类型。分组比较区分类型，但结果键会转为字符串；例如数字 `1` 与字符串 `"1"` 会争用同一结果键，后面的组覆盖前面的组。

## uniqueBy

`List uniqueBy(values, fieldName)`：values 为对象列表，fieldName 为每行都存在的基本值字段名。按该字段去重，字段值类型一致时保留每组第一条记录及首次出现的顺序；null 或空列表返回空列表。

```javascript
return collect.uniqueBy([{'id':1,'team':'A'},{'id':2,'team':'A'}], 'team');
// [{"id":1,"team":"A"}]
```

分组和去重都基于 list2map，字段值转为字符串后不可发生键冲突。字段缺失等行级错误在 groupBy 结果中进入 `errorData` 字段；uniqueBy 取其值列表，因此错误对象会作为额外元素混入返回列表。调用前应保证各行包含公共字段。
