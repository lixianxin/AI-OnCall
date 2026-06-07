# Android 客户端交接文档

更新时间：2026-05-30  
当前项目路径：`E:\Learning\Feishu\code\compose_chat-master`  
建议迁移后的总仓库路径：`Feishu-Training-Camp/client-android/compose_chat-master`

## 1. 项目背景

这是字节训练营大作业的 Android 客户端部分，基于 `compose_chat` demo 改造。

原 demo 技术栈：

- Kotlin
- Jetpack Compose
- MVVM
- 腾讯 IM SDK 作为原聊天后端

本次训练营目标不是删除原聊天 demo，而是在其基础上新增一套客户端能力：

- 训练营服务端登录态
- 动态 Tab 容器
- `TabManifest` / `TabDefinition` 映射
- native / web / external Tab 承载
- 权限不足、版本不兼容、route 不支持等异常态
- AI OnCall 页面
- 后续 REST / SSE 对接

验收时间：2026-06-04 周四。

## 2. 团队分工与仓库约定

已经和队友沟通过，建议使用一个总仓库：

```text
Feishu-Training-Camp/
  client-android/
    compose_chat-master/
  server/
  ai-service/
  protocol/
  docs/
```

目录职责：

- `client-android/compose_chat-master/`：Android 客户端，本项目放这里。
- `server/`：Go + Gin Mock Server，后续接 GORM + PostgreSQL。
- `ai-service/`：AI / Agent / SSE streaming 相关服务。
- `protocol/`：业务 Tab 接入协议。
- `docs/`：接口文档、运行说明、演示材料。

客户端最好只请求统一服务端地址：

```text
Android -> server -> ai-service
```

也就是 Android 只需要一个 `baseUrl`，SSE 也通过 server 暴露：

```http
GET /oncall/stream
```

不要让 Android 同时直连 server 和 ai-service 两套地址，联调会复杂很多。

## 3. 当前重要参考文档

本地参考文档：

```text
E:\Learning\Feishu\参考文档\客户端服务端接口文档.md
E:\Learning\Feishu\参考文档\业务 Tab 接入协议 v1.0-draft.pdf
```

项目内文档：

```text
docs/client-work-plan.md
docs/client-phase-one.md
docs/client-handoff-new-host.md
```

`客户端服务端接口文档.md` 是当前客户端开发的主要依据。协议端 PDF 与服务端接口文档大方向一致，但目前先不阻塞客户端开发。

## 4. 已经完成的客户端代码

### 4.1 新增 open 包

新增代码全部放在：

```text
app/src/main/java/github/leavesczy/compose_chat/open/
```

当前结构：

```text
open/
  config/
    OpenApiConfig.kt
  model/
    OpenApiModels.kt
    TabManifest.kt
  network/
    OpenApiClient.kt
    OpenApiResult.kt
    OpenJsonParser.kt
  repository/
    OpenApiResultExt.kt
    OpenAuthRepository.kt
    OpenBusinessRepository.kt
    OpenMockData.kt
    OpenTabRepository.kt
  session/
    OpenSessionManager.kt
  tab/
    OpenTabModels.kt
  ui/
    OpenTabContentHost.kt
```

设计原则：

- 不删除原 demo 逻辑。
- 不替换腾讯 IM 的 provider。
- 训练营客户端逻辑全部放到 `open` 包。
- 原登录流程暂时保留，训练营服务端登录态后续再接入。

### 4.2 配置

文件：

```text
app/src/main/java/github/leavesczy/compose_chat/open/config/OpenApiConfig.kt
```

当前配置：

```kotlin
object OpenApiConfig {
    const val CONTAINER_VERSION = 1
    const val DEFAULT_BASE_URL = "http://10.0.2.2:8080"
}
```

说明：

- `10.0.2.2` 是 Android 模拟器访问宿主机 localhost 的地址。
- 如果用真机，需要换成局域网 IP，例如 `http://192.168.x.x:8080`。

### 4.3 服务端模型

文件：

```text
open/model/OpenApiModels.kt
open/model/TabManifest.kt
```

已定义：

- `ApiErrorResponse`
- `HealthResponse`
- `LoginRequest`
- `LoginResponse`
- `MeResponse`
- `TeamDto`
- `DebugStatusResponse`
- `ApprovalSummaryResponse`
- `ApprovalItemDto`
- `TabManifest`
- `SemanticVersionDto`
- `EntryType`
- `TabExtensionDto`
- `TitleBarExtensionDto`
- `MenuItemDto`
- `FabExtensionDto`

`TabManifest` 字段按服务端接口文档 v1.1-draft 设计：

```text
id
displayName
description
icon
route
entryType
entryUri
version
minContainerVersion
permissions
enabled
sortOrder
extension
extraConfig
```

### 4.4 Session 管理

文件：

```text
open/session/OpenSessionManager.kt
```

职责：

- 保存训练营服务端 token。
- 保存 `userId` / `displayName`。
- 保存业务权限 `permissions`。
- 判断是否已登录。
- 清空训练营服务端登录态。

注意：

- 这是训练营服务端登录态。
- 原 demo 的腾讯 IM 登录态仍由原来的 `AccountProvider` 处理。
- 两套登录态当前是分离的。

已在 `ChatApplication.kt` 初始化：

```kotlin
OpenSessionManager.init(application = this)
```

### 4.5 网络层

文件：

```text
open/network/OpenApiClient.kt
open/network/OpenApiResult.kt
open/network/OpenJsonParser.kt
```

当前能力：

- 基于 OkHttp。
- 支持 `GET`。
- 支持 `POST application/json`。
- 支持 Bearer Token。
- 支持统一错误结构解析。
- 支持 JSON 解析为 DTO。

已预留接口：

```http
GET /health
POST /auth/login
GET /me
GET /tabs
GET /debug/status
GET /business/approval/summary
```

SSE 后续再接：

```http
GET /oncall/stream?message=xxx&sessionId=xxx
Accept: text/event-stream
```

### 4.6 Repository 层

文件：

```text
open/repository/OpenAuthRepository.kt
open/repository/OpenTabRepository.kt
open/repository/OpenBusinessRepository.kt
open/repository/OpenMockData.kt
```

职责：

- `OpenAuthRepository`：健康检查、登录、获取当前用户。
- `OpenTabRepository`：获取远程 Tabs、构造本地 Mock Tabs、判断 Tab 打开状态。
- `OpenBusinessRepository`：审批摘要接口。
- `OpenMockData`：第一阶段本地 Mock Tab 数据。

### 4.7 Tab 打开状态

文件：

```text
open/tab/OpenTabModels.kt
```

定义：

```kotlin
data class OpenTabItem(
    val manifest: TabManifest,
    val openState: OpenTabState,
    val icon: ImageVector
)
```

状态：

```text
Openable
Disabled
PermissionDenied
VersionIncompatible
RouteUnsupported
EntryUnsupported
InvalidConfig
```

当前判断逻辑在 `OpenTabRepository.resolveState()`：

1. 必填字段检查。
2. `enabled` 检查。
3. `minContainerVersion` 检查。
4. 权限检查。
5. `entryType` 支持检查。
6. `route` 支持检查。

### 4.8 Mock Tabs

文件：

```text
open/repository/OpenMockData.kt
```

当前本地 Mock Tabs：

| id | route | entryType | 说明 |
|---|---|---|---|
| approval | `/approval` | native | 审批中心占位 |
| calendar | `/calendar` | native | 日程占位 |
| finance | `/finance` | native | 权限不足演示 |
| docs | `/docs` | web | Web Tab 占位 |
| ai-oncall | `/ai-oncall` | native | AI OnCall 页面壳 |
| legacy-hybrid | `/legacy-hybrid` | hybrid | 不支持入口类型演示 |
| future-tab | `/future` | native | 容器版本不兼容演示 |

默认权限：

```text
tab.approval.read
tab.calendar.read
ai.oncall
```

所以当前预期：

- Approval 可打开。
- Calendar 可打开。
- AI OnCall 可打开。
- Docs 可打开 Web 占位。
- Finance 显示权限不足。
- Hybrid 显示入口类型不支持。
- Future 显示容器版本不兼容。

### 4.9 UI 接入

修改了：

```text
app/src/main/java/github/leavesczy/compose_chat/ui/logic/Models.kt
app/src/main/java/github/leavesczy/compose_chat/ui/logic/MainViewModel.kt
app/src/main/java/github/leavesczy/compose_chat/ui/MainPage.kt
app/src/main/java/github/leavesczy/compose_chat/ui/MainPageBottomBar.kt
```

新增了：

```text
open/ui/OpenTabContentHost.kt
```

当前 UI 逻辑：

- 原三 Tab 保留：Conversation / Friendship / Person。
- `MainViewModel` 初始化时加载本地 Mock open tabs。
- `MainPageBottomBar` 同时渲染原三 Tab 和动态业务 Tab。
- 底部栏支持横向滚动。
- 选中动态 Tab 时，`MainPage` 优先渲染 `OpenTabContentHost`。
- 未选动态 Tab 时，继续走原 demo 页面。

`OpenTabContentHost` 当前页面：

- Approval 占位页
- Calendar 占位页
- Finance 占位页或权限不足态
- Web Tab 占位页
- External 占位页
- AI OnCall 占位页
- unsupported route / entryType / permission / version 状态页

## 5. 当前代码注意事项

### 5.1 目前未完成真实服务端联调

第一阶段当前使用本地 Mock Tabs，不依赖服务端可用。

下一阶段再把：

```kotlin
openTabRepository.getMockTabs()
```

替换或升级为：

```kotlin
openTabRepository.getRemoteTabs()
```

并处理 loading / error / fallback。

### 5.2 文案暂时使用英文 ASCII

新增 open 包中部分 UI 文案使用英文，是为了避免 Windows PowerShell / 文件编码导致中文字符串损坏。

后续如果要中文展示，建议确认所有源码文件以 UTF-8 保存，并由 Android Studio 直接编辑，不要用错误编码的 PowerShell 输出重写文件。

### 5.3 误建目录已清理

开发过程中曾误建：

```text
E:\Learning\Feishu\app
```

这个不是项目目录，已经删除。

正确项目目录始终是：

```text
E:\Learning\Feishu\code\compose_chat-master
```

迁移和上传 Git 时不要上传 `E:\Learning\Feishu\app`。

## 6. 当前构建问题

在原主机上尝试执行：

```powershell
.\gradlew.bat :app:assembleDebug
```

失败原因不是 Kotlin 编译错误，而是 Gradle 自动下载 JDK 21 toolchain 失败：

```text
Unable to download toolchain matching the requirements languageVersion=21
Could not HEAD https://api.foojay.io/...
```

本机默认 `java -version` 是 JDK 11：

```text
java version "11.0.18"
```

项目要求 JVM 21，相关配置：

```text
gradle/gradle-daemon-jvm.properties
build-logic/convention/src/main/kotlin/github/leavesczy/compose_chat/Project.kt
```

新主机接手时建议：

1. 安装 Android Studio 最新稳定版。
2. 确认 Android Studio 使用 JDK 21。
3. 如果 Gradle 自动下载 toolchain 失败，手动安装 JDK 21。
4. 在 Android Studio 的 Gradle JDK 设置中选择 JDK 21。

Android Studio 路径：

```text
File -> Settings -> Build, Execution, Deployment -> Build Tools -> Gradle -> Gradle JDK
```

选择：

```text
JDK 21
```

如果没有 JDK 21，安装 Eclipse Temurin 21 / JetBrains Runtime 21 / Oracle JDK 21 均可。

## 7. 模拟器验证方式

当前没有真机，可以用 Android Studio 模拟器验证。

### 7.1 创建模拟器

在 Android Studio：

```text
Device Manager -> Create Virtual Device
```

建议设备：

```text
Pixel 7
Pixel 8
Pixel 6
```

建议系统镜像：

```text
API 35 或 API 36
x86_64
Google APIs
```

如果电脑性能一般：

```text
Pixel 4 / Pixel 5
API 34 或 35
```

创建后点击启动模拟器。

### 7.2 运行 app

顶部设备选择从 `No Devices` 切到创建好的模拟器。

Run Configuration 保持：

```text
app
```

点击绿色运行按钮。

### 7.3 验证页面

原 demo 登录流程仍然使用原逻辑。

登录成功进入主页面后，底部栏应出现：

```text
原 demo 三个 Tab + 动态业务 Tab 图标
```

底部栏可以横向滚动。

点击动态 Tab 预期：

- Approval：显示审批占位页。
- Calendar：显示日程占位页。
- Finance：显示权限不足。
- Docs：显示 Web Tab 占位。
- AI OnCall：显示 AI OnCall 占位。
- Hybrid：显示 unsupported entry type。
- Future：显示 container version too low。

### 7.4 模拟器访问本机服务端

如果后续服务端在电脑本机启动：

```text
http://localhost:8080
```

Android 模拟器里不能直接用 `localhost` 访问电脑本机，需要使用：

```text
http://10.0.2.2:8080
```

当前 `OpenApiConfig.DEFAULT_BASE_URL` 已经是：

```text
http://10.0.2.2:8080
```

## 8. 第一阶段验收标准

新主机上需要先验证：

1. 项目能 Gradle Sync。
2. 项目能 `assembleDebug`。
3. app 能在模拟器运行。
4. 原 demo 登录和主页面不被破坏。
5. 底部栏能看到动态业务 Tab。
6. 点击动态 Tab 能切换到占位页或异常态。
7. Finance 权限不足态能展示。
8. Future 版本不兼容态能展示。
9. Docs Web 占位能展示。
10. AI OnCall 页面壳能展示。

## 9. 下一阶段开发任务

### 9.1 接入训练营登录

服务端接口：

```http
POST /auth/login
```

请求：

```json
{
  "account": "demo",
  "password": "demo123"
}
```

响应：

```json
{
  "token": "mock-access-token",
  "userId": "user-demo",
  "displayName": "演示账号",
  "permissions": [
    "tab.approval.read",
    "tab.calendar.read",
    "ai.oncall"
  ]
}
```

客户端处理：

- 保存 token。
- 保存用户信息。
- 保存权限。
- 后续请求带 Bearer Token。

需要决定：

- 是在原 Login 页同时调用训练营登录。
- 还是新增一个训练营登录入口。
- 第一版建议先在原登录成功后，后台尝试训练营 mock 登录，避免破坏原腾讯 IM 登录。

### 9.2 接入 `GET /tabs`

当前 MainViewModel 使用：

```kotlin
openTabRepository.getMockTabs()
```

下一步：

- 登录成功后调用 `GET /tabs`。
- 用返回的 `TabManifest` 构造 `OpenTabItem`。
- 失败时 fallback 到本地 Mock 或显示错误态。

### 9.3 接入审批摘要

接口：

```http
GET /business/approval/summary
```

替换 Approval 占位页里的静态数据。

### 9.4 接入 AI OnCall SSE

接口：

```http
GET /oncall/stream?message=如何接入一个业务Tab
Accept: text/event-stream
Authorization: Bearer <token>
```

事件：

```text
event: delta
data: {"text":"..."}

event: tool
data: {"name":"validate_tab_config","status":"completed","summary":"..."}

event: done
data: {}

event: error
data: {"code":"AI_UNAVAILABLE","message":"AI 服务暂不可用"}
```

客户端页面需要：

- 输入框
- 发送按钮
- 消息列表
- delta 增量追加
- tool 事件卡片
- done 结束状态
- error 错误态
- retry

### 9.5 配置 baseUrl 切换

当前 baseUrl 写死：

```kotlin
http://10.0.2.2:8080
```

后续建议支持：

- 模拟器：`10.0.2.2`
- 真机：局域网 IP
- 演示服务器：固定地址

可以先做一个 debug 配置页面，或用 BuildConfig / local.properties。

## 10. 上传 Git 前建议

如果迁移到总仓库，建议放到：

```text
Feishu-Training-Camp/client-android/compose_chat-master
```

不要只上传 `app/`，要上传完整 Android 工程：

```text
compose_chat-master/
  app/
  base/
  proxy/
  build-logic/
  gradle/
  gradlew
  gradlew.bat
  settings.gradle.kts
  build.gradle.kts
  gradle.properties
  docs/
```

不要上传：

```text
.gradle/
.idea/
build/
app/build/
base/build/
proxy/build/
local.properties
```

当前 `.gitignore` 需要新主机检查一下是否覆盖这些构建产物。

建议提交顺序：

```text
chore: add android client baseline
feat: add open tab client phase one skeleton
docs: add client handoff and phase one notes
```

## 11. 新主机接手步骤

1. 安装 Android Studio。
2. 安装或配置 JDK 21。
3. Clone 总仓库。
4. 打开：

```text
client-android/compose_chat-master
```

5. 等待 Gradle Sync。
6. 创建 Android 模拟器。
7. 运行 `app`。
8. 验证原 demo 登录。
9. 验证动态 Tab。
10. 再开始接服务端接口。

## 12. 关键文件清单

新增文件：

```text
app/src/main/java/github/leavesczy/compose_chat/open/config/OpenApiConfig.kt
app/src/main/java/github/leavesczy/compose_chat/open/model/OpenApiModels.kt
app/src/main/java/github/leavesczy/compose_chat/open/model/TabManifest.kt
app/src/main/java/github/leavesczy/compose_chat/open/network/OpenApiClient.kt
app/src/main/java/github/leavesczy/compose_chat/open/network/OpenApiResult.kt
app/src/main/java/github/leavesczy/compose_chat/open/network/OpenJsonParser.kt
app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenApiResultExt.kt
app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenAuthRepository.kt
app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenBusinessRepository.kt
app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenMockData.kt
app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenTabRepository.kt
app/src/main/java/github/leavesczy/compose_chat/open/session/OpenSessionManager.kt
app/src/main/java/github/leavesczy/compose_chat/open/tab/OpenTabModels.kt
app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenTabContentHost.kt
docs/client-phase-one.md
docs/client-handoff-new-host.md
```

修改文件：

```text
app/src/main/java/github/leavesczy/compose_chat/ChatApplication.kt
app/src/main/java/github/leavesczy/compose_chat/ui/MainPage.kt
app/src/main/java/github/leavesczy/compose_chat/ui/MainPageBottomBar.kt
app/src/main/java/github/leavesczy/compose_chat/ui/logic/MainViewModel.kt
app/src/main/java/github/leavesczy/compose_chat/ui/logic/Models.kt
```

## 13. 接口协议快照

第一阶段遵循服务端接口文档 v1.1-draft。

必须稳定：

```http
GET /health
POST /auth/login
GET /me
GET /tabs
GET /debug/status
GET /business/approval/summary
GET /oncall/stream
```

可以暂缓：

```http
GET /tabs/catalog
POST /me/tabs
DELETE /me/tabs/{tabId}
POST /tabs/validate
POST /tabs/{tabId}/actions/{actionId}
```

通用鉴权：

```http
Authorization: Bearer <accessToken>
```

模拟器 baseUrl：

```text
http://10.0.2.2:8080
```

真机 baseUrl：

```text
http://<LAN_IP>:8080
```

## 14. 当前最重要的判断

新主机接手后，第一优先级不是继续堆功能，而是：

1. 先保证项目能在 JDK 21 下构建。
2. 先保证模拟器能运行。
3. 先验证动态 Tab 第一阶段页面能展示。
4. 再开始接服务端 `POST /auth/login` 和 `GET /tabs`。
5. 最后接 AI OnCall SSE。

如果构建失败，先区分：

- 是 JDK / Gradle / SDK 环境问题。
- 还是 Kotlin 代码问题。

原主机失败点目前明确是 JDK 21 toolchain 下载问题，不是 Kotlin 代码编译错误。

