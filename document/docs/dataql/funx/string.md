---
id: string
title: 7.1 字符串函数
---

:::info 依赖模块
本库由 `net.hasor:dataql-engine` 提供。将该依赖加入应用后，在 DataQL 脚本中导入即可使用。
:::

```javascript
import 'net.hasor.dataql.host.function.basic.StringUdfSource' as string;
return string.join(string.split('Alice,Bob', ','), ' / ');
// Alice / Bob
```

以下示例均为 DataQL 语法；执行单个示例时，先加入上面的 `import`。签名中的 `text` 表示待处理文本，`search` 表示待查找子串；字符串索引和长度按 Java UTF-16 字符单元计算，从 0 开始。方括号表示可选参数，调用时不写方括号。

## startsWith

`boolean startsWith(text, prefix)`：判断 text 是否以 prefix 开头，区分大小写；空 prefix 匹配任何非 null 文本。两个参数都为 null 时返回 true，仅一个为 null 时返回 false。

```javascript
return string.startsWith('DataQL', 'Data');
// true
```

## startsWithIgnoreCase

`boolean startsWithIgnoreCase(text, prefix)`：忽略大小写判断 text 是否以 prefix 开头；空前缀和 null 的规则与 startsWith 相同。

```javascript
return string.startsWithIgnoreCase('DataQL', 'data');
// true
```

## endsWith

`boolean endsWith(text, suffix)`：判断 text 是否以 suffix 结尾，区分大小写；空 suffix 匹配任何非 null 文本，null 规则与 startsWith 相同。

```javascript
return string.endsWith('DataQL', 'QL');
// true
```

## endsWithIgnoreCase

`boolean endsWithIgnoreCase(text, suffix)`：忽略大小写判断 text 是否以 suffix 结尾；空后缀和 null 的规则与 endsWith 相同。

```javascript
return string.endsWithIgnoreCase('DataQL', 'ql');
// true
```

## lineToHump

`String lineToHump(text)`：先将 text 转小写，再去掉英文字母、数字或下划线前的下划线，并将紧随的字母转大写，适合 USER_NAME 这类标识符；末尾孤立下划线保留，null 返回 null。

```javascript
return string.lineToHump('USER_NAME');
// userName
```

## humpToLine

`String humpToLine(text)`：在 text 的大写英文字母前加下划线并转小写，合并连续下划线，去除开头的下划线。null 返回 null，空字符串返回空字符串。

```javascript
return string.humpToLine('UserName');
// user_name
```

## firstCharToUpperCase

`String firstCharToUpperCase(text)`：将以英文字母开头的 text 的首字母转大写，其余字符不变。null、空字符串和纯空白文本保持原值；当前实现按字符码偏移处理首字符，调用前应保证首字符是英文字母。

```javascript
return string.firstCharToUpperCase('dataQL');
// DataQL
```

## firstCharToLowerCase

`String firstCharToLowerCase(text)`：将以英文字母开头的 text 的首字母转小写，其余字符不变；输入范围和 null、空白规则与 firstCharToUpperCase 相同。

```javascript
return string.firstCharToLowerCase('DataQL');
// dataQL
```

## toUpperCase

`String toUpperCase(text)`：将整个 text 转大写，使用 Java 默认地区规则；null 返回 null，空字符串保持不变。

```javascript
return string.toUpperCase('DataQL');
// DATAQL
```

## toLowerCase

`String toLowerCase(text)`：将整个 text 转小写，使用 Java 默认地区规则；null 返回 null，空字符串保持不变。

```javascript
return string.toLowerCase('DataQL');
// dataql
```

## indexOf

`int indexOf(text, search)`：返回 search 在 text 中首次出现的位置，从 0 开始；未找到或任一参数为 null 时返回 -1，空 search 返回 0。

```javascript
return string.indexOf('abcabc', 'bc');
// 1
```

## indexOfWithStart

`int indexOfWithStart(text, search, start)`：从 start 指定的位置向后查找 search，包含 start；返回相对于整个 text 的索引。负 start 按 0 处理；未找到或任一文本参数为 null 时返回 -1。

```javascript
return string.indexOfWithStart('abcabc', 'bc', 2);
// 4
```

## indexOfIgnoreCase

`int indexOfIgnoreCase(text, search)`：忽略大小写查找 search 首次出现的位置；未找到或任一参数为 null 时返回 -1。

```javascript
return string.indexOfIgnoreCase('AbCaBc', 'bc');
// 1
```

## indexOfIgnoreCaseWithStart

`int indexOfIgnoreCaseWithStart(text, search, start)`：从 start 指定的位置向后查找 search，忽略大小写；起点包含在查找范围内，负 start 按 0 处理，失败返回 -1。

```javascript
return string.indexOfIgnoreCaseWithStart('AbCaBc', 'bc', 2);
// 4
```

## lastIndexOf

`int lastIndexOf(text, search)`：返回 search 最后一次出现的起始索引；未找到或任一参数为 null 时返回 -1，空 search 返回文本长度。

```javascript
return string.lastIndexOf('abcabc', 'bc');
// 4
```

## lastIndexOfWithStart

`int lastIndexOfWithStart(text, search, start)`：向前查找 search，匹配的起始索引必须小于等于 start；start 从 0 开始，负数返回 -1，超过文本末尾则从末尾查找。

```javascript
return string.lastIndexOfWithStart('abcabc', 'bc', 3);
// 1
```

## lastIndexOfIgnoreCase

`int lastIndexOfIgnoreCase(text, search)`：忽略大小写查找 search 最后一次出现的起始索引；未找到或任一参数为 null 时返回 -1。

```javascript
return string.lastIndexOfIgnoreCase('AbCaBc', 'ab');
// 3
```

## lastIndexOfIgnoreCaseWithStart

`int lastIndexOfIgnoreCaseWithStart(text, search, start)`：从 start 指定的位置向前查找 search，忽略大小写；起点和返回值规则与 lastIndexOfWithStart 相同。

```javascript
return string.lastIndexOfIgnoreCaseWithStart('AbCaBc', 'ab', 2);
// 0
```

## contains

`boolean contains(text, search)`：判断 text 是否包含整个 search 子串，区分大小写；任一参数为 null 时返回 false，空 search 匹配任何非 null 文本。

```javascript
return string.contains('DataQL', 'Data');
// true
```

## containsIgnoreCase

`boolean containsIgnoreCase(text, search)`：忽略大小写判断 text 是否包含 search 子串；null 和空子串规则与 contains 相同。

```javascript
return string.containsIgnoreCase('DataQL', 'data');
// true
```

## containsAny

`boolean containsAny(text, searches)`：searches 是待匹配的字符串列表，任一完整子串命中时返回 true；空文本、null 文本、null 或空列表返回 false，列表中的 null 不参与匹配。

```javascript
return string.containsAny('DataQL', ['SQL', 'Data']);
// true
```

## containsAnyIgnoreCase

`boolean containsAnyIgnoreCase(text, searches)`：忽略大小写匹配 searches 列表中的任一完整子串；输入为空或 null 的规则与 containsAny 相同。

```javascript
return string.containsAnyIgnoreCase('DataQL', ['sql', 'data']);
// true
```

## trim

`String trim(text)`：移除 text 两端字符码不大于 U+0020 的字符（包括空格、制表符和换行），保留中间空白；null 返回 null。

```javascript
return string.trim(' DataQL ');
// DataQL
```

## sub

`String sub(text, start, end)`：截取 text 中包含 start、不包含 end 的区间。索引从 0 开始，负数从末尾计算（-1 表示最后一个字符的位置），越界收缩到有效范围；start 大于 end 返回空字符串，null 文本返回 null。

```javascript
return string.sub('DataQL', 0, 4);
// Data
```

```javascript
return string.sub('DataQL', -2, 6);
// QL
```

## left

`String left(text, length)`：取 text 左侧最多 length 个字符；length 小于等于 0 时返回空字符串，大于文本长度时返回全文，null 文本返回 null。

```javascript
return string.left('DataQL', 4);
// Data
```

## right

`String right(text, length)`：取 text 右侧最多 length 个字符；length 小于等于 0 时返回空字符串，大于文本长度时返回全文，null 文本返回 null。

```javascript
return string.right('DataQL', 2);
// QL
```

## alignRight

`String alignRight(text, padding, length)`：在 text 右侧补字符，使结果总长度达到 length。padding 必须为非 null 字符串，只使用首字符；空字符串表示用空格填充。文本已达到或超过目标长度时不截断；padding 有效时，null 文本返回 null。

```javascript
return string.alignRight('ab', '0', 4);
// ab00
```

## alignLeft

`String alignLeft(text, padding, length)`：在 text 左侧补字符到总长度 length；padding、超长文本及 null 文本规则与 alignRight 相同。

```javascript
return string.alignLeft('ab', '0', 4);
// 00ab
```

## alignCenter

`String alignCenter(text, padding, length)`：在 text 两侧补字符到总长度 length；需补奇数个字符时右侧多补一个。padding、超长文本及 null 文本规则与 alignRight 相同。

```javascript
return string.alignCenter('ab', '0', 4);
// 0ab0
```

```javascript
return string.alignCenter('ab', '0', 5);
// 0ab00
```

## compareString

`int compareString(left, right)`：按字符顺序比较两个文本；left 小于、等于、大于 right 时分别返回负数、0、正数，返回值不限定为 -1、0、1。null 按空字符串处理。

```javascript
return string.compareString('a', 'b');
// -1
```

## compareStringIgnoreCase

`int compareStringIgnoreCase(left, right)`：忽略大小写比较两个文本；返回值含义及 null 处理与 compareString 相同。

```javascript
return string.compareStringIgnoreCase('A', 'a');
// 0
```

## split

`List split(text[, separators])`：将 text 切成字符串列表；separators 中的每个字符都是分隔符，不是正则表达式或完整分隔串。连续分隔符合并，首尾分隔符不产生空元素；separators 省略或为 null 时按空白切分。null 文本返回 null，空文本返回空列表。

```javascript
return string.split('a,,b;c', ',;');
// ["a", "b", "c"]
```

```javascript
return string.split('  Alice  Bob  ');
// ["Alice", "Bob"]
```

## join

`String join(values[, separator])`：将 values 列表中的元素转换为文本后按 separator 连接。separator 省略或为 null 时使用空字符串；null 列表返回 null，空列表返回空字符串，null 元素按空字符串处理但保留其分隔位置。

```javascript
return string.join(['a', null, 'b'], ',');
// a,,b
```

```javascript
return string.join(['a', 'b']);
// ab
```

## isEmpty

`boolean isEmpty(text)`：判断 text 是否为 null 或空字符串；这两种情况返回 true，纯空白字符串返回 false。

```javascript
return string.isEmpty(' ');
// false
```

## equalsIgnoreCase

`boolean equalsIgnoreCase(left, right)`：忽略大小写判断两个文本是否相等；两个 null 返回 true，仅一个为 null 时返回 false。

```javascript
return string.equalsIgnoreCase('DataQL', 'dataql');
// true
```

CompareUdfSource 已标记弃用，字符串比较直接使用本库的 `compareString` 和 `compareStringIgnoreCase`。
