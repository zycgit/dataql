---
title: "5.2.3 结果响应"
description: "介绍 JSON、失败、二进制及自定义 HTTP 响应。"
---

Dataway 支持 JSON、文本和二进制响应。接口可选择[结果处理器](../result-handlers.md)控制输出格式，脚本通过 Web 函数设置 Header、Cookie 或返回二进制内容。

## JSON 响应

默认响应将脚本结果放入 `value`，同时返回执行状态和耗时。例如脚本 `return {"message": ${message}};`：

```json title="默认响应"
{
  "success": true,
  "message": "OK",
  "code": 0,
  "lifeCycleTime": 2,
  "executionTime": 1,
  "value": {"message": "Hello Dataway"}
}
```

耗时单位为毫秒，空值字段在 JSON 中省略。调用方按接口约定读取响应字段；默认模板通过 `success` 判断执行状态，通过 `value` 获取业务数据。

## 失败响应

脚本可以主动返回业务错误：

```javascript title="业务校验"
if (${id} <= 0) {
    throw 422, "id must be positive";
}
return ${id};
```

默认结构中 `success` 为 false、`code` 为 422、`value` 为错误内容，`location` 表示错误位置。脚本执行失败仍使用响应模板，默认 HTTP 状态为 200。调用方同时检查 HTTP 状态和模板中的成功标识。

选择 Raw Value 时，有错误数据就直接返回该数据；错误数据为空时仍返回失败结构。鉴权、路由、正文解析及编译阶段抛出的异常交给宿主处理。

## Header 与 Cookie {#response-headers}

脚本直接调用 Web 函数设置响应头和 Cookie：

```javascript title="设置响应 Header 和 Cookie"
import 'net.hasor.dataway.function.WebUdfSource' as web;
run web.setHeader('X-Result', 'ready');
run web.setCookie('theme', 'dark', {'path': '/', 'sameSite': 'Lax'});
return {'message': 'ready'};
```

这些设置随本次响应发送，正文仍按 JSON 规则输出。函数说明见 [Web 函数库](../../../dataql/funx/web.md)。

## 二进制响应 {#binary-response}

DataQL 使用 `BinaryModel` 保留二进制对象，支持变量、参数、集合、UDF 和 Lambda 之间的传递。Structure 和 Raw Value 均直接输出二进制内容。

### 手动生成

`convert.textToByte(text)` 将文本编码为 UTF-8 二进制值。Header 和 Cookie 仍通过 Web 函数设置：

```javascript
import 'net.hasor.dataway.function.WebUdfSource' as web;
import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;
run web.setHeader('Content-Type', 'text/plain; charset=UTF-8');
run web.setHeader('Content-Disposition', 'attachment; filename=hello.txt');
return convert.textToByte('Hello Dataway');
```

### 返回上传文件

上传对象 `WebFile` 可以直接返回，通过响应头设置类型和下载文件名：

```javascript
import 'net.hasor.dataway.function.WebUdfSource' as web;
run web.setHeader('Content-Type', 'application/octet-stream');
run web.setHeader('Content-Disposition', 'attachment; filename="download.bin"');
return ${file};
```

以 multipart/form-data 提交名为 `file` 的文件。`Content-Type` 指定响应类型，`attachment` 表示下载，`filename` 指定文件名；省略这两行设置时沿用上传文件的类型和名称，文件内容保持不变。示例工程已提供 `POST /upload-download`，上传缓存在响应完成后清理。

### 应用 UDF

应用 UDF 可以生成文件内容，并通过 `BinaryValue` 将字节作为二进制结果返回。下面以 Spring 配置为例，注册一个名为 `report` 的函数，生成 UTF-8 编码的人员报表。已有 `DatawayConfig` Bean 时，将函数注册代码加入其创建方法。

```java title="注册报表函数"
import java.nio.charset.StandardCharsets;
import net.hasor.dataql.domain.BinaryValue;
import net.hasor.dataway.authorization.IdentityProvider;
import net.hasor.dataway.service.DatawayConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DatawayConfiguration {
    @Bean
    public DatawayConfig datawayConfig(IdentityProvider identityProvider) {
        DatawayConfig config = new DatawayConfig();
        config.identityProvider(identityProvider);

        // report 是 DataQL 脚本中使用的函数名，每次调用时生成文件内容。
        config.function("report", (hints, params) -> {
            String content = "id,name\n1,Alice\n2,Bob\n";
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            return new BinaryValue(bytes);
        });
        return config;
    }
}
```

`IdentityProvider` 使用应用已注册的身份提供者，配置见[Spring 整合](../../integration/spring.md)。函数回调中的 `hints` 为执行选项，`params` 为函数调用参数，本例无需读取它们。实际报表内容可以来自应用服务，返回前用 `BinaryValue` 包装字节。

在控制台创建 DataQL 接口，结果处理器选择 Structure 或 Raw Value，填写以下脚本。`report` 已注册为可直接调用的函数，Web 函数用于设置下载类型和文件名：

```javascript title="生成并下载报表"
import 'net.hasor.dataway.function.WebUdfSource' as web;

var reportData = report();
run web.setHeader('Content-Type', 'text/csv; charset=UTF-8');
run web.setHeader('Content-Disposition', 'attachment; filename="people.csv"');
return reportData;
```

保存并发布后，调用接口将收到 CSV 文件内容，下载文件名为 `people.csv`。Structure 和 Raw Value 均直接输出这些二进制字节，响应正文为：

```text
id,name
1,Alice
2,Bob
```

`new BinaryValue(inputStream)` 接收一次性流，默认 Content-Type 为 application/octet-stream。Dataway 在输出完成、写入失败或 HEAD 请求结束时关闭响应流；应用负责关闭未作为响应返回的流。需要延迟打开资源时，可继承 BinaryModel 并实现 `openStream()`，已知长度由 `getSize()` 返回。

普通 `byte[]` 在 DataQL 中仍转换为列表，二进制内容通过 BinaryModel 显式传递。JSON 对象中的二进制值应由应用处理器转换或显式编码。
