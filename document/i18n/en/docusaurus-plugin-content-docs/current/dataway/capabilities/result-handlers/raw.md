---
title: "5.3.2 Raw Value"
description: "Raw Value outputs the script value directly for APIs that define their own response data structure."
---

## Introduction

Raw Value outputs the script value directly for APIs that define their own response data structure.

## Purpose

Ordinary values use `application/json; charset=utf-8` and serialize the script value as JSON. For example, the string `hello` produces `"hello"`. Script failures with error data return that data directly; null error data produces a Structure failure response.

Binary content passes through directly. `WebFile` preserves its filename and type, and `ResultInfo` preserves response settings; see [Binary responses](../development/response.md#binary-response).

## Usage

Call `POST /result-raw` with `{"message":"Hello Dataway"}`:

```javascript
return {"message": ${message}};
```

The response body is `{"message":"Hello Dataway"}`.

## Configuration

Select Raw Value in the console or set the API option below, then save and publish:

```json title="API options"
{"resultHandler": "raw"}
```
