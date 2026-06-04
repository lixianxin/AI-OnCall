---
topic: 端到端调试指南
keywords: [调试, Logcat, 断点, 排查树, 日志关键字, 启动顺序, 常见踩坑]
related_questions:
  - "Tab 不显示怎么系统性排查？"
  - "Logcat 搜什么关键字？"
  - "在哪打断点能最快定位问题？"
  - "为什么 registerTab 调了但没效果？"
  - "服务端配置和本地配置冲突了怎么办？"
---

# 端到端调试指南

## 排查决策树

```
Tab 不显示
  |
  +-- BottomBar 上没有图标？
  |     |
  |     +-- registerTab 被调用了没有？
  |     |     打断点在 registerTab 方法入口
  |     |     没走到 → 检查调用时机（是否在 Application.onCreate 之后？）
  |     |
  |     +-- 是否返回了 RegistrationResult.Failed？
  |     |     看返回值 → errorCode 是多少？
  |     |       1001 → id 重复
  |     |       1002 → 容器版本太低
  |     |       1003 → 必填字段缺失
  |     |       1006 → 权限不足
  |     |
  |     +-- 服务端 enabled=false？
  |           抓包看 GET /api/v1/tabs 返回的 enabled 字段
  |           或看客户端日志"Tab enabled=false, hiding: xxx"
  |
  +-- BottomBar 有图标，但点进去是空白/兜底页面？
        |
        +-- 报 1004 超时？
        |     Logcat 搜索 "LIFECYCLE_TIMEOUT"
        |     检查 onCreate/onResume 是否有同步 IO
        |
        +-- 报 1005 异常？
        |     Logcat 搜索 "LIFECYCLE_EXCEPTION"
        |     看堆栈定位崩溃点
        |
        +-- 白屏但不报错？
              Composable 返回了 nothing？
              检查 page lambda 是否正确传递
```

## Logcat 关键字速查

| 关键字 | 含义 |
|--------|------|
| `TabRegistry` | Tab 注册/注销日志 |
| `registerTab` | 注册事件 |
| `switchTab` | Tab 切换事件 |
| `onCreate` / `onResume` / `onPause` / `onDestroy` | 生命周期回调 |
| `1001` ~ `1006` | 协议错误码 |
| `LIFECYCLE_TIMEOUT` | 回调超时 |
| `LIFECYCLE_EXCEPTION` | 回调异常 |
| `Tab enabled=false` | 服务端控制禁用 |

## 建议埋点位置

容器开发者在以下位置加日志，方便团队联调：

```kotlin
class TabContainerImpl : ContainerInterface {

    fun registerTab(def: TabDefinition, lifecycle: TabLifecycle, page: ...): RegistrationResult {
        Log.d("TabRegistry", "registerTab called: id=${def.id}, route=${def.route}")

        if (registry.containsKey(def.id)) {
            Log.w("TabRegistry", "1001 DUPLICATE: ${def.id}")
            return RegistrationResult.Failed(TabErrors.DUPLICATE_ID)
        }

        // ... 校验 ...

        Log.i("TabRegistry", "Tab registered: ${def.fullId}, " +
                "total tabs: ${registry.size + 1}")
        return RegistrationResult.Success
    }

    fun switchTab(route: String): Boolean {
        val target = findByRoute(route)
        Log.i("TabRegistry", "switchTab: ${current?.route} -> ${target?.route}")

        try {
            lifecycleDispatcher.onPause(current)
            if (target.isFirstTime) lifecycleDispatcher.onCreate(target, bundle)
            lifecycleDispatcher.onResume(target)
        } catch (e: TimeoutException) {
            Log.e("TabRegistry", "1004 TIMEOUT: ${target.id}", e)
        } catch (e: Exception) {
            Log.e("TabRegistry", "1005 EXCEPTION: ${target.id}", e)
        }
    }
}
```

## 常见踩坑案例

### 案例 1：registerTab 调了但不生效

```
原因：registerTab 在 Application.onCreate 之前调用，容器未初始化
解决：确保容器初始化之后再注册，或使用懒注册（容器就绪后自动注册）
```

### 案例 2：服务端 enabled=false，本地还是显示了

```
原因：客户端缓存未过期，用的是旧配置
解决：清缓存重试；或让容器在 Tab 渲染前强制检查一次服务端配置
```

### 案例 3：debug 模式正常，release 包 Tab 消失

```
原因：ProGuard/R8 混淆了反射调用，Tab 的 Page Composable 被优化掉
解决：在 proguard-rules.pro 添加 keep 规则：
      -keep class **.protocol.** { *; }
      -keep class **.TabDefinition { *; }
      -keep class **.TabLifecycle { *; }
```

### 案例 4：切了几个 Tab 后 OOM

```
原因：Tab 切换时没有释放资源，旧 Tab 的 Bitmap/List 没有清理
解决：在 onPause 中清理大对象（图片缓存、大数据列表），onResume 重建
```

### 案例 5：生命周期回调没按预期顺序执行

```
现象：期望 onCreate → onResume，实际只有 onResume
原因：Tab 已创建过（onCreate 已调用），当前是恢复
解决：这是正常行为。首次 = onCreate + onResume，非首次 = 仅 onResume
```

