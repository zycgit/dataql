---
id: binary
title: 6.7.4 流与二进制
---

DataQL 用 `BinaryModel` 表示二进制值。上传文件和应用返回的 `BinaryValue` 可以绑定到二进制列；读取的二进制结果仍保持该模型，可以交给 Dataway 的结果处理器输出。

| 处理器 | JDBC 类型 | 读写方式 |
| --- | --- | --- |
| bytes.BytesTypeHandler | BINARY、VARBINARY、LONGVARBINARY | setBytes / getBytes |
| bytes.BlobAsBytesTypeHandler | BLOB | setBlob / getBlob，读取后释放 Blob |

类位于 `net.hasor.dataql.sqlproc.types` 下。默认 `BinaryModel` 使用 BLOB，指定 `jdbcType=VARBINARY` 可选择字节列处理器。

## 写入上传文件

在 H2 建表：

```sql
CREATE TABLE attachments (id INT PRIMARY KEY, content BLOB);
```

在 Dataway 创建 POST API，将 multipart 文件域命名为 `file`：

```javascript
var save = @@insertSql(id, content)<%
    INSERT INTO attachments(id, content)
    VALUES (#{id}, #{content, jdbcType=BLOB})
%>;
return save(1, ${file});
```

```bash
curl -X POST 'http://localhost:8080/api/save-file' \
  -b cookies.txt -F 'file=@hello.txt'
```

文件作为 `BinaryModel` 写入，脚本返回影响行数；接口的身份认证按应用配置执行。文件名、Content-Type 等元信息如需保留，应另设数据库字段，见 [Web 函数](../../funx/web.md)。

## 查询并下载

另建一个 DataQL API，结果处理器选择 Raw Value：

```javascript
import 'net.hasor.dataway.function.WebUdfSource' as web;
var find = @@selectSql(id)<%
    SELECT content FROM attachments WHERE id = #{id}
%>;
run web.setHeader('Content-Type', 'application/octet-stream');
run web.setHeader('Content-Disposition', 'attachment; filename=hello.bin');
return find(1);
```

单行单列默认拆包后，返回值就是二进制内容。响应输出原始字节；文件名和媒体类型由上述 Header 指定。多行或多列查询会返回含二进制成员的列表或对象，应先选取需要输出的那个值。

## 应用提供二进制

UDF 可返回 `new BinaryValue(byte[])` 或 `new BinaryValue(inputStream)`。直接返回 Java 字节数组可能被 DataQL 当作列表转换；使用 `BinaryValue` 可以明确保持二进制语义。完整 UDF 配置见 [结果响应](../../../dataway/capabilities/development/response.md#binary-response)。

## 资源与空值

- 写入前读取 `BinaryModel.openStream()` 的全部字节，并关闭该输入流；JDBC 执行不再依赖原始上传流。
- 读取时，在 ResultSet 和连接关闭前将二进制内容复制到内存；Blob 随后释放，脚本持有独立的 `BinaryValue`。
- SQL NULL 返回 null；零字节文件返回空二进制值。
- BLOB 读写会占用与内容大小相关的内存；上传缓存写入临时文件不等于 JDBC BLOB 全程流式传输。

当前不提供独立 InputStream/Reader 类型处理器。文本大字段读取为字符串，二进制流通过 `BinaryModel` 接入。大型文件可以由应用存储服务管理，数据库保存文件标识。
