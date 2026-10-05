---
id: binary
title: 6.7.4 Streams and Binary Values
---

DataQL represents binary values with `BinaryModel`. Uploaded files and application `BinaryValue` results can bind to binary columns. Query results retain binary semantics and can be passed to a Dataway result handler.

| Handler | JDBC types | JDBC operations |
| --- | --- | --- |
| bytes.BytesTypeHandler | BINARY, VARBINARY, LONGVARBINARY | setBytes / getBytes |
| bytes.BlobAsBytesTypeHandler | BLOB | setBlob / getBlob; free the read Blob |

The package prefix is `net.hasor.dataql.sqlproc.types`. BinaryModel defaults to BLOB; `jdbcType=VARBINARY` selects byte-column handling.

## Store an upload

Create this H2 table:

```sql
CREATE TABLE attachments (id INT PRIMARY KEY, content BLOB);
```

Create a Dataway POST API and submit a multipart file named `file`:

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

The file binds as BinaryModel and the script returns the affected-row count. Authentication follows application configuration. Store filename and Content-Type in separate columns when required; see [Web functions](../../funx/web.md).

## Query and download

Create another DataQL API with Raw Value as its result handler:

```javascript
import 'net.hasor.dataway.function.WebUdfSource' as web;
var find = @@selectSql(id)<%
    SELECT content FROM attachments WHERE id = #{id}
%>;
run web.setHeader('Content-Type', 'application/octet-stream');
run web.setHeader('Content-Disposition', 'attachment; filename=hello.bin');
return find(1);
```

Default single-row/single-column unwrapping returns the binary value itself. The response contains raw bytes with the chosen headers. For multi-column or multi-row results, select the binary member you want to return.

## Application binary values

A UDF can return `new BinaryValue(byte[])` or `new BinaryValue(inputStream)`. A plain Java byte array can become a DataQL list; BinaryValue explicitly preserves binary semantics. See the complete UDF setup under [binary responses](../../../dataway/capabilities/development/response.md#binary-response).

## Resources and nulls

- Binding reads all bytes from `BinaryModel.openStream()` and closes that input stream before JDBC execution.
- Reading copies content into memory before ResultSet and Connection close, frees the Blob, and returns an independent BinaryValue.
- SQL NULL returns null; a zero-byte file returns an empty binary value.
- BLOB operations use memory proportional to content size. Upload spilling to disk does not make JDBC BLOB access an end-to-end streaming operation.

There are no standalone InputStream or Reader handlers. Text LOBs become strings and binary streams enter through BinaryModel. Applications can store large files in a file service and keep only their identifiers in SQL.
