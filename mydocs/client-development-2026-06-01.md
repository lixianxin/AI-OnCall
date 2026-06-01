# Android 客户端开发记录（2026-06-01）

## 1. 本轮目标

本轮基于 2026-06-01 服务端接口实测结果，把客户端从“接口预留/演示态”推进到“真实接口闭环态”。

开发目录：

```text
E:\workspace\Feishu\AI-OnCall\client-android\compose_chat-master
```

统一仓库目录：

```text
E:\workspace\Feishu\AI-OnCall
```

约束：

1. 不使用历史目录 `E:\workspace\Feishu\code\compose_chat-master`。
2. 不删除原 compose_chat / 腾讯 IM 逻辑。
3. 新增训练营能力继续集中在 `open` 包。
4. 当前底部主入口保持为：

```text
工作台 / AI助手 / 我的
```

服务端接口测试文档：

```text
mydocs/server-api-test-report-2026-06-01.md
```

本轮 10 个优化目标：

1. 修复侧边栏“个人资料”入口。
2. 给 AI oncall 补返回工作台入口。
3. 注册接口改为真实可用状态。
4. 自定义 Web Tab 完成创建/编辑/删除闭环。
5. AI oncall 升级到 session/message 模型。
6. 审批和日程页从摘要升级为列表/详情。
7. WebView 增强返回栈、刷新、外链拦截。
8. 增加调试/协议信息页。
9. 更新文档与客户端状态。
10. UI 细节打磨，避免空白页和不可返回页面。

头像说明：

```text
头像继续由客户端实现，不依赖 /me.avatarUrl。
当前策略保持：用户本地选择头像 > 客户端默认头像。
```

## 2. 本轮代码改动概览

涉及文件：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/model/OpenApiModels.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/network/OpenJsonParser.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenBusinessRepository.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenOnCallRepository.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenTabRepository.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenOnCallPage.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenTabContentHost.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenWorkbenchPage.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/ui/MainPage.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/ui/logic/MainViewModel.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/ui/login/logic/LoginViewModel.kt
```

未改动：

```text
原 compose_chat 聊天页
腾讯 IM provider/proxy 逻辑
旧消息/通讯录能力
头像本地选择和默认头像策略
```

## 3. 侧边栏“个人资料”修复

文件：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/ui/logic/MainViewModel.kt
```

修改前：

```text
侧边栏点击“个人资料”会启动旧 demo 的 ProfileUpdateActivity。
实测会进入旧页面，和新的“我的”页割裂，并且返回路径不清晰。
```

修改后：

```text
侧边栏点击“个人资料”时：
1. 关闭抽屉。
2. 切换到底部主入口“我的”。
3. 不再进入旧 demo 空白页。
```

实现方式：

```kotlin
bottomBarViewState = bottomBarViewState.copy(
    selectedTab = MainPageTab.Person,
    selectedOpenTabId = null
)
drawerViewState.drawerState.close()
```

效果：

```text
侧边栏“个人资料”现在接到新 OpenProfilePage。
原 ProfileUpdateActivity 未删除，只是不再作为训练营主流程入口。
```

## 4. AI oncall 返回工作台入口

文件：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/ui/MainPage.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenTabContentHost.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenOnCallPage.kt
```

背景：

```text
AI助手一级页为了键盘适配隐藏底部导航栏。
此前页面顶部没有返回按钮，用户不方便切回工作台。
```

修改：

1. `MainPage` 在打开 `ai-oncall` 时传入 `mainViewModel::backToWorkbench`。
2. `OpenTabContentHost` 将 `onBackToWorkbench` 继续传给 `OpenOnCallPage`。
3. `OpenOnCallPage` 顶部左侧增加返回工作台按钮。
4. 保持底部导航隐藏和 `imePadding()` 不变，避免键盘空白问题复发。

效果：

```text
AI 页面现在可一键返回工作台。
输入框键盘适配策略保持不变。
```

## 5. 注册接口真实接入

文件：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/ui/login/logic/LoginViewModel.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenAuthRepository.kt
```

服务端实测状态：

```text
POST /auth/register 已可用。
注册成功返回 token/userId/displayName/permissions。
重复注册返回 409 ACCOUNT_EXISTS。
非法请求返回 400 INVALID_REQUEST。
```

本轮修改：

```text
移除旧的“注册接口暂未开放，请先使用演示账号登录”固定提示。
注册失败时直接展示服务端返回的中文 message。
注册成功后继续复用 OpenAuthRepository.register() 的保存 session 逻辑并进入 MainActivity。
```

说明：

```text
OpenAuthRepository.register() 此前已经按照 LoginResponse 解析并保存 token。
本轮只修正 ViewModel 的旧兜底提示。
```

## 6. 自定义 Web Tab 闭环

文件：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/model/OpenApiModels.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenTabRepository.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenWorkbenchPage.kt
```

服务端实测状态：

```text
POST   /tabs           已可用
PUT    /tabs/{tabId}   已可用
DELETE /tabs/{tabId}   已可用
```

新增模型：

```kotlin
data class UpdateCustomWebTabRequest(
    val displayName: String,
    val description: String,
    val icon: String,
    val entryUri: String,
    val sortOrder: Int? = null
)
```

新增 Repository 方法：

```kotlin
OpenTabRepository.updateCustomWebTab()
OpenTabRepository.deleteCustomTab()
```

工作台管理模式变化：

1. “新增自定义网页 Tab”继续使用 `POST /tabs`。
2. 已启用列表中，对客户端创建的自定义 Web Tab 展示：
   - 编辑
   - 删除配置
3. 编辑走 `PUT /tabs/{tabId}`。
4. 删除配置走 `DELETE /tabs/{tabId}`。
5. 创建/编辑/删除成功后刷新 `/tabs` 和 `/tabs/catalog`。

安全边界：

```text
编辑/删除仅对满足以下条件的 Tab 展示：
1. entryType == web
2. source == 服务端下发
3. id 以 custom- 开头
```

这样避免误删服务端内置 Tab，例如：

```text
approval
calendar
finance
docs
next
```

## 7. AI oncall 升级到 session/message

文件：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/model/OpenApiModels.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/network/OpenJsonParser.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenOnCallRepository.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenOnCallPage.kt
```

服务端实测状态：

```text
GET    /oncall/sessions
POST   /oncall/sessions
POST   /oncall/sessions/{sessionId}/messages
GET    /oncall/sessions/{sessionId}/messages
GET    /oncall/sessions/{sessionId}/stream?messageId=...
DELETE /oncall/sessions/{sessionId}
```

新增模型：

```kotlin
OnCallSessionDto
OnCallMessageDto
```

新增 Repository 方法：

```kotlin
createSession()
sessions()
messages()
postMessage()
stream(sessionId, messageId)
deleteSession()
```

页面逻辑：

1. 进入 AI 页面时优先请求 `/oncall/sessions`。
2. 如果已有历史会话，使用最近更新的会话并拉取历史消息。
3. 如果没有历史会话，调用 `POST /oncall/sessions` 创建新会话。
4. 用户发送消息时：

```text
POST /oncall/sessions/{sessionId}/messages
GET  /oncall/sessions/{sessionId}/stream?messageId=...
```

5. SSE 仍支持：

```text
delta
tool
done
error
unknown
```

6. 如果 session 创建失败，则自动降级到旧接口：

```text
GET /oncall/stream?message=...
```

UI 变化：

```text
AI 页顶部显示会话标题和模式：
多轮会话 / 多轮会话，历史加载失败 / 旧版单轮 SSE
```

新增“新会话”入口：

```text
点击后创建新的 session，并清空当前 UI 消息列表。
```

## 8. 审批与日程真实接口

文件：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/model/OpenApiModels.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/network/OpenJsonParser.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenBusinessRepository.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenTabContentHost.kt
```

新增模型：

```kotlin
ApprovalItemDto 扩展 amount/reason/comment/updatedAt
CalendarSummaryResponse
CalendarEventDto
```

新增接口封装：

```kotlin
approvalSummary()
approvalItems(status)
approvalDetail(itemId)
calendarSummary()
calendarEvents(date)
calendarDetail(eventId)
```

审批页变化：

1. 继续展示待处理审批数、今日已通过数。
2. 从 `GET /business/approval/items?status=all` 拉取审批列表。
3. 点击审批条目后调用：

```text
GET /business/approval/items/{itemId}
```

4. 展示申请人、状态、金额、原因、备注、创建时间。

日程页变化：

1. 从 `GET /business/calendar/summary` 拉取今日日程数。
2. 从 `GET /business/calendar/events?date=...` 拉取列表。
3. 点击日程条目后调用：

```text
GET /business/calendar/events/{eventId}
```

4. 展示描述、时间、地点、参与人。
5. 页面提供两个日期入口：

```text
今天：2026-06-01
示例日程：2026-05-31
```

说明：

```text
2026-06-01 服务端当前返回空日程。
2026-05-31 有两条服务端示例日程，便于演示列表和详情接口。
```

未做：

```text
审批通过/驳回成功路径
新增日程成功路径
```

原因：

```text
这些接口会改变种子数据，当前没有公开恢复/删除接口。
```

## 9. WebView 增强

文件：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenTabContentHost.kt
```

本轮增强：

1. 增加网页内部返回栈。
2. Android 系统返回键优先执行 `webView.goBack()`。
3. 页面头部增加“网页返回”按钮。
4. 页面头部增加“重新加载”按钮。
5. 拦截非 `http://` / `https://` 外链，避免直接拉起不可控外部 scheme。
6. 展示当前 URL，便于调试 Web Tab。

仍未做：

```text
全屏视频播放
文件下载
页面权限申请
更完整 UA 策略
```

这些属于 WebView P2/P3 增强，建议后续专门做一轮。

## 10. 调试/协议信息面板

文件：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenBusinessRepository.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenWorkbenchPage.kt
```

服务端实测状态：

```text
GET /debug/permissions    已可用
GET /debug/sample-tabs    已可用
```

工作台管理模式新增“接口调试”区域：

1. 点击“权限列表”调用 `/debug/permissions`。
2. 点击“示例 Tab”调用 `/debug/sample-tabs`。
3. 展示权限码、权限描述、示例 Tab 的 id/displayName/entryType/route。

设计边界：

```text
调试面板只放在工作台管理模式，不作为普通用户主流程。
```

## 11. DebugStatus 模型更新

文件：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/model/OpenApiModels.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/network/OpenJsonParser.kt
```

新增解析：

```kotlin
data class DebugDatabaseDto(
    val enabled: Boolean,
    val type: String
)
```

原因：

```text
接口测试发现 /debug/status 返回 database.enabled=false、type=memory、mockMode=true。
客户端暂时不在普通用户主流程突出展示数据库状态，但模型已可解析，便于后续调试页使用。
```

## 12. 10 个目标完成状态

| 序号 | 目标 | 状态 | 说明 |
|---|---|---|---|
| 1 | 修复侧边栏个人资料 | 已完成 | 接到新的“我的”页 |
| 2 | AI oncall 返回入口 | 已完成 | 顶部返回工作台 |
| 3 | 注册真实接口 | 已完成 | 移除旧 404 固定提示 |
| 4 | 自定义 Web Tab 闭环 | 已完成 | 创建/编辑/删除接服务端 |
| 5 | AI session/message | 已完成 | 新接口优先，旧 SSE 兜底 |
| 6 | 审批/日程列表详情 | 已完成 | 列表和详情均接服务端 |
| 7 | WebView 增强 | 部分完成 | 返回栈、刷新、外链拦截已做；全屏/下载未做 |
| 8 | 调试/协议信息页 | 已完成 | 放在工作台管理模式 |
| 9 | 文档同步 | 已完成 | 本文档记录本轮状态 |
| 10 | UI 细节打磨 | 部分完成 | 解决空白页/不可返回；暗色和细节仍可继续 |

## 13. 构建验证

执行目录：

```text
E:\workspace\Feishu\AI-OnCall\client-android\compose_chat-master
```

环境变量：

```powershell
$env:JAVA_HOME="E:\dev\Android Studio\jbr"
$env:ANDROID_HOME="E:\Android\SDK"
$env:ANDROID_SDK_ROOT="E:\Android\SDK"
$env:Path="$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
```

先执行 Kotlin 编译：

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

结果：

```text
BUILD SUCCESSFUL
```

再执行完整 Debug 构建：

```powershell
.\gradlew.bat :app:assembleDebug
```

结果：

```text
BUILD SUCCESSFUL
```

最终再次执行：

```powershell
.\gradlew.bat :app:assembleDebug
```

结果：

```text
BUILD SUCCESSFUL
```

## 14. 当前仍需人工验证

建议在模拟器或真机上按以下路径验证：

1. 登录页注册新账号：
   - 注册成功进入主页面。
   - 重复账号显示服务端错误。
2. 侧边栏：
   - 打开抽屉，点击“个人资料”。
   - 应进入新的“我的”页，不再进入旧 demo 空白页。
3. AI助手：
   - 顶部返回按钮可以回工作台。
   - 发送消息后走多轮 session/message。
   - 键盘弹起时输入框仍贴合键盘。
4. 工作台管理：
   - 创建自定义 Web Tab。
   - 打开 Web Tab。
   - 编辑 Web Tab 地址。
   - 删除自定义 Web Tab。
5. 审批中心：
   - 列表展示服务端审批。
   - 点击条目展示详情。
6. 团队日程：
   - 今天为空状态正常。
   - 点击“示例日程”后展示 2026-05-31 服务端日程。
   - 点击条目展示详情。
7. WebView：
   - 网页内跳转后“网页返回”可用。
   - 外部 scheme 不直接拉起。
8. 工作台管理模式接口调试：
   - 权限列表加载成功。
   - 示例 TabManifest 加载成功。

## 15. 后续建议

下一轮推荐：

1. 针对 WebView 做专项增强：
   - 全屏视频播放。
   - 下载处理。
   - 页面权限处理。
   - UA 策略。
2. 给审批通过/驳回、新增日程增加专用测试数据或恢复接口后再接成功路径。
3. 给自定义 Tab 增加二次确认弹窗，降低误删风险。
4. AI oncall 可继续补：
   - 会话列表侧栏。
   - 删除会话入口。
   - Markdown/代码块渲染。
5. 更新正式协议文档：
   - 注册、Tab CRUD、AI session/message 已实现。
   - `/me.avatarUrl` 不再作为客户端依赖。

## 16. 用户反馈优化补充

本轮根据用户反馈继续做了一次客户端信息架构和交互打磨，重点是“默认给业务用户看业务信息，联调信息进入管理/调试区域”。

### 16.1 AI 助手返回入口

问题：

- AI 助手为了键盘适配隐藏了底部导航，用户反馈顶部返回按钮不明显。

调整：

- `OpenOnCallPage` 顶部返回从图标入口改为文字胶囊按钮。
- 文案为“工作台”，左侧带返回箭头。
- 点击后调用 `MainViewModel.backToWorkbench()`，回到底部主入口的工作台。
- AI 助手仍保持直接聊天界面，不默认展示会话列表。
- 顶部保留“新会话”入口，当前语义是创建新的服务端 session。

设计判断：

- 当前阶段 AI 助手应先服务“即时咨询/接口诊断”主流程。
- 会话列表、删除会话、会话重命名等历史管理能力可以后续在服务端语义明确后再补。

### 16.2 工作台默认信息收敛

问题：

- 工作台默认界面出现过多协议/技术表达，例如 TabDefinition、Tab 来源、配置语义等。
- 这些内容对普通业务用户帮助有限，会干扰“我现在能做什么”的判断。

调整文件：

```text
app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenWorkbenchPage.kt
```

调整内容：

1. 工作台副标题从技术描述改为业务描述：
   - 当前为“团队应用与待办入口。”
2. 默认列表标题统一为“业务应用”。
3. 默认应用卡片不再展示 `服务端下发 / 客户端内置 / 本地兜底`。
4. 默认应用卡片使用业务化说明：
   - 审批：查看待处理审批与审批记录。
   - 日程：查看团队日程安排。
   - 财务：查看财务相关信息。
   - AI oncall：咨询接入、接口和配置问题。
5. 工作台统计文案从“已启用/可打开/受限”收敛为：
   - 应用
   - 可用
   - 需处理
6. 协议调试信息仍保留在工作台管理模式内：
   - `/debug/permissions`
   - `/debug/sample-tabs`

### 16.3 业务 Tab 默认页去技术化

问题：

- 业务页顶部默认展示 `Tab 配置` 卡片，包括 `id`、`route`、`entryType`、`minContainerVersion`。
- 打不开页面时也会把权限码、route、entryType 直接暴露给用户。

调整文件：

```text
app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenTabContentHost.kt
```

调整内容：

1. 删除默认 `Tab 配置` 卡片。
2. 顶部副标题改为“团队业务入口”。
3. 只有业务描述不为空时才展示描述文本。
4. 业务页返回入口统一改为“工作台”文字胶囊按钮。
5. 权限不足、版本不兼容、入口不支持等状态改为业务可读文案：
   - 权限不足：当前账号暂无访问权限，请联系管理员开通。
   - 版本不兼容：当前客户端版本过低，请升级后再打开。
   - 未接入：该业务页暂未接入当前客户端。
   - 配置不完整：该业务应用配置不完整，暂时无法打开。
6. Web Tab 默认不再把当前 URL 显示在页面顶部。
7. Web 加载失败只展示失败原因，不默认附带完整地址。
8. 外部入口占位文案改为用户能理解的“外部入口暂不可用”。
9. 财务看板占位文案从“权限演示”改为业务接入说明。

设计判断：

- 协议字段对开发联调非常重要，但不适合成为普通用户默认信息。
- 默认页应告诉用户“能不能打开、为什么不能打开、下一步该做什么”，而不是暴露协议实现。

### 16.4 审批中心交互修复

问题：

- 点击审批列表第二条时，详情卡原本出现在列表上方，页面视觉焦点跳到顶部。
- 指标卡如“待处理审批 1”没有点击反馈，用户无法感知它是否可操作。

调整文件：

```text
app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenTabContentHost.kt
```

调整内容：

1. 审批详情改为行内展开：
   - 点击某一行后，详情卡直接出现在该行下方。
   - 再点击同一行可收起。
   - 点击第二行会关闭上一行详情并加载第二行详情。
2. 详情加载仍调用服务端：
   - `GET /business/approval/items/{itemId}`
3. 指标卡变为可点击筛选入口：
   - 点击“待处理审批”后调用 `GET /business/approval/items?status=pending`
   - 点击“今日已通过”后调用 `GET /business/approval/items?status=approved`
   - 点击“查看全部”恢复 `GET /business/approval/items?status=all`
4. 当前筛选状态会展示在列表上方：
   - 当前查看：全部审批 / 待处理审批 / 已通过审批
5. 点击指标卡后会滚动到列表区域，并给对应状态的行一个短暂高亮反馈。
6. 空状态按筛选类型区分：
   - 暂无审批
   - 暂无待处理审批
   - 暂无已通过审批

说明：

- `approvedToday` 是服务端摘要字段，当前列表接口的筛选参数只有 `approved`，客户端先按协议能力展示“已通过审批”列表。
- 如果后续服务端提供 `approvedToday` 专用列表或日期筛选，客户端可再把该指标卡精确对齐为“今日已通过”明细。

### 16.5 我的页默认展示收敛

问题：

- “我的”页默认展示 Base URL、Token、容器版本、API 版本、SSE、运行模式等联调信息。
- 权限区直接展示 `tab.approval.read` 等权限码，对普通用户不友好。

调整文件：

```text
app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenProfilePage.kt
```

调整内容：

1. 默认服务卡改为“账号状态”：
   - 服务连接
   - 团队
   - 同步时间
2. 业务能力保留：
   - 入口
   - 可打开
   - 受限
3. 权限区改为“可用能力”，展示业务名：
   - 审批
   - 日程
   - 财务
   - AI oncall
4. 调试信息改为折叠区：
   - 默认只显示“展开联调信息”。
   - 展开后才展示 Base URL、Token 掩码、容器版本、API 版本、运行模式、SSE。
5. 头像仍完全由客户端本地实现，服务端不涉及。

### 16.6 构建验证

执行目录：

```text
E:\workspace\Feishu\AI-OnCall\client-android\compose_chat-master
```

执行命令：

```powershell
$env:JAVA_HOME="E:\dev\Android Studio\jbr"
$env:ANDROID_HOME="E:\Android\SDK"
$env:ANDROID_SDK_ROOT="E:\Android\SDK"
$env:Path="$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
.\gradlew.bat :app:assembleDebug
```

结果：

```text
BUILD SUCCESSFUL
```

### 16.7 本轮仍建议人工验证

1. AI 助手：
   - 从底部“AI助手”进入。
   - 顶部应出现“工作台”文字返回按钮。
   - 点击后应回到工作台，底部导航重新出现。
2. 审批中心：
   - 点击“待处理审批”指标，列表切换为待处理。
   - 点击“今日已通过”指标，列表切换为已通过。
   - 点击“查看全部”恢复全部。
   - 点击任意审批行，详情应在该行下方展开。
   - 再点击第二行，详情应切换到第二行下方，不应跳到顶部。
3. 工作台：
   - 默认页面不应出现 TabDefinition、route、entryType、服务端下发等技术说明。
   - 管理模式仍能看到接口调试入口。
4. 我的：
   - 默认页面不应直接暴露 Base URL、Token、API/SSE。
   - 点击“展开联调信息”后才展示调试字段。

## 17. AI 助手二级结构调整

用户继续反馈：AI 助手作为底部一级入口，如果点击后直接进入聊天详情页，会让返回逻辑变得不自然。合理结构应该是：

```text
底部 AI助手
  -> AI助手首页
      -> 新建咨询 / 最近会话 / 快捷能力
          -> 聊天详情页
              -> 返回 AI助手首页
```

### 17.1 调整目标

1. AI 助手拥有自己的一级首页。
2. 底部点击“AI助手”后先进入 AI 首页，而不是直接进入聊天详情。
3. AI 首页保留底部导航。
4. 聊天详情页作为 AI 助手内部二级页。
5. 聊天详情页隐藏底部导航，保证键盘和输入框空间。
6. 聊天详情页左上角返回按钮文案为“AI助手”，点击后回 AI 首页。
7. 原聊天页顶部的三个快捷按钮移到 AI 首页。

### 17.2 导航状态改造

调整文件：

```text
app/src/main/java/github/leavesczy/compose_chat/ui/logic/Models.kt
app/src/main/java/github/leavesczy/compose_chat/ui/logic/MainViewModel.kt
app/src/main/java/github/leavesczy/compose_chat/ui/MainPage.kt
```

`MainPageBottomBarViewState` 新增 AI 内部路由字段：

```kotlin
val onCallRouteKey: Long
val onCallSessionId: String?
val onCallSessionTitle: String?
val onCallInitialPrompt: String?
val onCallForceNewSession: Boolean
```

新增 ViewModel 方法：

```kotlin
openOnCallChat(...)
backToOnCallHome()
```

当前导航规则：

1. 点击底部 `AI助手`：
   - `selectedTab = AiOncall`
   - `selectedOpenTabId = null`
   - 展示 AI 助手首页
2. 在 AI 首页点击新建咨询、最近会话或快捷能力：
   - `selectedOpenTabId = "ai-oncall"`
   - 展示聊天详情页
   - 隐藏底部导航
3. 在聊天详情页点击左上角 `AI助手`：
   - `selectedTab = AiOncall`
   - `selectedOpenTabId = null`
   - 返回 AI 首页
   - 底部导航恢复

说明：

- 旧逻辑中底部 AI 会直接设置 `selectedOpenTabId = ai-oncall`，导致 AI 被当成动态业务 Tab 打开。
- 新逻辑中 AI 是一级主入口，聊天详情才使用 `selectedOpenTabId = ai-oncall` 表示二级详情页。

### 17.3 新增 AI 助手首页

调整文件：

```text
app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenOnCallPage.kt
```

新增组件：

```kotlin
OpenOnCallHomePage(...)
```

首页内容：

1. 顶部标题：
   - AI助手
   - 最近会话、快捷能力与问题诊断。
2. 主操作：
   - 新建咨询
3. 快捷能力：
   - 协议问答
   - 配置诊断
   - 错误定位
4. 最近会话：
   - 调用 `GET /oncall/sessions`
   - 展示最近 6 条
   - 点击会话进入聊天详情并加载历史消息
5. 建议问题：
   - 如何接入一个业务 Tab？
   - TabManifest 必填字段有哪些？
   - 为什么会版本不兼容？

快捷能力行为：

1. 点击快捷能力后创建新会话。
2. 根据能力预填输入框：
   - 协议问答：解释协议字段和必填规则
   - 配置诊断：检查 TabManifest 配置
   - 错误定位：根据错误码或日志定位问题
3. 不自动发送，用户可以编辑后再发。

### 17.4 聊天详情页调整

`OpenOnCallPage` 新增入参：

```kotlin
initialSessionId: String?
initialSessionTitle: String?
initialPrompt: String?
forceNewSession: Boolean
onBackToOnCallHome: (() -> Unit)?
```

聊天详情规则：

1. 如果传入 `initialSessionId`：
   - 调用 `GET /oncall/sessions/{sessionId}/messages`
   - 加载该会话历史消息
2. 如果 `forceNewSession = true`：
   - 调用 `POST /oncall/sessions`
   - 创建新会话
3. 如果传入 `initialPrompt`：
   - 将内容预填到输入框
   - 不自动发送
4. 顶部返回按钮文案从“工作台”改成“AI助手”
5. 聊天详情页不再展示三个快捷能力卡片，避免占用聊天空间。

### 17.5 构建验证

执行目录：

```text
E:\workspace\Feishu\AI-OnCall\client-android\compose_chat-master
```

执行命令：

```powershell
$env:JAVA_HOME="E:\dev\Android Studio\jbr"
$env:ANDROID_HOME="E:\Android\SDK"
$env:ANDROID_SDK_ROOT="E:\Android\SDK"
$env:Path="$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
.\gradlew.bat :app:assembleDebug
```

结果：

```text
BUILD SUCCESSFUL
```

### 17.6 建议人工验证

1. 点击底部 `AI助手`：
   - 应进入 AI 助手首页。
   - 底部导航应继续显示。
2. 点击 `新建咨询`：
   - 应进入聊天详情页。
   - 底部导航应隐藏。
   - 左上角应显示 `AI助手` 返回按钮。
3. 点击聊天详情页左上角 `AI助手`：
   - 应返回 AI 首页。
   - 底部导航恢复。
4. 点击快捷能力：
   - 应进入新聊天详情页。
   - 输入框应预填对应问题模板。
   - 不应自动发送。
5. 点击最近会话：
   - 应进入对应历史会话。
   - 应加载历史消息。
