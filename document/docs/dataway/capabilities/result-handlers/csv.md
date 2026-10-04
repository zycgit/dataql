---
title: "5.3.3 CSV"
description: "CSV 将脚本返回的对象列表转换为 CSV 文件，适用于查询结果导出。"
---

## 介绍

CSV 将脚本返回的对象列表转换为 CSV 文件，适用于查询结果导出。

## 作用

响应类型为 `text/csv; charset=UTF-8`，默认下载文件名为 `results.csv`。字段按出现顺序生成表头，单元格支持字符串、数字、布尔值和空值；空值输出空单元格，逗号、引号和换行按 CSV 规则转义。

脚本需返回对象列表，嵌套对象或列表不能作为单元格。脚本执行失败或结果格式不符合要求时，返回 Structure 失败结构。

## 用法

示例 `POST /people-csv` 查询人员列表，请求参数为 `{}`：

```javascript
hint FRAGMENT_SQL_DATA_SOURCE = "ds1"
hint FRAGMENT_SQL_OPEN_PACKAGE = "off"
var people = @@selectSql()<%
    SELECT id AS "id", name AS "name", balance AS "balance"
    FROM example_people ORDER BY id
%>;
return people();
```

```text title="响应正文"
id,name,balance
1,Alice,100
2,Bob,200
```

## 如何配置

控制台选择 CSV，或设置以下 API 选项，保存并发布：

```json title="接口选项"
{"resultHandler": "csv"}
```
