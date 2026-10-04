---
title: "5.3 结果处理器"
hide_table_of_contents: true
description: "结果处理器将脚本执行结果转换为 HTTP 响应，每个 API 选择一个处理器，默认使用 Structure。"
---

结果处理器将脚本执行结果转换为 HTTP 响应，每个 API 选择一个处理器，默认使用 Structure。

## 使用指引

- [Structure](result-handlers/structure.md)：按模板包装结果，默认处理器。
- [Raw Value](result-handlers/raw.md)：直接输出脚本返回值。
- [CSV](result-handlers/csv.md)：将对象列表导出为 CSV 文件。
- [Text](result-handlers/text.md)：将返回值转换为普通文本。
- [VerifyCode](result-handlers/verify-code.md)：将字符串绘制为 PNG 验证码图片。
- [自定义结果处理器](result-handlers/custom.md)：通过 ResultHandler 扩展输出格式、状态和响应头。

## 选择处理器 {#selection}

控制台选择 Result Handler，或设置 API 的 `resultHandler` 选项，保存并发布即可生效。省略时使用应用默认值，未注册的名称会报错。操作见[可视化操作](management.md#result-panel)，各类型示例见[样例工程](https://gitee.com/zycgit/dataql/tree/dev/example)。

## 配置优先级

内置默认值 → 构造默认配置 → API 选项，后者优先。API 选项在 More Settings → API Options 中编辑；Structure 标签页留空时沿用默认模板。

内置处理器均支持 `Map<String, ?> defaults` 构造参数。`prepareOptions` 合并并校验配置，执行时通过 `ResultContext.getOptions()` 读取，见[自定义结果处理器](result-handlers/custom.md)。
