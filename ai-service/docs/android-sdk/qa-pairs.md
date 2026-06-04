---
topic: 协议典型问答对（RAG 检索评测用）
keywords: [问答对, Q&A, 检索评测, 准确率, 命中率, 典型问题]
usage: |
  AI 同事使用方式：
  1. 将问题列作为检索查询，验证向量库能否命中正确的知识片段
  2. 将答案作为 ground truth，对比 Agent 生成答案的准确率
  3. 未命中或答错的问题，针对性补充知识片段或调优检索策略
total_questions: 90
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
**答案**：是的。每次从其他 Tab 切回来都会触发 onResume。因此建议 onResume 中只做轻量刷新操作，避免重操作。

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
**答案**：接入方自行调用 `context.startActivity(Intent(context, DetailActivity::class.java))`，容器不拦截。

### Q31
**问题**：Tab 怎么接收路由参数？
**答案**：在 onCreate(bundle: Bundle?) 中解析。容器将完整路由放入 bundle，例如 bundle.getString("route") 得到 `/content-audit?filter=pending`，Tab 自行解析 filter 等参数。

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

---

## 十、ViewModel 与生命周期协作（7 题）

### Q56
**问题**：ViewModel 应该在哪个生命周期回调里创建？
**答案**：推荐在 `onCreate` 中创建或获取 ViewModel：`viewModel = ViewModelProvider(viewModelStoreOwner)[MyViewModel::class.java]`。不要在 onResume 中创建，因为 onResume 会被频繁调用。

### Q57
**问题**：Tab 调用 onDestroy 了，ViewModel 也会同时销毁吗？
**答案**：不会。Tab.onDestroy 不等于 ViewModel 销毁。ViewModel 的生命周期由 ViewModelStoreOwner 控制，只有当 ViewModelStoreOwner（如宿主 Activity）被销毁时，ViewModel.onCleared() 才会被调用。

### Q58
**问题**：ViewModel 的 viewModelScope 有什么用？
**答案**：`viewModelScope` 是 ViewModel 的协程作用域，当 ViewModel.onCleared() 被调用时，所有在此作用域内启动的协程会自动取消。不需要手动管理协程生命周期，也不会造成内存泄漏。

### Q59
**问题**：多个 Tab 能共享一个 ViewModel 吗？
**答案**：可以。只需给两个 Tab 传入同一个 ViewModelStoreOwner，然后各自通过 `ViewModelProvider(storeOwner)[SharedViewModel::class.java]` 获取，就会得到同一个 ViewModel 实例。但 v1.0 协议不提供 Tab 间通信机制，共享 ViewModel 是容器实现层的选择。

### Q60
**问题**：onResume 里应该从 ViewModel 加载数据还是重新发网络请求？
**答案**：推荐在 onResume 中调用 `viewModel.refresh()` 做轻量刷新（如检查增量更新），而不是重新发全量网络请求。因为 onResume 会被频繁调用，全量重拉浪费流量和性能。

### Q61
**问题**：ViewModel 里面抛异常会影响 Tab 生命周期吗？
**答案**：如果异常未在 ViewModel 内部捕获，传播到 `viewModelScope.launch` 的顶层，协程会被取消但不会传播到 TabLifecycle 回调——不会触发 1005 错误。但如果你在协程中调用了一个同步方法抛异常传播到了 onCreate/onResume 的回调线程，就会触发 1005。

### Q62
**问题**：Tab 切走再切回来，ViewModel 里的数据还在吗？
**答案**：在。onPause → onResume 不会销毁 ViewModel，所有 LiveData / StateFlow 数据保持。但建议在 onResume 中主动调用 `viewModel.refresh()` 以确保数据是最新的。

---

## 十一、异步编程（7 题）

### Q63
**问题**：onCreate 里能做网络请求吗？
**答案**：能做，但必须是**异步的**。正确做法是在 ViewModel 中用 `viewModelScope.launch` 发起网络请求，onCreate 只负责触发请求不等待结果。如果同步调用 `OkHttpClient.execute()` 会阻塞回调线程超过 5 秒，触发 1004 超时错误。

### Q64
**问题**：为什么我的 onCreate 触发了 1004 超时？已经用了协程。
**答案**：检查是否用了 `runBlocking` 包裹协程代码——`runBlocking` 会阻塞当前线程直到协程完成，和同步阻塞一样。正确做法是用 `viewModelScope.launch`（不阻塞）替代 `runBlocking`。

### Q65
**问题**：协程应该用哪个 Dispatcher？
**答案**：网络请求——Room/Retrofit 的 suspend 函数自动在后台线程执行，不需要手动指定。耗时计算——用 `viewModelScope.launch(Dispatchers.Default)`。UI 更新——自动回到 Main 线程，不需要手动 `withContext(Dispatchers.Main)`。绝对不要用 `Dispatchers.Unconfined`，它会让代码执行线程不确定。

### Q66
**问题**：怎么区分是网络超时还是生命周期回调超时？
**答案**：回调超时（1004）→ Logcat 有 `LIFECYCLE_TIMEOUT`，堆栈在 `onCreate` 或 `onResume` 方法内。网络超时 → Logcat 有 OkHttp `timeout` 日志，出现在 ViewModel 层而非生命周期回调层。两者错误码不同、堆栈不同。

### Q67
**问题**：数据库查询能在 onResume 里直接调用吗？
**答案**：不能同步调用。Room 的 `@Query suspend fun` 必须在协程中调用。如果直接调用 `dao.queryAll()` 同步方法且数据量大，会阻塞主线程触发 1004。应通过 ViewModel 的 `viewModelScope.launch` 异步查询。

### Q68
**问题**：生命周期回调里能直接开 Thread 吗？
**答案**：技术上可以，但强烈不推荐。手动线程管理容易泄漏，且容器无法感知线程状态——即使 Tab 已被销毁，线程可能还在运行。统一使用 `viewModelScope.launch`，自动绑定生命周期。

### Q69
**问题**：协程里报 1005 异常怎么排查？
**答案**：1005 是**生命周期回调本身**抛异常，不是协程内部异常。排查：是否在 onCreate/onResume 的 lambda 体里直接写了可能抛异常的同步代码（如 `JSON.parse()`、`Intent.getParcelableExtra()!!`）。把这些移到 ViewModel 的协程中并用 try-catch 包裹即可。

---

## 十二、Compose 集成约束（7 题）

### Q70
**问题**：Tab 的 Composable 能不能包含 Scaffold？
**答案**：不能。容器已经提供顶层 Scaffold（含 TopBar + BottomBar），如果 Tab 内部再套一个 Scaffold，会出现双层 TopBar、双层 padding，布局错乱。

### Q71
**问题**：为什么不能自己设置 WindowInsets？
**答案**：容器已在顶层 Scaffold 处理了状态栏、导航栏的安全区域（WindowInsets）。Tab 的 Composable 只需要渲染业务内容，inset 由容器自动处理，Tab 内部无需关心。

### Q72
**问题**：Tab 里能用 LazyColumn 吗？
**答案**：可以。LazyColumn、LazyRow、LazyVerticalGrid 等懒加载组件完全可用，是推荐做法。只需注意不要包裹在额外的 Scaffold 中即可。

### Q73
**问题**：Tab 里能用 Navigation Compose 做子路由吗？
**答案**：v1.0 不支持。容器没有嵌套 NavHost 的接口。Tab 内部需要页面跳转时，使用独立 Activity 的 `startActivity` 方式。

### Q74
**问题**：容器提供的 MaterialTheme 我的 Tab 能直接用吗？
**答案**：可以。Tab 的 Composable 自动继承容器的 MaterialTheme，直接使用 `MaterialTheme.colorScheme.primary`、`MaterialTheme.typography.titleLarge` 等即可，无需重新包裹 MaterialTheme。

### Q75
**问题**：Modifier 使用有什么限制吗？
**答案**：可以使用 Modifier.fillMaxSize() / padding() / weight() 等常规修饰符。但不要使用 Modifier.statusBarsPadding()/navigationBarsPadding()/systemBarsPadding()——这些与 WindowInsets 相关，容器已处理。

### Q76
**问题**：哪些 Compose 组件是确定不能用的？
**答案**：4 类禁止：Scaffold、TopAppBar/CenterAlignedTopAppBar、NavigationBar/BottomAppBar、ModalNavigationDrawer。原因是容器已提供这些顶层结构，重复使用会导致双层布局。

---

## 十三、端到端调试（7 题）

### Q77
**问题**：Tab 不显示，系统性排查步骤是什么？
**答案**：按决策树排查：(1) 打断点在 `registerTab` 入口，确认是否被调用；(2) 看返回值是不是 `RegistrationResult.Failed`，errorCode 是 1001/1002/1003/1006？(3) 抓包看服务端 GET /api/v1/tabs 返回的 enabled 字段；(4) Logcat 搜 `TabRegistry` 看注册日志。按此顺序逐层排除。

### Q78
**问题**：Logcat 搜什么关键字能快速定位 Tab 相关问题？
**答案**：`TabRegistry`（注册/切换日志）、`registerTab`（注册事件）、`switchTab`（切换事件）、`1001`~`1006`（协议错误码）、`LIFECYCLE_TIMEOUT`（超时）、`LIFECYCLE_EXCEPTION`（异常）。

### Q79
**问题**：为什么 registerTab 调了但没效果？
**答案**：常见原因：(1) registerTab 在 Container 初始化之前调用——注册被丢弃，需等容器就绪后再注册；(2) 服务端 enabled=false——检查 /api/v1/tabs 返回；(3) ProGuard 混淆了 Composable——release 包需要 keep 规则。

### Q80
**问题**：debug 模式 Tab 正常，release 包 Tab 消失了怎么办？
**答案**：ProGuard/R8 混淆导致。在 proguard-rules.pro 中添加：`-keep class **.protocol.** { *; }` 和 `-keep class **.TabDefinition { *; }`。确保协议数据类和接口不被混淆。

### Q81
**问题**：切了几个 Tab 后 App 崩溃 OOM 了，怎么排查？
**答案**：Tab 切换时旧页面资源未释放。检查 onPause/onDestroy 中是否清理了大对象（Bitmap 缓存、大量数据列表）。建议 onPause 中释放重资源，onResume 中重建。

### Q82
**问题**：生命周期回调没按预期顺序执行——只看到 onResume 没看到 onCreate？
**答案**：这是正常行为。onCreate 只在 Tab 首次创建时调用一次。如果 Tab 已创建过（在后台），切换回来只会触发 onResume，不会重新 onCreate。首次 = onCreate + onResume，非首次 = 仅 onResume。

### Q83
**问题**：服务端配置和本地配置冲突了怎么办？
**答案**：服务端优先。当 /api/v1/tabs 返回 `enabled=false` 时，即使本地已静态注册该 Tab，也不显示。当返回 `extension` 不为 null 时，覆盖本地 TabDefinition 的 extension 字段。本地配置作为兜底（服务端不可用时使用）。

---

## 十四、Gradle 模块化（7 题）

### Q84
**问题**：我的 Tab 怎么依赖 protocol 包？
**答案**：在 Tab 模块的 `build.gradle.kts` 中添加 `implementation(project(":protocol"))`。tab 模块使用 `com.android.library` 插件（不是 application）。

### Q85
**问题**：Tab 做成独立模块还是直接放在 app 模块里？
**答案**：3 人以上团队推荐独立模块——编译隔离、减少 merge 冲突、可按模块分工。2 人以下小团队可以放在 app 模块内——配置简单。协议代码本身必须抽成独立 `:protocol` 模块供所有 Tab 依赖。

### Q86
**问题**：protocol 包的版本怎么同步？
**答案**：三处需保持一致：(1) `SemanticVersion.CURRENT` 常量 = `SemanticVersion(1, 0, 0)`；(2) protocol 模块 `build.gradle.kts` 中的 `versionName = "1.0.0"`；(3) 协议文档 changelog 中的版本号。建议在 CI 中加校验脚本，确保三处一致。

### Q87
**问题**：多个 Tab 各自是独立模块，能打包到同一个 APK 吗？
**答案**：能。App 主模块通过 `implementation(project(":tab-audit"))` 等依赖所有 Tab 模块，Gradle 会自动将所有模块打包进 APK。各 Tab 在初始化时向容器注册即可。

### Q88
**问题**：每个 Tab 模块需要自己的 Application 吗？
**答案**：不需要。只有主模块有 Application。Tab 模块是 `com.android.library`，不包含自己的 Application 类。入口是通过暴露一个注册函数给主模块调用。

### Q89
**问题**：proguard-rules.pro 需要什么特殊配置？
**答案**：必须保留协议接口和数据类：`-keep class github.leavesczy.compose_chat.protocol.** { *; }`。各 Tab 模块的 Composable 入口也要保留：`-keep class com.aioncall.tab.** { *; }`。否则 release 包中 Compose 函数被混淆后容器无法调用。

### Q90
**问题**：Tab 模块之间能相互依赖吗？
**答案**：不建议。Tab 之间应保持独立，通过容器或共享 ViewModel 通信。如果 TabA 依赖 TabB 的代码，违反了协议"各 Tab 独立"的原则，且会造成循环依赖风险。
