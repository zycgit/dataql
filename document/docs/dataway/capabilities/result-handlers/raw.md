---
title: "5.3.2 Raw Value"
description: "Raw Value 直接输出脚本返回值，适用于自行约定响应数据结构的 API。"
---

## 介绍

Raw Value 直接输出脚本返回值，适用于自行约定响应数据结构的 API。

## 作用

普通结果使用 `application/json; charset=utf-8`，正文为脚本返回值的 JSON。例如字符串 `hello` 输出为 `"hello"`。脚本失败且有错误数据时直接输出错误数据；错误数据为空时返回 Structure 失败结构。

二进制直接输出，`WebFile` 保留文件名和类型，`ResultInfo` 保留响应设置，见[二进制响应](../development/response.md#binary-response)。

## 用法

示例 `POST /result-raw` 的参数为 `{"message":"Hello Dataway"}`：

```javascript
return {"message": ${message}};
```

响应正文为 `{"message":"Hello Dataway"}`。

## 如何配置

控制台选择 Raw Value，或设置以下 API 选项，保存并发布：

```json title="接口选项"
{"resultHandler": "raw"}
```
