---
title: "5.3.5 VerifyCode"
description: "VerifyCode 将脚本返回的字符串绘制为 PNG 验证码图片，使用 Java 自带的图形和图片编码能力。"
---

## 介绍

VerifyCode 将脚本返回的字符串绘制为 PNG 验证码图片，使用 Java 自带的图形和图片编码能力。

## 作用

响应类型为 `image/png`，设置 `Cache-Control: no-store`。图片包含随机颜色、字符旋转和干扰线。输入需为非空字符串，最多 32 个字符，不含控制字符。

应用负责生成、保存和校验验证码文本，处理器负责生成图片。脚本执行失败或文本不符合要求时，返回 Structure 失败结构。

## 用法

示例 `POST /verifyCode` 的参数为 `{"text":"A7K9"}`：

```javascript
return ${text};
```

![VerifyCode 处理器生成的 PNG 图片](/img/dataway/verify-code.png)

## 如何配置

控制台选择 VerifyCode，或设置以下 API 选项，保存并发布：

```json title="接口选项"
{"resultHandler": "verifyCode"}
```
