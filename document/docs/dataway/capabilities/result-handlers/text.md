---
title: "5.3.4 Text"
description: "Text 将脚本返回值转换为普通文本，适用于文本内容或简单状态输出。"
---

## 介绍

Text 将脚本返回值转换为普通文本，适用于文本内容或简单状态输出。

## 作用

响应类型为 `text/plain; charset=UTF-8`，内容由 `String.valueOf(value)` 转换；对象使用自身的字符串表示，例如 `{name=Ada}`，空值输出 `null`。

脚本执行失败时返回 Structure 失败结构。二进制模型和输入流使用 Raw Value 输出。

## 用法

示例 `POST /result-text` 的参数为 `{"message":"Hello Dataway"}`：

```javascript
return ${message};
```

响应正文为 `Hello Dataway`。

## 如何配置

控制台选择 Text，或设置以下 API 选项，保存并发布：

```json title="接口选项"
{"resultHandler": "text"}
```
