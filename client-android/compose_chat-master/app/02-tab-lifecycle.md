---
topic: TabLifecycle 生命周期回调与调度规则
keywords: [TabLifecycle, 生命周期, onCreate, onResume, onPause, onDestroy, onConfigChange, 状态机, 超时保护, 异常保护]
related_questions:
  - "Tab 生命周期有哪些回调？"
  - "onCreate 和 onResume 的调用顺序是什么？"
  - "Tab 切换时生命周期怎么走？"
  - "生命周期回调超时了会怎样？"
  - "onConfigChange 是必须实现的吗？"
  - "onDestroy 之后还能收到回调吗？"
  - "每个回调最多执行多久？"
  - "回调抛异常会影响其他 Tab 吗？"
---

# Tab 生命周期

## 回调接口

```kotlin
interface TabLifecycle {
    fun onCreate(bundle: Bundle?)                    // Tab 首次创建
    fun onResume()                                   // Tab 可见（被切换到）
    fun onPause()                                    // Tab 不可见（被切换走）
    fun onDestroy()                                  // Tab 被销毁
    fun onConfigChange(config: Configuration) = {}   // 配置变更（横竖屏等），可选实现
}
```

## 状态机

```
Tab 创建
  │
  ▼
onCreate ──→ onResume
                │
   ◄────────────┘ (Tab 切换)
                │
                ▼
            onPause
                │
          ┌─────┴─────┐
          ▼           ▼
      onResume    onDestroy
      (切换回来)    (移除 Tab)
```

## 调度规则（5条）

1. **首次进入**：onCreate → onResume
2. **Tab 间切换**：当前 Tab.onPause → 新 Tab.onResume（若新 Tab 首次创建，则先 onCreate 再 onResume）
3. **退出/移除 Tab**：onPause → onDestroy
4. **超时保护**：每个回调执行上限 **5 秒**，超时容器自动降级——显示兜底页面
5. **异常保护**：回调抛异常不影响其他 Tab，当前 Tab 显示兜底页面并打印日志

## 最佳实践

- `onCreate`：加载数据、注册监听、初始化资源
- `onResume`：刷新 UI 状态、恢复动画。**避免重操作**（可能被频繁调用）
- `onPause`：停止动画、释放临时资源、保存草稿
- `onDestroy`：取消注册、关闭连接、释放全部资源。**此回调后容器不会再调该 Tab 的任何方法**
- `onConfigChange`：可选，处理横竖屏切换、深色模式等配置变更
