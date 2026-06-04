---
topic: 协议典型问答对（RAG 检索评测用）
keywords: [问答对, Q&A, 检索评测, 准确率, 命中率, 典型问题]
usage: |
  AI 同事使用方式：
  1. 将问题列作为检索查询，验证向量库能否命中正确的知识片段
  2. 将答案作为 ground truth，对比 Agent 生成答案的准确率
  3. 未命中或答错的问题，针对性补充知识片段或调优检索策略
total_questions: 55
---

# 协议典型问答对

## 一、Tab 元信息（8 题）

### Q1
**问题**：TabDefinition 有哪些必填字段？
**答案**：5 个必填字段：id（反向域名格式，全局唯一）、displayName（≤16 字符）、icon（24dp×24dp）、route（以 / 开头）、version（SemanticVersion 格式）。非必填字段有 minContainerVersion（默认 1）、permissions（默认空列表）、extension（默认 null）。

### Q2
**问题**：id 字段的格式要求是什么？
**答案**：id 必须使用反向域名格式，例如 `com.example.content-audit`，且全局唯一，不能与其他 Tab 重复。如果重复注册，容器会拒绝并报错误码 1001。

### Q3
**问题**：displayName 最多多少字符？
**答案**：最多 16 个字符。该字段显示在容器的 TitleBar 上作为标题。

### Q4
**问题**：icon 的尺寸要求是多少？
**答案**：24dp × 24dp。显示在 BottomBar 上。

### Q5
**问题**：route 字段有什么格式要求？
**答案**：必须以 `/` 开头，例如 `/content-audit`。全局唯一，不可重复。容器根据 route 前缀匹配来分发 Tab。

### Q6
**问题**：minContainerVersion 是干什么的？
**答案**：声明当前 Tab 需要的最低容器版本。如果容器版本低于此值，容器会拒绝加载该 Tab，并 Toast 提示"请升级客户端"，对应错误码 1002。默认值为 1。

### Q7
**问题**：SemanticVersion 的格式是什么？
**答案**：`major.minor.patch`，例如 `SemanticVersion(1, 2, 3)` 即 `1.2.3`。major 表示不兼容变更，minor 表示向后兼容新增，patch 表示文档/bug 修复无行为变化。

### Q8
**问题**：permissions 字段怎么填？
**答案**：填 Android 权限字符串列表，例如 `listOf("android.permission.CAMERA")`。如果 Tab 声明了权限但未在 AndroidManifest.xml 中注册对应 `<uses-permission>`，或用户未授权，容器会报错误码 1006 并拒绝加载。

---

## 二、生命周期（9 题）

### Q9
**问题**：Tab 生命周期有哪些回调？
**答案**：5 个回调。4 个必实现：onCreate（首次创建）、onResume（可见）、onPause（不可见）、onDestroy（销毁）。1 个可选实现：onConfigChange（配置变更，如横竖屏切换）。

### Q10
**问题**：onCreate 和 onResume 的调用顺序是什么？
**答案**：Tab 首次进入时，先调用 onCreate 再调用 onResume。顺序是：onCreate → onResume。

### Q11
**问题**：Tab 切换时生命周期怎么走？
**答案**：当前 Tab 先调用 onPause，然后新 Tab 调用 onResume。如果新 Tab 是首次显示，则新 Tab 先 onCreate 再 onResume。例如从 TabA 切换到 TabB（首次）：TabA.onPause → TabB.onCreate → TabB.onResume。

### Q12
**问题**：onResume 可能被频繁调用吗？
**答案**：是的。每次从其他 Tab 切回来都会触发 onResume。因此建议 onResume 中只做轻量刷新操作，避免重操作（如大数据量网络请求）。

### Q13
**问题**：onCreate 里应该做什么？
**答案**：初始化操作——加载数据、注册监听、初始化资源。注意不能做超过 5 秒的同步操作，否则超时触发 1004 错误。

### Q14
**问题**：onDestroy 之后还能收到回调吗？
**答案**：不会。onDestroy 是最后一个回调，之后容器不会再调用该 Tab 的任何方法。因此 onDestroy 中应释放全部资源。

### Q15
**问题**：每个生命周期回调最多执行多久？
**答案**：最多 5 秒。超时后容器会显示兜底页面，对应错误码 1004。

### Q16
**问题**：生命周期回调抛异常会影响其他 Tab 吗？
**答案**：不会。异常保护机制确保单个 Tab 的回调异常不会影响其他 Tab。当前 Tab 显示兜底页面并打印日志，对应错误码 1005。

### Q17
**问题**：onConfigChange 是必须实现的吗？
**答案**：不是必须的。它是一个可选回调，接口中已提供空实现 `= {}`。如果 Tab 不需要处理横竖屏切换、深色模式等配置变更，可以不重写。

---

## 三、扩展点（8 题）

### Q18
**问题**：有哪些扩展点可以配置？
**答案**：3 个扩展点：TitleBar（右侧按钮/菜单）、FAB（悬浮按钮）、BottomPanel（底部扩展面板）。三个都是可选的，不配置则使用容器默认行为。

### Q19
**问题**：TitleBar 右侧怎么加按钮？
**答案**：通过 TabExtension 的 titleBar 字段配置 TitleBarExtension，有三种方式：rightIcon（单个图标按钮）、rightText（文字按钮）、menuItems（三点溢出菜单列表）。

### Q20
**问题**：rightIcon 和 menuItems 能同时显示吗？
**答案**：不能。两者互斥，当 rightIcon 不为 null 时，menuItems 被忽略。只能二选一。

### Q21
**问题**：怎么加悬浮按钮 FAB？
**答案**：配置 TabExtension 的 fab 字段，传入 FabExtension（icon, label, onClick）。FAB 显示在页面右下角。

### Q22
**问题**：BottomPanel 是什么？
**答案**：页面底部可展开/收起的扩展面板，配置 BottomPanel(content, defaultHeight)。content 是 Composable 内容，defaultHeight 指定默认高度（dp）。

### Q23
**问题**：扩展点不配置会怎样？
**答案**：不配置（即 extension = null 或某个扩展点为 null）则使用容器的默认行为：TitleBar 右侧无按钮、无 FAB、无 BottomPanel。

### Q24
**问题**：MenuItem 怎么定义？
**答案**：MenuItem 包含三个字段：id（唯一标识，如 "filter"）、label（显示文字，如 "筛选"）、onClick（点击回调 lambda）。

### Q25
**问题**：哪些扩展点本期不支持？
**答案**：3 个已预留但本期不支持：BottomBar 动态增加 Tab（当前只支持预定义枚举 MainPageTab）、自定义 Drawer 内容、Tab 间通信/数据共享。这些留给后续版本。

---

## 四、路由（6 题）

### Q26
**问题**：route 字段怎么写？有格式要求吗？
**答案**：必须以 `/` 开头，且全局唯一。建议使用有意义的路径名，如 `/content-audit`。不要与容器内置路由冲突。

### Q27
**问题**：Tab 之间怎么切换？
**答案**：通过容器提供的 switchTab 方法：`container.switchTab(route = "/content-audit")`。

### Q28
**问题**：怎么带参数跳转到某个 Tab？
**答案**：在 route 后附加查询参数：`container.switchTab(route = "/content-audit?filter=pending")`。查询参数由 Tab 自行在 onCreate 中解析，容器只透传不处理。

### Q29
**问题**：容器怎么根据 route 分发到正确的 Tab？
**答案**：容器使用 route 前缀匹配。例如注册了 route = "/content-audit" 的 Tab，访问 `/content-audit?filter=pending` 时会匹配到它。

### Q30
**问题**：Tab 内部怎么跳转到独立 Activity？
**答案**：接入方自行调用 `context.startActivity(Intent(context, DetailActivity::class.java))`，容器不拦截。这在 Tab 内需要打开详情页等场景使用。

### Q31
**问题**：Tab 怎么接收路由参数？
**答案**：在 onCreate(bundle: Bundle?) 中解析。容器将完整路由放入 bundle，例如 bundle.getString("route") 得到 `/content-audit?filter=pending`，Tab 自行解析 filter 等参数。

---

## 五、兼容策略（6 题）

### Q32
**问题**：版本号 major、minor、patch 分别代表什么？
**答案**：major 表示不兼容变更（如删除必填字段、改回调签名），minor 表示向后兼容新增（如新增可选字段、新增扩展点），patch 表示无行为变化（文档修正、bug 修复）。

### Q33
**问题**：什么情况该升级 major 版本？
**答案**：发生了不兼容变更时——删除必填字段、修改回调方法签名、改变路由规则等。这种情况下老容器无法正常加载新版本的 Tab，必须升级容器。

### Q34
**问题**：老容器遇到新版本 Tab 里不认识的字段怎么办？
**答案**：容器忽略多余字段（向前兼容）。Tab 发来的新字段，老容器不认识，直接忽略，不影响基本功能。

### Q35
**问题**：容器新增了字段但 Tab 没填，会怎样？
**答案**：容器使用该字段的默认值（向后兼容）。老 Tab 不受影响。

### Q36
**问题**：怎么废弃一个字段？
**答案**：分两步：v1.x 版本标记 `@Deprecated` 并注释替代方案 → v2.0 正式移除。给接入方足够的过渡时间。

### Q37
**问题**：容器版本太低无法加载 Tab 怎么办？
**答案**：容器会拒绝加载（错误码 1002），Toast 提示"请升级客户端"。Tab 方应合理设置 minContainerVersion，引导用户升级。

---

## 六、错误码（7 题）

### Q38
**问题**：Tab 注册失败有哪些错误码？
**答案**：共 6 个错误码：1001（ID 重复）、1002（容器版本过低）、1003（缺失必填字段）、1004（生命周期回调超时 >5s）、1005（回调抛异常）、1006（权限不足）。

### Q39
**问题**：1001 错误是什么原因？怎么解决？
**答案**：两个 Tab 使用了相同的 id 字符串。解决方案：检查所有已注册的 Tab，确保 id 全局唯一，修改其中一个为不同的值。

### Q40
**问题**：1002 错误怎么解决？
**答案**：容器版本低于 Tab 要求的 minContainerVersion。解决方案：降低 Tab 的 minContainerVersion，或引导用户升级客户端到最新版本。

### Q41
**问题**：1003 错误是什么？怎么修？
**答案**：Tab 缺失了必填字段（id / displayName / icon / route / version 任一为空或不合法）。检查 TabDefinition 所有必填字段是否已正确赋值。

### Q42
**问题**：1004 超时了怎么排查？
**答案**：生命周期回调执行超过 5 秒。重点检查 onCreate 和 onResume 中是否有同步耗时操作——网络请求、数据库查询、文件 I/O 等必须改为异步执行。

### Q43
**问题**：1005 异常保护是什么意思？
**答案**：当某个 Tab 的生命周期回调（如 onResume）内部抛出未捕获异常时，容器捕获该异常，为此 Tab 显示兜底页面，打印日志，但不影响其他 Tab 的正常运行。

### Q44
**问题**：兜底页面什么时候显示？
**答案**：当 Tab 触发 1004（超时）或 1005（异常）时，容器为该 Tab 显示兜底页面，提示用户"该功能暂不可用"。其他 Tab 照常使用。

---

## 七、注册方式（5 题）

### Q45
**问题**：怎么注册一个 Tab？需要传哪些参数？
**答案**：调用 `container.registerTab(definition, lifecycle, page)`，传三个参数：TabDefinition（元信息）、TabLifecycle（生命周期回调）、Composable 页面 lambda。

### Q46
**问题**：静态注册和动态注册有什么区别？
**答案**：静态注册在 Kotlin 代码中直接定义 TabDefinition + TabLifecycle + Composable，类型安全、IDE 可检查。动态注册通过 JSON 配置文件下发 Tab 列表，服务端可动态控制 Tab 显隐和排序，无需发版。

### Q47
**问题**：什么时候用静态注册？
**答案**：固定业务 Tab（如内容审核面板、数据看板）推荐使用静态注册。因为 Kotlin 代码类型安全，编译期能发现错误，IDE 有自动补全和检查。

### Q48
**问题**：动态注册的 JSON 格式是什么？
**答案**：JSON 对象包含：id（字符串）、displayName（字符串）、icon（字符串）、route（字符串）、version（{major, minor} 对象）、minContainerVersion（整数）、permissions（数组）、extension（可选对象，如 titleBar: {rightText: "刷新"}）。

### Q49
**问题**：两种注册方式能混用吗？
**答案**：可以。容器同时支持静态和动态注册。静态用于固定业务，动态用于运营灵活配置。

---

## 八、排查指南（3 题）

### Q50
**问题**：Tab 注册后不显示，最先检查什么？
**答案**：按顺序排查：(1) 检查日志中是否有 1001（id 重复）；(2) 确认容器版本 ≥ minContainerVersion；(3) 检查必填字段是否都正确填写；(4) 确认 registerTab 是否被调用到了（打断点验证）。

### Q51
**问题**：TitleBar 右侧按钮配置了 menuItems 但不生效，为什么？
**答案**：很可能是同时配置了 rightIcon。rightIcon 和 menuItems 互斥，rightIcon 不为 null 时 menuItems 被忽略。解决办法：只设置其中一个。

### Q52
**问题**：onCreate 里写同步网络请求有什么问题？
**答案**：会导致 1004 超时错误——onCreate 回调超过 5 秒未返回，容器显示兜底页面。正确做法是：网络请求应在协程中异步执行，onCreate 只启动请求不等待结果。

---

## 九、接入清单场景（3 题）

### Q53
**问题**：接入完成上线前要检查哪些项？
**答案**：逐项检查 5 大类：(1) 元信息——必填字段完整、id/route 全局唯一；(2) 生命周期——四个回调全部实现、onDestroy 释放资源；(3) 页面——Composable 不含自己 Scaffold/TopBar；(4) 扩展点——rightIcon 和 menuItems 不同时设置；(5) 测试——Tab 切换、横竖屏、杀进程重进、真机验证。

### Q54
**问题**：我的 Composable 页面能不能有自己的 TopBar？
**答案**：不能。容器已提供统一的 TopBar/Scaffold，Tab 的 Composable 应只渲染主内容区域。如果 Tab 自己套 Scaffold，会出现双层 TopBar。

### Q55
**问题**：切换到另一个 Tab 再切回来，我的数据是不是还在？
**答案**：取决于你的实现。onPause → onResume 流程中 Tab 实例不会被销毁，内存中的状态（如 ViewModel 数据）仍在。但不要依赖 onResume 恢复全部状态——建议在 onResume 中主动刷新数据。

