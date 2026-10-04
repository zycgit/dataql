---
title: "5.3.3 CSV"
description: "CSV converts a list of objects into a CSV file for query result exports."
---

## Introduction

CSV converts a list of objects into a CSV file for query result exports.

## Purpose

Responses use `text/csv; charset=UTF-8` and the default download filename `results.csv`. Columns retain encounter order. Cells accept strings, numbers, booleans and null; null becomes an empty cell. Commas, quotes and line breaks are escaped.

Scripts must return a list of objects. Nested objects and lists cannot be cells. Script failures and invalid results produce a Structure failure response.

## Usage

Call `POST /people-csv` with `{}` to query people:

```javascript
hint FRAGMENT_SQL_DATA_SOURCE = "ds1"
hint FRAGMENT_SQL_OPEN_PACKAGE = "off"
var people = @@selectSql()<%
    SELECT id AS "id", name AS "name", balance AS "balance"
    FROM example_people ORDER BY id
%>;
return people();
```

```text title="Response body"
id,name,balance
1,Alice,100
2,Bob,200
```

## Configuration

Select CSV in the console or set the API option below, then save and publish:

```json title="API options"
{"resultHandler": "csv"}
```
