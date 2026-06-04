---
topic: ViewModel 与 Tab 生命周期协作
keywords: [ViewModel, ViewModelProvider, 生命周期, onCreate, onDestroy, ViewModelScope, 状态管理, 内存泄漏]
related_questions:
  - "ViewModel 应该在哪个回调里创建？"
  - "Tab 销毁时 ViewModel 会自动清理吗？"
  - "ViewModel 的 viewModelScope 和生命周期是什么关系？"
  - "多个 Tab 能共享一个 ViewModel 吗？"
  - "onResume 里应该从 ViewModel 加载数据还是网络请求？"
  - "Tab 切走再切回来，ViewModel 还在吗？"
---

# ViewModel 与 Tab 生命周期协作

## 推荐架构

每个 Tab 应有自己的 ViewModel，在 `onCreate` 中创建或获取：

```kotlin
class ContentAuditTab(
    private val viewModelStoreOwner: ViewModelStoreOwner  // 容器传入
) {
    val lifecycle = object : TabLifecycle {
        private lateinit var viewModel: ContentAuditViewModel

        override fun onCreate(bundle: Bundle?) {
            // 推荐：在 onCreate 中初始化 ViewModel
            viewModel = ViewModelProvider(viewModelStoreOwner)[ContentAuditViewModel::class.java]
            viewModel.loadData()
        }

        override fun onResume() {
            // 只刷新，不重新创建
            viewModel.refresh()
        }

        override fun onPause() = Unit

        override fun onDestroy() {
            // ViewModel 由 ViewModelStoreOwner 管理生命周期
            // 此处只需清理 ViewModel 无关的资源
        }
    }
}
```

## ViewModel 与 Tab 生命周期对照

| Tab 生命周期 | ViewModel 状态 | 说明 |
|-------------|---------------|------|
| onCreate | ViewModel 创建 | 首次调用 `ViewModelProvider(...).get()` 时创建 |
| onResume | 活跃 | ViewModel 数据仍在，可刷新 UI |
| onPause | 内存中 | ViewModel 不被销毁，数据保持 |
| onDestroy | 仍存活 | Tab.onDestroy ≠ ViewModel 销毁。ViewModel 由 ViewModelStoreOwner 控制 |
| ViewModelStoreOwner 销毁 | onCleared() | ViewModel.onCleared() 被调用，释放协程等资源 |

## ViewModel 协程作用域

```kotlin
class ContentAuditViewModel : ViewModel() {
    // ✅ 使用 viewModelScope 发起协程
    fun loadData() {
        viewModelScope.launch {
            val data = repository.fetchAuditList()
            _items.value = data
        }
    }

    // ❌ 不要在 ViewModel 里用 GlobalScope
    // fun loadData() {
    //     GlobalScope.launch { ... }  // 泄漏！
    // }
}
```

`viewModelScope` 会在 ViewModel.onCleared() 时自动取消所有协程，无需手动管理。

## 多 Tab 共享 ViewModel

```kotlin
// 场景：两个 Tab 共享同一批数据
class SharedViewModel : ViewModel() {
    val commonData = MutableStateFlow<List<Item>>(emptyList())
}

// 两个 Tab 使用同一个 ViewModelStoreOwner
val sharedVM = ViewModelProvider(viewModelStoreOwner)[SharedViewModel::class.java]
```

v1.0 不提供 Tab 间通信机制，但可以通过共享 ViewModel 实现数据共享（需要容器传入同一个 ViewModelStoreOwner）。

## onResume 数据刷新策略

```
场景：用户从 TabA 切到 TabB，再切回 TabA

推荐做法（轻量刷新）：
  onResume → viewModel.refresh()  // 只拉增量/检查是否有更新

不推荐（每次都全量重拉）：
  onResume → viewModel.loadAllData()  // 重拉全部数据，浪费流量
```

## 异常保护

ViewModel 的协程异常应在内部捕获，不要让未捕获异常传播到生命周期回调：

```kotlin
fun refresh() {
    viewModelScope.launch {
        try {
            _items.value = repository.fetchAuditList()
        } catch (e: Exception) {
            _error.value = "刷新失败: ${e.message}"
            // 不抛到 TabLifecycle，避免触发错误码 1005
        }
    }
}
```

