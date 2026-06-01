# Android 客户端交接文档（2026-05-31）

## 1. 当前开发目录

统一仓库目录：

```text
E:\workspace\Feishu\AI-OnCall
```

Android Studio 应打开的项目目录：

```text
E:\workspace\Feishu\AI-OnCall\client-android\compose_chat-master
```

历史目录：

```text
E:\workspace\Feishu\code\compose_chat-master
```

说明：历史目录只作为旧资料参考，后续不要继续在那里开发或提交代码。

当前 Git 分支：

```text
liujiaxin
```

## 2. 项目定位

项目来自 `compose_chat` demo，原始能力是 Kotlin + Jetpack Compose + MVVM + 腾讯 IM SDK。

当前训练营客户端目标不是删除原 demo，而是在保留原逻辑的基础上新增开放式 Tab 容器能力：

1. 服务端登录态。
2. TabManifest 拉取与解析。
3. 动态业务 Tab 容器。
4. Tab 管理：启用、停用、后续支持新增和修改。
5. AI oncall 助手聊天页。
6. REST / SSE 接口对接。
7. 产品化中文 UI。

新能力主要集中在：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/
```

原 demo 的 IM 代码仍保留，但当前底部主入口已收敛为：

```text
工作台 / AI助手 / 我的
```

## 3. 云端服务信息

服务端 mock 地址：

```text
http://121.40.241.161:8080
```

已验证可用接口：

```text
GET  /health
POST /auth/login
GET  /me
GET  /tabs
GET  /tabs/catalog
POST /me/tabs
DELETE /me/tabs/{tabId}
GET  /business/approval/summary
GET  /oncall/stream?message=...
```

当前待服务端补齐或后续升级接口：

```text
POST /auth/register
POST /tabs
PUT /tabs/{tabId}
DELETE /tabs/{tabId}
POST /oncall/sessions
POST /oncall/sessions/{sessionId}/messages
GET /oncall/sessions/{sessionId}/messages
GET /oncall/sessions/{sessionId}/stream?messageId=...
GET /me 返回 avatarUrl 字段
```

演示账号：

```text
普通账号：opentab-demo / demo123
管理员账号：opentab-admin / admin123
```

## 4. 今日已完成的客户端能力

### 4.1 登录与注册入口

相关文件：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/ui/login/LoginPage.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/ui/login/logic/LoginViewModel.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/ui/login/logic/Models.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenAuthRepository.kt
```

完成内容：

1. 登录页标题统一为“开放式Tab容器”。
2. 登录页改为产品化中文 UI。
3. 登录走服务端 `POST /auth/login`。
4. 增加注册入口和本地表单校验。
5. 注册接口当前服务端未实现，客户端保留请求和中文错误提示，不伪造成功。

### 4.2 主入口收敛

相关文件：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/ui/MainPage.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/ui/MainPageBottomBar.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/ui/logic/MainViewModel.kt
```

完成内容：

1. 底部栏只展示 `工作台 / AI助手 / 我的`。
2. 默认进入工作台。
3. 原 demo 的 `消息 / 通讯录` 代码未删除，只是不作为当前产品一级入口展示。
4. 从工作台打开动态业务 Tab 后，业务页提供返回工作台能力。

### 4.3 工作台与动态 Tab

相关文件：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenWorkbenchPage.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenTabContentHost.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenTabRepository.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/tab/OpenTabModels.kt
```

完成内容：

1. 工作台从 `/tabs` 拉取服务端 TabManifest。
2. 支持权限不足、版本不兼容、入口类型不支持、配置异常等状态。
3. 工作台顶部“刷新 / 管理”改为图标按钮，避免窄屏文字显示不全。
4. 管理模式支持查看已启用 Tab 和可添加的内置 Tab。
5. 支持 `POST /me/tabs` 启用 Tab。
6. 支持 `DELETE /me/tabs/{tabId}` 停用 Tab。
7. 支持自定义 Web Tab 表单、预览和接口预留；当前 `POST /tabs` 服务端未实现时会提示接口暂未开放。
8. Tab 卡片增加来源标识：
   - `服务端下发`
   - `客户端内置`
   - `本地兜底`

客户端内置 Tab：

```text
ai-oncall
bilibili-web
```

`bilibili-web` 当前用于演示 Web Tab 承载能力，地址为：

```text
https://m.bilibili.com
```

### 4.4 审批中心接口接入

相关文件：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/model/OpenApiModels.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/network/OpenJsonParser.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenTabRepository.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenTabContentHost.kt
```

完成内容：

1. 接入 `GET /business/approval/summary`。
2. 展示待处理审批数、今日已通过数。
3. 展示审批条目：标题、申请人、状态、创建时间。

### 4.5 AI oncall 聊天页

相关文件：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenOnCallPage.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenOnCallRepository.kt
```

完成内容：

1. 增加 AI oncall 聊天页面壳。
2. 支持快捷问题。
3. 支持用户输入并发送。
4. 接入旧版 SSE：`GET /oncall/stream?message=...`。
5. 支持流式增量展示 AI 回复。
6. 预留 `tool`、`error`、`done` 事件展示逻辑。
7. 底部 `AI助手` 标签进入该页面。
8. 为避免输入框和键盘之间出现空白，当前 `AI助手` 一级页面隐藏底部导航栏，并使用 `imePadding()`。

### 4.6 “我的”页面与头像

相关文件：

```text
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/ui/OpenProfilePage.kt
client-android/compose_chat-master/app/src/main/java/github/leavesczy/compose_chat/open/repository/OpenAvatarRepository.kt
client-android/compose_chat-master/app/src/main/res/drawable/open_default_avatar.xml
```

完成内容：

1. “我的”页展示服务端 `/me` 返回的用户信息、团队信息、权限和服务状态。
2. 页面增加 `statusBarsPadding()`，避免和系统状态栏重叠。
3. 新增默认卡通头像资源。
4. 支持点击头像从本机相册选择图片。
5. 本地头像复制到 App 私有目录，并保存路径，避免直接保存 `content://` Uri 导致重启后授权失效。

当前头像优先级：

```text
用户本地选择头像 > 客户端默认卡通头像
```

后续服务端返回 `avatarUrl` 后推荐升级为：

```text
服务端 avatarUrl > 用户本地选择头像 > 客户端默认卡通头像
```

## 5. 构建验证

最后一次构建命令：

```powershell
$env:JAVA_HOME="E:\dev\Android Studio\jbr"
$env:ANDROID_HOME="E:\Android\SDK"
$env:ANDROID_SDK_ROOT="E:\Android\SDK"
$env:Path="$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
.\gradlew.bat :app:assembleDebug
```

执行目录：

```text
E:\workspace\Feishu\AI-OnCall\client-android\compose_chat-master
```

结果：

```text
BUILD SUCCESSFUL
```

## 6. 当前已知问题

### 6.1 侧边栏个人资料页

现象：

1. 从左侧抽屉点击“个人资料”后进入空白页。
2. 进入后没有明确返回路径。

建议：

1. 当前训练营产品主流程已经有“我的”页，因此侧边栏里的“个人资料”可以改为跳转到新的 `OpenProfilePage`。
2. 或者暂时隐藏侧边栏个人资料入口，避免用户进入旧 demo 的空白页。
3. 如果继续保留侧边栏，需要补返回按钮或关闭抽屉逻辑。

### 6.2 AI oncall 页面缺少返回入口

现象：

1. 当前 `AI助手` 页面为了修复键盘空白，隐藏了底部导航栏。
2. 页面没有返回按钮，因此进入后不方便切回工作台或我的页面。

建议：

1. 在 AI oncall 顶部左侧增加返回/菜单按钮。
2. 可选择返回工作台，或重新显示底部导航但重新计算键盘 inset。
3. 优先方案：AI 页面顶部增加一个返回工作台按钮，保持底部导航隐藏，避免键盘适配问题反复出现。

### 6.3 WebView 后续增强

当前 B站 Web Tab 已能用 WebView 承载，但还缺：

1. 网页内部返回栈。
2. 外链拦截。
3. 全屏视频播放处理。
4. 文件下载和页面权限处理。
5. 更稳定的 UA 和错误页策略。

### 6.4 AI 会话模型

当前 AI oncall 使用旧 SSE 单轮接口：

```text
GET /oncall/stream?message=...
```

后续服务端补齐 session/message 接口后，需要升级为多轮会话模型。

## 7. 下一步优先级

建议下一轮按这个顺序做：

1. 修复侧边栏个人资料空白和不可返回问题。
2. 给 AI oncall 页面补返回入口，保证一级页面之间可流畅切换。
3. 和服务端确认 `POST /tabs`、`PUT /tabs/{tabId}`、`DELETE /tabs/{tabId}` 的最终协议，完成自定义 Tab 新增/修改/删除闭环。
4. 和服务端确认注册接口是否要做；如果要做，补齐 `POST /auth/register`。
5. 和 AI 同学确认新版会话接口，升级 AI oncall 为多轮聊天。
6. 优化 WebView：返回栈、全屏播放、外链拦截。
7. 继续打磨 UI：图标、间距、暗色模式、加载态、错误态、空状态。

## 8. 提交注意事项

不要提交这个旧交接文件：

```text
docs/client-handoff-new-host.md
```

它是上一台主机给当前开发者看的资料，不适合作为正式项目文档继续上传。

本次新增交接文件是：

```text
docs/client-handoff-2026-05-31.md
```

正式工作记录是：

```text
docs/client-development-2026-05-31.md
```

