---
id: collect
title: 7.2 Collection functions
---

:::info Module dependency
This library is provided by `net.hasor:dataql-engine`. Add the dependency to your application, then import the library in your DataQL script.
:::

```javascript
import 'net.hasor.dataql.host.function.basic.CollectionUdfSource' as collect;
return collect.size([{'id':1},{'id':2}]);
// 2
```

All examples below use DataQL syntax. Prepend the import above when running an individual example. `values` denotes a list; `object` denotes an object with named fields and values. Square brackets in signatures denote optional arguments and are not written in calls. Callbacks use `(arguments) -> { return result; }`; each function describes their argument order.

## isEmpty

`boolean isEmpty(value)`: For a list or object, checks whether value has no elements or fields. Null and unsupported scalar types return false.

```javascript
return [collect.isEmpty([]), collect.isEmpty({}), collect.isEmpty(null)];
// [true,true,false]
```

## size

`int size(value)`: Returns the number of list elements or object fields in value. Null returns 0; other single values count as one element. This does not count characters in a string.

```javascript
return [collect.size([1,2]), collect.size({'name':'Alice'}), collect.size(null)];
// [2,1,0]
```

## merge

`List merge(value...)`: Accepts any number of lists or single values. Flattens lists by one level into a new list in argument order. Null adds no element; nested lists are retained.

```javascript
return collect.merge([1,2], 3, [4,[5]], null);
// [1,2,3,4,[5]]
```

## mergeMap

`Map mergeMap(object...)`: Merges fields from any number of objects into a new object. Later values replace earlier values for the same field; nested objects are not merged recursively. Null, lists, and other non-object inputs cause an error.

```javascript
return collect.mergeMap({'name':'Alice','age':18}, {'age':20});
// {"name":"Alice","age":20}
```

## filter

`List filter(values[, predicate])`: Values is the input list. Predicate(value) returns a boolean for each element; matching elements retain their order. A null/empty input returns null; filtering out every element of a nonempty list returns an empty list. An omitted/null predicate retains the original list.

```javascript
return collect.filter([1,2,3], (value) -> { return value > 1; });
// [2,3]
```

## filterMap

`Map filterMap(object[, predicate])`: Predicate(key) receives each field name and returns a boolean; only matching fields and their original values are retained. An omitted/null predicate retains the original object. When a callback is supplied, object must be non-null. An empty object returns an empty object.

```javascript
return collect.filterMap({'name':'Alice','age':18}, (key) -> { return key == 'name'; });
// {"name":"Alice"}
```

## limit

`List limit(values, start, count)`: Takes at most count elements from values starting at start. Start is zero-based; negative values act as 0. A nonpositive count takes all remaining elements. A null/empty list returns null; starting beyond a nonempty list returns an empty list.

```javascript
return collect.limit([0,1,2,3,4], 1, 2);
// [1,2]
```

## newList

`Map newList([initial])`: Creates an operation object that holds list state. Initial may be a list or a single value; omitted/null initial creates an empty list. The returned object contains functions; use data() to obtain the actual list.

| Call | Arguments and result |
| --- | --- |
| `values.addFirst(value)` | Prepends one value or a list flattened by one level, preserving the list's internal order. |
| `values.addLast(value)` | Appends one value or a list flattened by one level. |
| `values.size()` | Returns the current element count. |
| `values.data()` | Returns the current list data. |

Both addition methods return the same operation object for chaining. Passing `null` adds nothing; passing `[null]` adds one null element.

```javascript
var values = collect.newList([2,3]);
run values.addFirst([0,1]);
run values.addLast(4);
run values.addLast(null);
return {'size':values.size(), 'data':values.data()};
// {"size":5,"data":[0,1,2,3,4]}
```

## newMap

`Map newMap([initial])`: Creates an operation object that holds object state. Initial is the initial object; omitted/null initial creates an empty object. The returned object contains functions; use data() to obtain the actual data.

| Call | Arguments and result |
| --- | --- |
| `values.put(key,value)` | Key is the field name; replaces an existing value for the same name. Value may be null. |
| `values.putAll(object)` | Merges fields, replacing existing values for the same names. Null makes no change. |
| `values.size()` | Returns the current field count. |
| `values.data()` | Returns the current object data. |

Both write methods return the same operation object for chaining.

```javascript
var values = collect.newMap({'name':'Alice','age':18});
run values.put('age',20);
run values.putAll({'city':'Shanghai'});
return {'size':values.size(), 'data':values.data()};
// {"size":3,"data":{"name":"Alice","age":20,"city":"Shanghai"}}
```

## mapJoin

`List mapJoin(left, right, fields)`: Left and right are lists of objects. Each key in fields names a left-side field, and its value names the corresponding right-side field, for example `{'id':'owner'}`. Multiple field pairs form a combined match. Each left record produces an object containing data1 (the left record) and data2 (the matching right record), in left-list order. Duplicate right-side keys keep the last record; unmatched data2 is null. One-to-many matches are not expanded.

```javascript
return collect.mapJoin([{'id':1},{'id':2}], [{'owner':1,'name':'Alice'}], {'id':'owner'});
// [{"data1":{"id":1},"data2":{"owner":1,"name":"Alice"}},{"data1":{"id":2},"data2":null}]
```

Join fields should exist. The implementation builds join keys from string representations: numeric `1` can match string `"1"`, and two `null` values can match. Normalize join fields beforehand when types must remain distinct.

## mapKeyToLowerCase

`Map mapKeyToLowerCase(object)`: Returns a new object with every field name lowercased and values unchanged. Name collisions keep the later value. Null returns an empty object.

```javascript
return collect.mapKeyToLowerCase({'USER_ID':1});
// {"user_id":1}
```

## mapKeyToUpperCase

`Map mapKeyToUpperCase(object)`: Uppercases all field names in a new object, preserving values. Name collisions keep the later value; null returns an empty object.

```javascript
return collect.mapKeyToUpperCase({'user_id':1});
// {"USER_ID":1}
```

## mapKeyToHumpCase

`Map mapKeyToHumpCase(object)`: Lowercases each field name, then converts underscore naming to lower camel case, preserving values. An existing camel-case name such as userName therefore becomes username. Name collisions keep the later value; null returns an empty object.

```javascript
return collect.mapKeyToHumpCase({'USER_ID':1});
// {"userId":1}
```

## mapKeys

`List mapKeys(object)`: Returns field names in the traversal order of object. A null/empty object returns an empty list.

```javascript
return collect.mapKeys({'name':'Alice','age':18});
// ["name","age"]
```

## mapValues

`List mapValues(object)`: Returns field values in the traversal order of object, matching the positions returned by mapKeys. A null/empty object returns an empty list.

```javascript
return collect.mapValues({'name':'Alice','age':18});
// ["Alice",18]
```

## mapKeyReplace

`Map mapKeyReplace(object[, callback])`: Callback(key,value) receives each original field name and value and returns a new name, converted to a string. Original values are retained; name collisions keep the later value. An omitted/null callback retains the original object. Null/empty objects are unchanged.

```javascript
return collect.mapKeyReplace({'name':'Alice'}, (key,value) -> { return 'user_' + key; });
// {"user_name":"Alice"}
```

## mapValueReplace

`Map mapValueReplace(object[, callback])`: Callback(key,value) receives each original field name and value and returns the replacement value, which may be a scalar, object, list, or null. Field names stay unchanged. An omitted/null callback retains the original object; null/empty objects are unchanged.

```javascript
return collect.mapValueReplace({'a':1,'b':2}, (key,value) -> { return value * 10; });
// {"a":10,"b":20}
```

## list2map

`Map list2map(values, key[, convert])`: Converts the values list to an object. Key may be a field name (each row must be an object with a scalar field value) or a callback(index,value) returning a scalar key. Index is zero-based and value is the current element. Convert(index,value) optionally transforms each output value; omitted/null convert keeps the original element. Keys become strings; duplicate keys keep the later value.

```javascript
return collect.list2map([{'id':1,'name':'Alice'},{'id':2,'name':'Bob'}], 'id', (index,row) -> { return row.name; });
// {"1":"Alice","2":"Bob"}
```

```javascript
return collect.list2map(['Alice','Bob'], (index,value) -> { return 'user_' + index; });
// {"user_0":"Alice","user_1":"Bob"}
```

A failure to extract a key or convert a row is collected under `errorData.idx_index` in the result, with `errorMsg` and `errorData` fields. If the name is occupied, `errorData_1` and so on are used. `exit` still terminates the whole query. A null key or a key that is neither a string nor a function causes an immediate error. With a valid key, a null/empty list returns an empty object.

## map2list

`List map2list(object[, callback])`: Builds a list in object-field order. Without callback, each element is a `{key,value}` object; with callback(key,value), each element is the callback result. A null/empty object returns an empty list.

```javascript
return collect.map2list({'a':1,'b':2}, (key,value) -> { return key + ':' + value; });
// ["a:1","b:2"]
```

```javascript
return collect.map2list({'a':1,'b':2});
// [{"key":"a","value":1},{"key":"b","value":2}]
```

## map2string

`String map2string(object, separator, callback)`: Callback(key,value) receives each field name and value and produces its text. Results are joined in field order with separator, without a trailing delimiter. Nonempty objects require non-null separator and callback arguments. A null/empty object returns an empty string.

```javascript
return collect.map2string({'a':1,'b':2}, '&', (key,value) -> { return key + '=' + value; });
// a=1&b=2
```

## mapSort

`Map mapSort(object[, comparator])`: Returns a new object sorted by field name, with values unchanged. Comparator(leftKey,rightKey) returns a negative number, 0, or a positive number to place the left name before, equal to, or after the right name. An omitted/null comparator sorts by hashCode rather than natural order. Null returns an empty object.

```javascript
import 'net.hasor.dataql.host.function.basic.CollectionUdfSource' as collect;
import 'net.hasor.dataql.host.function.basic.StringUdfSource' as string;
return collect.mapSort({'b':2,'a':1}, (left,right) -> { return string.compareString(left,right); });
// {"a":1,"b":2}
```

## listSort

`List listSort(values[, comparator])`: Comparator(left,right) receives two elements and returns a negative number, 0, or a positive number to place left before, equal to, or after right. An omitted/null comparator sorts by hashCode, treating null as hashCode 0. Returns the sorted list; null input returns an empty list. Use the return value to obtain the sorted result in scripts.

```javascript
return collect.listSort([3,1,2], (left,right) -> { return left < right ? -1 : (left == right ? 0 : 1); });
// [1,2,3]
```

## groupBy

`Map groupBy(values, fieldName)`: Values is a list of objects; fieldName names a scalar field present in every row. Groups rows by that field, preserving row order within each group. Result keys become strings; a null field value uses the key "null". A null/empty list returns an empty object.

```javascript
return collect.groupBy([{'id':1,'team':'A'},{'id':2,'team':'A'}], 'team');
// {"A":[{"id":1,"team":"A"},{"id":2,"team":"A"}]}
```

Use a consistent value type for the grouping field. Group comparison distinguishes types, but output keys become strings: numeric `1` and string `"1"` share a result key, so the later group replaces the earlier group.

## uniqueBy

`List uniqueBy(values, fieldName)`: Values is a list of objects; fieldName names a scalar field present in every row. Deduplicates by that field. When field-value types are consistent, keeps the first row of each group in first-occurrence order. A null/empty list returns an empty list.

```javascript
return collect.uniqueBy([{'id':1,'team':'A'},{'id':2,'team':'A'}], 'team');
// [{"id":1,"team":"A"}]
```

Grouping and deduplication both use list2map, so stringified field values must not collide. Row errors such as missing fields appear under `errorData` in groupBy. UniqueBy takes the values of that result, so its list can include an extra error object. Ensure every row contains the common field before calling.
