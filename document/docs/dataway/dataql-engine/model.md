---
title: "8.2 数据模型"
description: "DataModel 的类型、Java 数据转换、结果读取与二进制资源处理。"
---

DataQL 使用 `DataModel` 表示运行数据和查询结果。Java 调用通过 `QueryResult.getData()` 获取模型，通过 `unwrap()` 取得普通 Java 数据。

## 模型类型

| 模型 | 判断方法 | 内容 |
| --- | --- | --- |
| `ValueModel` | `isValue()` | 字符串、数字、布尔值、null |
| `ListModel` | `isList()` | 有序列表 |
| `ObjectModel` | `isObject()` | 按插入顺序保存的字段 |
| `UdfModel` | `isUdf()` | 可调用的函数 |
| `BinaryModel` | `isBinary()` | 文件、字节或输入流等二进制内容 |

## Java 数据转换

`DomainHelper.convertTo(value)` 将 Java 数据转为模型，并递归转换对象字段和列表元素。

| Java 输入 | 转换结果 |
| --- | --- |
| `null`、Boolean、Number | ValueModel，数值保留原 Java 类型 |
| Character、CharSequence、UUID、Enum | 字符串 ValueModel；枚举使用 `name()` |
| Date | 毫秒时间戳 ValueModel |
| Map、Java Bean | ObjectModel；Map 键转为字符串，Bean 读取可读属性并排除 `class` |
| Collection、数组 | ListModel；`char[]` 为字符字符串列表，`byte[]` 为数值列表 |
| Udf | UdfModel |
| DataModel | 保留原实例，包括 BinaryModel |

输入数据应无循环引用。Map 键不能为 null，转为字符串后同名的键由后值覆盖。

## 读取与修改

`ObjectModel.put` 和 `ListModel.add` 自动转换写入值；`get` 返回 DataModel，`getValue`、`getList`、`getObject`、`getUdf` 按类型读取。

```java
import java.util.Map;
import net.hasor.dataql.domain.DomainHelper;
import net.hasor.dataql.domain.ListModel;
import net.hasor.dataql.domain.ObjectModel;

ObjectModel user = (ObjectModel) DomainHelper.convertTo(Map.of("name", "Alice", "age", 18));
user.put("nickname", null);
String name = user.getValue("name").asString(); // Alice
int age = user.getValue("age").asInt();         // 18
boolean empty = user.getValue("nickname").isNull(); // true
boolean absent = user.get("unknown") == null;       // true

ListModel users = new ListModel();
users.add(user);
String firstName = users.getObject(0).getValue("name").asString(); // Alice
```

- 对象字段名区分大小写。字段不存在时读取返回 null，类型判断返回 false；显式 null 保存为 ValueModel。
- 列表索引从 0 开始，Java 方法越界抛出 `IndexOutOfBoundsException`。脚本的负索引和 [INDEX_OVERFLOW](../../dataql/hints/hint_core.md#INDEX_OVERFLOW) 由引擎处理。
- 类型化读取遇到类型不匹配时抛出 `ClassCastException`。二进制值通过 `get(...).isBinary()` 判断。

### 值转换

ValueModel 提供 `isString()`、`isNumber()`、`isBoolean()`、`isNull()`。常用转换如下，转换不改变模型保存的值：

| 方法 | 行为 |
| --- | --- |
| `asString()` | 调用值的 `toString()`，null 返回 null |
| `asBoolean()` | 数字按非零判断；字符串支持 true/false（忽略大小写）、1/0；null 返回 false |
| `asNumber()` | 返回原始 Number，不解析字符串；null 返回整数 0 |
| `asInt()`、`asLong()`、`asDouble()` 等 | 转为对应数值类型，支持数字字符串和布尔值的 1/0；null 返回 0 |

`isInt()` 等数值判断包含兼容类型，例如 Byte 的 `isInt()` 为 true。需要精确类型时，先排除 null 再检查 `asOri().getClass()`。数值缩窄可能截断或溢出，无法转换时抛出异常。脚本中的转换函数见[转换函数库](../../dataql/funx/convert.md)。

## 解包结果

| 模型 | `asOri()` | `unwrap()` |
| --- | --- | --- |
| ValueModel | 原始值 | 原始值 |
| ListModel / ObjectModel | 内部可变的模型 List / Map | 新建普通 List / Map，递归解包并保留 null |
| UdfModel | 原始 Udf | 原始 Udf |
| BinaryModel | 模型本身 | 模型本身 |

通常使用 `put`、`add` 修改模型，使用 `unwrap()` 获取普通数据。解包不执行 JSON 序列化，也不读取二进制流。

## 二进制资源

通过 `BinaryValue` 显式创建二进制值，普通 `byte[]` 使用列表语义。二进制值在参数、变量和函数调用中保留引用。

```java
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.hasor.dataql.domain.BinaryValue;

try (BinaryValue content = new BinaryValue("Hello DataQL".getBytes(StandardCharsets.UTF_8));
     InputStream input = content.openStream()) {
    String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
    System.out.println(text);
}
```

- `BinaryValue(byte[])`：可重复打开流，直接保存数组引用，创建后应避免修改数组。
- `BinaryValue(InputStream)`：只能打开一次，`getSize()` 返回 -1 表示大小未知。
- 继承 `BinaryModel`：实现 `openStream()`，按需覆盖 `getSize()`。

调用方负责关闭读取流；关闭 BinaryValue 后不能再次打开。二进制不自动参与数值运算或 JSON 编码，HTTP 输出见[结果响应](../capabilities/development/response.md#binary-response)。

## 函数模型

UdfModel 包装 `Udf`，脚本中的 Lambda 和导入函数可作为值传递。Java 中通过 `DomainHelper.convertTo(udf)` 创建模型，`UdfModel.call(Hints, UdfParams)` 将函数返回值转为 DataModel。

调用方法声明 `throws Throwable`，Java 调用方需处理或继续声明；脚本调用由引擎提供参数。函数编写与注册见[自定义函数](../engine/functions.md)，Java 对象的方法导入见[应用对象导入](../engine/imports.md)。
