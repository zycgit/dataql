---
title: "5.1.1 可视化操作"
description: "通过 Dataway UI 查找、编辑和调试 API，管理接口状态与发布历史。"
---

import ConsoleGuideImage from '@site/src/components/ConsoleGuideImage';

Dataway UI 提供接口列表和编辑页面，支持查找接口、编写脚本、配置参数、调试、发布及历史恢复。

## 接口列表

点击顶部 Interface 进入列表页。页面分为 A 接口列表、B 调用参数、C 响应结果三个区域，拖动分隔线可以调整区域大小。

<ConsoleGuideImage page="list" alt="接口列表页：A 指向 API 列表，B 指向调用参数区，C 指向响应结果区" />

### A：API 列表

- 选择接口：点击所在行或左侧选择框，将该接口的参数样例和请求头载入 B 区。
- 搜索：在顶部 search Api 中输入路径或描述的关键词，筛选匹配接口。
- 目录：点击搜索框左侧的网格图标，按接口路径目录筛选。
- 刷新：点击搜索框右侧的刷新图标，重新加载接口列表。
- 编辑：点击行末的编辑图标，进入该接口的编辑页面。

每行显示 HTTP 方法、状态、路径和描述。状态标记的含义见下方[接口状态](#api-status)。

### B：调用参数

- Parameters：填写 JSON 参数，右上角的橡皮图标用于格式化 JSON。
- Headers：添加、修改或删除请求头；勾选的请求头会随调用发送。
- Execute Query：点击右上角绿色播放按钮，调用选中的已发布 API。

列表页的参数修改用于本次调用。需要保存参数样例时，在编辑页面修改并保存。Changes 状态的接口仍执行此前已发布的内容。

### C：响应结果

Result 显示已发布 API 的调用结果，预览、复制、格式化和下载操作见 [D：结果区](#result-panel)。

## 新建与编辑

点击顶部 New 新建接口，或从列表行末进入编辑页。图中 A 为功能条，B 为脚本编辑区，C 为测试参数区，D 为结果区。

<ConsoleGuideImage page="editor" alt="接口编辑页：A 指向功能条，B 指向脚本编辑区，C 指向测试参数区，D 指向执行结果区" />

### A：功能条

左侧设置 HTTP 方法和接口路径。路径以 `/` 开头，例如 `/hello`。首次保存后，方法和路径固定。

点击路径右侧的信息图标，在 Description 多行文本框中填写备注，点击 Confirm 确认或 Cancel 取消。随后选择 DataQL 或 SQL 脚本类型。

右侧按钮从左到右依次为：

- More Settings（省略号）：设置参数包装及包装后的参数名，见 [API 选项](development/options.md#parameter-wrapping)。
- Save（软盘）：保存脚本、描述、参数样例和执行选项。
- Execute Query（绿色播放）：执行当前编辑内容，结果显示在 D 区。
- Smoke Test（烧杯）：测试已保存的草稿，需要先保存当前修改。
- Publish（向上箭头）：冒烟测试通过后发布，后续 API 调用使用本次发布的内容。
- Release History List（时钟）：查看发布历史，将选中记录载入编辑器。
- Disable API / Delete API：已发布接口可以停用，未发布或已停用接口可以删除。

按钮右侧显示接口状态。Unsaved 表示当前编辑内容尚未保存；已有接口还提供 Reload API 刷新按钮，用于载入最新草稿。

### B：脚本编辑区

编写 DataQL 或 SQL，编辑器提供语法着色、查找和替换。点击 Execute Query 调试当前内容，脚本示例见[脚本支持](development/script.md)。

### C：测试参数区

在 Parameters 填写参数样例，在 Headers 配置请求头。操作方式与列表页相同，点击 Save 后随接口一起保存。

### D：结果区 {#result-panel}

Result 展示调试响应，右上角显示 HTTP 状态码和请求耗时。Interface 列表页使用相同的预览方式。

<ConsoleGuideImage page="result-handlers" alt="结果处理器：1 指向 Result Handler 下拉按钮，2 指向展开的处理器列表" />

点击图中 1 处的 Result Handler 下拉按钮，在 2 处的列表中单选输出方式。默认选择 Structure，并可在 Structure 标签页编辑响应模板。选择 Raw Value、CSV、Text、VerifyCode 或其它处理器后，模板编辑关闭，已有模板保留。列表包含应用注册的结果处理器，配置随接口保存，发布后对外生效，见 [结果处理器](result-handlers.md)。

Result 按响应的 Content-Type 自动选择预览方式：

- JSON：`application/json` 及带 `+json` 后缀的类型；数组结果可切换为 Table，支持顶层数组及 `value`、`data`、`result` 中的数组。
- Table：`text/csv`、`application/csv`；支持带引号、逗号和换行的单元格。
- Text：`text/*`、`application/xml`、`application/*+xml`、`application/javascript`、`application/x-www-form-urlencoded`；HTML、XML 和 JavaScript 展示源码。
- Image：`image/*`，包括 SVG；支持浏览器可显示的图片格式。
- File：其它类型或未指定 Content-Type 的响应，显示文件名、类型和大小，提供下载。

View 切换只影响当前展示。表格最多预览 200 行，文本、JSON 和下载保留完整内容。文本按响应字符集解码，默认 UTF-8；JSON 无法解析时展示原文，图片无法显示时提供下载。

- 复制图标：复制当前结果。
- 橡皮图标：格式化 JSON 结果。
- 下载图标或 File 视图中的 Download file：按响应文件名保存原始内容。

## 接口状态 {#api-status}

- Editor：接口草稿尚未发布。
- Published：接口已发布，草稿与当前发布内容一致。
- Changes：接口已发布，草稿有待发布的改动。
- Disable：接口已停用，草稿和发布历史保留。

保存只更新草稿。对外提供服务时，依次执行 Save → Smoke Test → Publish。修改内容或重新载入接口后，需要再次测试。草稿与发布记录的关系见[版本管理](../principles/index.md#lifecycle)。

## 历史、停用与删除

### 恢复历史

<ConsoleGuideImage page="history" alt="恢复历史：1 指向功能条上的时钟按钮，2 指向历史记录右侧的恢复按钮" />

1. 点击图中 1 处的时钟按钮（Release History List），展开 History Version 发布历史。
2. 找到需要恢复的记录，点击图中 2 处的恢复按钮，将该次发布的内容载入编辑器。
3. 检查脚本和参数，依次执行 Save → Smoke Test → Publish，使恢复的内容对外生效。

历史内容与当前草稿不同时，页面显示 Unsaved。重新发布前，已发布 API 继续使用当前生效的内容。

### 停用接口

<ConsoleGuideImage page="disable" alt="停用接口：1 指向 Disable API 按钮，2 指向停用确认框中的确认按钮" />

1. 在 Published 或 Changes 状态的接口中，点击图中 1 处的 Disable API 按钮。
2. 在确认框中点击图中 2 处的 OK，完成停用；点击 Cancel 取消操作。

停用后显示 Disable，接口停止对外调用，草稿和发布历史保留。再次执行 Smoke Test → Publish 可以恢复服务。

### 删除接口

<ConsoleGuideImage page="delete" alt="删除接口：1 指向 Delete API 按钮，2 指向删除确认框中的确认按钮" />

1. 打开已保存且未发布的接口，或先停用已发布接口，点击图中 1 处的垃圾桶按钮（Delete API）。
2. 在确认框中点击图中 2 处的 OK，删除接口；点击 Cancel 保留接口。

删除会清除接口及全部发布历史；离开或重载时会提示未保存的修改，版本冲突时先保留修改，再重载合并。

通过 Java 或管理 HTTP 接口维护 API 的用法见[程序化管理](programmatic.md)。
