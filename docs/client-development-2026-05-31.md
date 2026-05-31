# Android 客户端开发记录（2026-05-31）

## 1. 开发目标

本次开发围绕“开放式 Tab 容器 + AI oncall 助手”的客户端主链路展开，目标不是简单展示服务端返回的数据，而是把服务端接口能力转化为可交互、可恢复、可演示的客户端体验。

本次优先完成：

1. AI oncall 聊天页面。
2. 工作台 Tab 管理能力。
3. 内置 Tab 启用/停用联调入口。
4. 自定义 Web Tab 表单、预览、校验和接口预留。
5. 注册入口和 `/auth/register` 接口预留。
6. REST/SSE 网络层扩展。
7. 产品主入口收敛为“工作台 / AI助手 / 我的”。
8. App 名称和登录页标题统一为“开放式Tab容器”。
9. 动态业务 Tab 增加返回工作台能力。
10. 工作台顶部操作改为图标按钮，避免“刷新”等文字按钮在窄屏显示不完整。
11. AI 助手输入框适配系统键盘，输入时跟随键盘上移。
12. 增加 B站首页 Web Tab 示例，并使用 WebView 承载真实网页。
13. 登录页、工作台、“我的”页做第一轮商业化视觉升级。
14. “我的”页补齐状态栏安全距离，避免与系统状态栏重叠。

当前云端服务仍有部分接口未实现，因此客户端采用“已实现接口直接接入、未实现接口明确提示、Repository 层预留”的方式，避免后续服务端上线后大面积重写 UI。

---

## 2. 已验证服务端接口边界

本次开发依据以下接口状态进行设计：

### 2.1 当前可用

```text
POST /auth/login
GET  /me
GET  /tabs
GET  /tabs/catalog
POST /me/tabs
DELETE /me/tabs/{tabId}
GET  /oncall/stream?message=...
```

### 2.2 当前未实现或待服务端补齐

```text
POST /auth/register
POST /tabs
PUT /tabs/{tabId}
DELETE /tabs/{tabId}
POST /oncall/sessions
POST /oncall/sessions/{sessionId}/messages
GET /oncall/sessions/{sessionId}/messages
GET /oncall/sessions/{sessionId}/stream?messageId=...
```

因此本次客户端开发中：

- AI oncall 第一版接旧 SSE：`GET /oncall/stream?message=...`。
- Tab 管理接已有的 catalog、enable、disable 接口。
- 自定义 Web Tab 按 `POST /tabs` 规划协议写入 Repository，但当前服务端 404 时展示“接口暂未开放”。
- 注册按 `POST /auth/register` 规划协议写入 Repository，但当前服务端 404 时提示使用演示账号。

---

## 3. 代码改动概览

### 3.1 网络层

文件：

```text
app/src/main/java/github/leavesczy/compose_chat/open/network/OpenApiClient.kt
app/src/main/java/github/leavesczy/compose_chat/open/network/OpenJsonParser.kt
```

新增能力：

```text
putJson()
delete()
sse()
urlEncode()
```

设计说明：

- `sse()` 只负责把服务端 SSE 按行读出来，不在网络层理解业务语义。
- 业务事件解析放在 Repository 层，后续服务端新增 SSE event 时不需要改网络层。
- `parseError()` 增强了纯文本错误处理，例如服务端返回 `404 page not found` 时，不再变成模糊的 JSON 解析失败。

关键边界：

- 网络层仍统一返回 `OpenApiResult` 或 `OpenSseLine`。
- UI 不直接使用 OkHttp。

---

### 3.2 数据模型

文件：

```text
app/src/main/java/github/leavesczy/compose_chat/open/model/OpenApiModels.kt
```

新增模型：

```text
RegisterRequest
SuccessResponse
CreateCustomWebTabRequest
TabMutationResponse
OnCallToolEvent
```

用途：

- `RegisterRequest` 对应规划中的注册接口。
- `CreateCustomWebTabRequest` 对应规划中的 `POST /tabs`。
- `TabMutationResponse` 对应自定义 Tab 创建/修改后的响应。
- `OnCallToolEvent` 对应 SSE 中的 `tool` 事件。

---

### 3.3 Repository 层

文件：

```text
app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenAuthRepository.kt
app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenTabRepository.kt
app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenOnCallRepository.kt
```

新增能力：

```text
OpenAuthRepository.register()
OpenTabRepository.getCatalogTabs()
OpenTabRepository.enableTab()
OpenTabRepository.disableTab()
OpenTabRepository.createCustomWebTab()
OpenOnCallRepository.stream()
```

设计说明：

- `OpenOnCallRepository.stream()` 将 SSE 原始行转换为业务事件：`delta`、`tool`、`done`、`error`。
- 当前服务端只稳定返回 `delta` 和 `done`，但客户端已经预留 `tool` 和 `error` 展示能力。
- 后续如果升级到 session/message 接口，优先改 `OpenOnCallRepository`，UI 层不需要重写。

---

## 4. AI oncall 聊天页

文件：

```text
app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenOnCallPage.kt
app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenTabContentHost.kt
```

入口：

```text
/ai-oncall
```

当前能力：

1. 展示欢迎消息。
2. 展示快捷问题。
3. 用户输入问题并发送。
4. 调用 `GET /oncall/stream?message=...`。
5. 增量追加 AI 回复文本。
6. 展示流式生成状态。
7. 预留工具调用卡片。
8. 展示失败信息和重试入口。

UI 状态：

```text
空状态 / 欢迎态
用户消息
AI 消息
AI 流式生成中
工具调用结果
失败提示
重试
```

实现边界：

- 当前未做复杂 Markdown 渲染。
- 检测到代码块时先提示“后续升级代码块渲染”。
- 第一版不做历史会话列表，因为服务端 session 接口尚未上线。

---

## 5. 工作台与 Tab 管理

文件：

```text
app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenWorkbenchPage.kt
app/src/main/java/github/leavesczy/compose_chat/open/tab/OpenTabModels.kt
app/src/main/java/github/leavesczy/compose_chat/ui/MainPage.kt
app/src/main/java/github/leavesczy/compose_chat/ui/logic/MainViewModel.kt
```

新增能力：

1. 工作台顶部增加“刷新”和“管理”。
2. 增加状态摘要：已启用、可打开、受限。
3. 管理模式下展示“已启用”列表。
4. 管理模式下展示“可添加的内置 Tab”。
5. 支持调用 `DELETE /me/tabs/{tabId}` 停用 Tab。
6. 支持调用 `POST /me/tabs` 启用 Tab。
7. 对 `FORBIDDEN` 显示权限不足提示。
8. 增加自定义 Web Tab 表单和预览。
9. 客户端内置 `B站首页` Web Tab 示例，用于演示网页类业务 Tab 的接入效果。

自定义 Web Tab 表单字段：

```text
Tab 名称
description
entryUri
icon
```

客户端校验：

```text
Tab 名称不能为空
entryUri 必须以 http:// 或 https:// 开头
```

当前接口状态：

- `POST /tabs` 云端当前返回 404。
- 客户端会展示“服务端暂未开放自定义 Tab 创建接口”。
- 服务端实现后，当前 Repository 可直接复用。
- `bilibili-web` 是客户端内置兜底示例。服务端后续如果下发同 id 的 TabManifest，客户端会自动去重并使用服务端配置。

---

## 6. 注册入口

文件：

```text
app/src/main/java/github/leavesczy/compose_chat/ui/login/LoginPage.kt
app/src/main/java/github/leavesczy/compose_chat/ui/login/logic/LoginViewModel.kt
app/src/main/java/github/leavesczy/compose_chat/ui/login/logic/Models.kt
```

新增能力：

1. 登录页增加“注册账号”入口。
2. 注册模式下展示确认密码和昵称输入。
3. 本地校验账号、昵称、密码、确认密码。
4. 调用 `POST /auth/register`。
5. 注册成功后复用登录成功流程。
6. 当前接口 404 时提示“注册接口暂未开放，请先使用演示账号登录”。

实现边界：

- 不伪造注册成功。
- 不写本地假账号。
- 接口上线后只需服务端返回与登录一致的 token/userId/displayName/permissions 即可跑通。

---

## 7. UI 和交互设计原则

本次 UI 设计遵循以下原则：

1. 工作台不是简单列表，而是展示业务状态：可打开、权限不足、版本不兼容、入口不支持。
2. Tab 管理把“内置 Tab 启用”和“自定义 Web Tab 创建”分成两个区域，避免概念混淆。
3. AI oncall 页面保留聊天产品常见体验：消息气泡、快捷问题、流式状态、失败重试。
4. 所有接口失败都要有中文提示，不让用户面对崩溃或空白页。
5. 图标继续通过 `OpenTabRegistry.iconOf()` 集中映射，后续替换图标只改一处。
6. 原 demo 的“消息 / 通讯录”能力继续保留在代码里，但不再作为当前训练营产品的底部主入口展示。
7. 业务页从工作台打开后必须有明确返回路径，避免用户进入动态 Tab 后只能依赖系统返回键。
8. 工作台和 AI 页面自行处理状态栏安全距离，不再依赖原 demo 顶部栏承载页面标题。

---

## 8. 本轮产品化 UI 调整

本轮根据 UI 参考图和产品定位做了主流程收敛，目标是让客户端更像一个真实的“开放式 Tab 容器”产品，而不是原 IM demo 的附加页面。

### 8.1 App 名称与登录页

文件：

```text
client-android/compose_chat-master/app/src/main/res/values/strings.xml
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/ui/login/LoginPage.kt
```

调整：

- `app_name` 改为“开放式Tab容器”。
- 登录页主标题复用 `app_name`，因此启动器名称和登录页标题保持一致。
- 登录页增加副标题“开发者接入与 AI oncall 助理”，说明产品用途。

### 8.2 底部导航收敛

文件：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/ui/MainPageBottomBar.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/ui/logic/MainViewModel.kt
```

调整：

- 底部栏只展示三个主入口：`工作台`、`AI助手`、`我的`。
- 默认进入 `工作台`，不再默认进入原 demo 的消息列表。
- `AI oncall` 的底部标签按产品文案展示为 `AI助手`。
- `MainPageTab.Conversation` 和 `MainPageTab.Friendship` 没有删除，后续如需复用原 demo 聊天能力，仍可从代码层恢复入口。

### 8.3 工作台产品化

文件：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenWorkbenchPage.kt
```

调整：

- 顶部展示“工作台”和“开放容器团队”，强化容器产品语义。
- 增加提示条：“TabDefinition 驱动展示：服务端下发配置，客户端负责权限、版本、入口类型和交互状态。”
- 增加“服务端下发的业务 Tab”分区标题。
- 顶部“刷新 / 管理”改为图标按钮，解决窄屏下文字显示不完整的问题。
- 保留管理模式：已启用列表、可添加内置 Tab、自定义 Web Tab 表单与预览。

### 8.4 动态业务页返回

文件：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/ui/MainPage.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/ui/logic/MainViewModel.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenTabContentHost.kt
```

调整：

- `MainViewModel.backToWorkbench()` 统一处理从动态业务页返回工作台。
- `OpenTabContentHost` 接收可空的 `onBackToWorkbench` 回调。
- 从工作台打开审批、日程、财务、Web Tab、异常状态页时，页面顶部或状态页会展示返回入口。
- 从底部 `AI助手` 进入 AI 页面时不展示返回工作台，因为底部导航本身就是一级返回路径。

### 8.5 AI 助手页

文件：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenOnCallPage.kt
```

调整：

- 页面标题继续使用 `AI oncall`，底部标签使用 `AI助手`。
- 副标题改为“协议问答 · 配置生成 · 错误诊断”，贴合作业中 AI OnCall 助理定位。
- 页面增加状态栏安全距离，去掉原 demo 顶部栏后仍保持正确布局。
- 页面增加 `imePadding()`，输入框会跟随系统键盘上移，避免真实聊天输入时被键盘遮挡。

### 8.6 Web Tab 示例

文件：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenMockData.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenTabRepository.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/tab/OpenTabModels.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenTabContentHost.kt
```

调整：

- 新增客户端内置 `B站首页` Web Tab：
  - `id = bilibili-web`
  - `route = /bilibili`
  - `entryType = web`
  - `entryUri = https://m.bilibili.com`
- `OpenTabRepository.withClientBuiltInTabs()` 会在服务端没有下发该 id 时补齐示例。
- `OpenTabContentHost` 将 Web Tab 从占位页升级为真实 WebView 承载。
- WebView 开启 JavaScript、DOM storage、宽视口和媒体播放基础配置。
- WebView 页面提供加载中和失败提示，避免网页白屏时没有反馈。

说明：

- B站移动端网页可用于演示“网页业务 Tab 接入”，但视频播放能力仍受 B站网页自身策略、登录态、WebView 兼容性和跳转策略影响。
- 如果后续需要稳定视频播放，建议服务端或产品侧提供可控的测试网页，或者客户端增加更完整的 WebView 生命周期、下载、全屏播放、外链拦截和 UA 配置。

### 8.7 登录页、工作台与我的页视觉升级

文件：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/ui/login/LoginPage.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenWorkbenchPage.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenProfilePage.kt
```

调整：

- 登录页改为产品化入口：
  - 顶部品牌图标。
  - 产品名“开放式Tab容器”。
  - 卡片式登录/注册表单。
  - 输入框增加账号、密码、昵称图标。
  - 页面增加滚动和键盘适配，注册模式下小屏不再挤压。
- 工作台第一轮商业化升级：
  - 浅灰页面背景。
  - 白色业务卡片。
  - 业务 Tab 图标容器。
  - 可打开、权限不足、版本不兼容等状态 Badge。
  - 更明显的信息提示卡。
- “我的”页第一轮账户中心升级：
  - 补齐 `statusBarsPadding()`，避免和系统状态栏重叠。
  - 顶部账户卡片改为白色卡片。
  - 团队信息改成 Badge。
  - 统计卡片、权限标签改为更轻量的产品样式。

### 8.8 头像策略与本地替换

文件：

```text
client-android/compose_chat-master/app/src/main/res/drawable/open_default_avatar.xml
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenAvatarRepository.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenProfilePage.kt
```

调整：

- 新增 `open_default_avatar.xml` 作为默认卡通头像资源。
- “我的”页头像区域默认展示卡通头像，避免账户中心显得过于模板化。
- 点击头像可从本机相册选择图片。
- 选择后的图片会复制到 App 私有目录，并通过 `SharedPreferences` 保存本地路径。
- 不直接保存系统相册返回的 `content://` Uri，避免 App 重启后 Uri 授权失效。

当前头像优先级：

```text
用户本地选择头像 > 客户端默认卡通头像
```

后续服务端补充 `/me.avatarUrl` 后，推荐升级为：

```text
服务端 avatarUrl > 用户本地选择头像 > 客户端默认卡通头像
```

这样做的好处是：当前演示阶段无需等待服务端头像接口，后续服务端上线后也只需要调整 `OpenProfilePage` 的头像数据来源，不需要改账户页整体结构。

替换默认头像的位置：

```text
client-android/compose_chat-master/app/src/main/res/drawable/open_default_avatar.xml
```

如果后续使用设计师提供的 PNG/WebP 头像，可放到：

```text
client-android/compose_chat-master/app/src/main/res/drawable-nodpi/
```

然后把 `OpenProfilePage` 中的默认资源从 `R.drawable.open_default_avatar` 换成新的资源名即可。

### 8.9 本轮细节修复

文件：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/ui/MainPage.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenOnCallPage.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenTabRepository.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenWorkbenchPage.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenTabContentHost.kt
```

调整：

- `AI助手` 作为一级聊天页时隐藏底部导航栏，避免底部导航栏高度和 `imePadding()` 叠加后，在输入框和键盘之间形成空白。
- AI 发送按钮图标改为 `Icons.AutoMirrored.Rounded.Send`，清理 Compose 图标废弃警告。
- 工作台 Tab 卡片增加来源标识：
  - `服务端下发`：来自云端 `/tabs`。
  - `客户端内置`：客户端为了演示和兜底补齐的入口，例如 `AI oncall`、`B站首页`。
  - `本地兜底`：服务端不可用时使用的本地 Mock 数据。
- 审批中心不再只展示占位文案，已接入 `GET /business/approval/summary`，展示待处理数量、今日通过数量和审批条目。
- 版本不兼容页面补充容器版本说明，便于解释“新版实验 Tab”为什么不能打开。

---

## 9. 规范性说明

本次开发遵循：

1. 不删除原 compose_chat / 腾讯 IM 逻辑。
2. 新增训练营能力集中在 `open` 包。
3. UI 不直接请求接口，统一通过 Repository。
4. 接口字段遵循 `mock-server-api.md` 和 `待补接口规划.md`。
5. 对当前未上线接口保持清晰兜底提示。
6. 关键集成边界使用中文注释说明，避免后续同学误以为当前接口已全部可用。

---

## 10. 构建验证

使用 Android Studio JBR 21 和本机 Android SDK 进行 Kotlin 编译验证：

```powershell
$env:JAVA_HOME="E:\dev\Android Studio\jbr"
$env:ANDROID_HOME="E:\Android\SDK"
$env:ANDROID_SDK_ROOT="E:\Android\SDK"
$env:Path="$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
.\gradlew.bat :app:compileDebugKotlin
```

验证结果：

```text
BUILD SUCCESSFUL
```

说明：仓库中不提交 `local.properties`，因此命令行构建需要本机环境提供 Android SDK 路径。Android Studio 正常配置 SDK 后不受影响。

本轮 UI 收敛后再次执行：

```powershell
.\gradlew.bat :app:assembleDebug
```

验证结果：

```text
BUILD SUCCESSFUL
```

本轮键盘适配、B站 Web Tab、登录页/工作台/我的页视觉升级后再次执行：

```powershell
.\gradlew.bat :app:assembleDebug
```

验证结果：

```text
BUILD SUCCESSFUL
```

本轮头像策略、Tab 来源标识、审批摘要接入和 AI 图标警告清理后再次执行：

```powershell
.\gradlew.bat :app:assembleDebug
```

验证结果：

```text
BUILD SUCCESSFUL
```

---

## 11. 后续建议

### 11.1 服务端优先补齐

```text
POST /tabs
POST /auth/register
POST /oncall/sessions
POST /oncall/sessions/{sessionId}/messages
GET /oncall/sessions/{sessionId}/stream?messageId=...
GET /me 返回 avatarUrl 字段
```

### 11.2 客户端下一步

1. 服务端 `POST /tabs` 上线后，验证自定义 Web Tab 创建成功并刷新工作台。
2. 服务端 session/message 接口上线后，把 AI oncall 从旧 SSE 切到会话模型。
3. 增强代码块渲染和工具调用事件展示。
4. 为 Web Tab 增加更完整的 WebView 生命周期、外链拦截、全屏播放和返回栈处理。
5. 服务端返回头像字段后，把“我的”页头像优先级升级为 `服务端 avatarUrl > 用户本地选择头像 > 客户端默认卡通头像`。

---

## 12. 下一轮保留优化项（追加）

追加时间：2026-05-31 22:30 左右。

当前模拟器实测又发现两个影响操作流畅度的问题，暂未在本轮直接修改，下一轮应优先处理。

### 12.1 侧边栏个人资料入口

现象：

1. 从左侧抽屉点击“个人资料”后，进入的是旧 demo 的个人资料页面。
2. 当前页面为空白，且缺少清晰返回路径。
3. 这会和我们已经完成的新“我的”页形成体验割裂。

建议方案：

1. 优先把侧边栏“个人资料”入口接到新的 `OpenProfilePage` 或触发底部栏切换到“我的”。
2. 如果短期不想改抽屉逻辑，可以先隐藏“个人资料”入口，避免用户误入空白页。
3. 如果保留旧 demo 个人资料页，必须补返回按钮和有效资料内容，否则不建议在训练营演示中暴露。

涉及文件预计包括：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/ui/MainPageDrawer.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/ui/logic/MainViewModel.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenProfilePage.kt
```

### 12.2 AI oncall 页面返回路径

现象：

1. 为了解决键盘弹起后输入框和键盘之间存在空白的问题，当前 `AI助手` 页面隐藏了底部导航栏。
2. 隐藏底部栏后，AI oncall 页面本身没有返回按钮，用户进入后不方便切回工作台或“我的”。

建议方案：

1. 在 AI oncall 顶部左侧增加返回按钮，点击后回到工作台。
2. 保持底部导航隐藏，继续避免键盘 inset 与底部栏高度叠加的问题。
3. 后续如果想恢复底部导航，需要重新梳理 `Scaffold`、`imePadding()`、`navigationBarsPadding()` 的组合，避免键盘底部空白复现。

涉及文件预计包括：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenOnCallPage.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenTabContentHost.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/ui/MainPage.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/ui/logic/MainViewModel.kt
```

### 12.3 下一轮推荐顺序

1. 先修复侧边栏个人资料空白和不可返回问题。
2. 再给 AI oncall 页面补返回工作台入口。
3. 完成后重新跑 `.\gradlew.bat :app:assembleDebug`。
4. 在模拟器上验证：
   - 侧边栏不会进入空白页。
   - AI oncall 可以返回工作台。
   - 键盘弹起时 AI 输入框仍然紧贴键盘。
   - 底部三入口仍然是 `工作台 / AI助手 / 我的`。
