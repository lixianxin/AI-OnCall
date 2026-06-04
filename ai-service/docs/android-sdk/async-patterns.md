---
topic: 异步编程模式
keywords: [协程, Coroutine, 异步, 网络请求, 数据库, 5秒超时, 1004, viewModelScope, Dispatchers, suspend]
related_questions:
  - "onCreate 里能做网络请求吗？"
  - "生命周期回调里怎么写异步代码？"
  - "为什么我的 onCreate 触发了 1004 超时？"
  - "协程应该用哪个 Dispatcher？"
  - "数据库查询在 onResume 里怎么写？"
  - "如何区分网络超时和回调超时？"
---

# 异步编程模式

## 核心原则

**生命周期回调（onCreate/onResume/onPause/onDestroy）不得包含超过 5 秒的同步阻塞操作。** 所有网络请求、数据库查询、文件 I/O 必须异步执行。

## 正确写法

```kotlin
val lifecycle = object : TabLifecycle {
    private lateinit var viewModel: MyViewModel

    override fun onCreate(bundle: Bundle?) {
        viewModel = ViewModelProvider(store)[MyViewModel::class.java]

        // onCreate 立即返回，不阻塞
    }

    override fun onResume() {
        viewModel.refreshData()
    }

    override fun onPause() = Unit
    override fun onDestroy() = Unit
}

class MyViewModel : ViewModel() {
    fun loadInitialData() {
        viewModelScope.launch {
            _state.value = LoadingState.Loading
            try {
                _data.value = repository.fetchFromNetwork()
                _state.value = LoadingState.Success
            } catch (e: Exception) {
                _state.value = LoadingState.Error(e.message ?: "unknown")
            }
        }
    }
}
```

## 错误写法（会导致 1004）

```kotlin
// 同步网络请求——1004 超时！
override fun onCreate(bundle: Bundle?) {
    val response = OkHttpClient().newCall(request).execute()  // 阻塞！
    data = response.body?.string()
}

// 同步数据库查询——可能触发 1004！
override fun onResume() {
    val list = database.dao().queryAll()  // 如果数据量大，阻塞主线程
    updateUI(list)
}

// runBlocking——和同步阻塞一样！
override fun onCreate(bundle: Bundle?) {
    runBlocking {
        val data = repository.fetchData()  // 看似协程，实际阻塞回调线程
    }
}
```

## 超时区分指南

| 错误类型 | 根因 | 错误码 | 排查 |
|---------|------|--------|------|
| 回调超时 | 生命周期回调内同步 IO | 1004 | 搜索 Logcat `1004` + 堆栈看是否在回调方法内 |
| 网络超时 | OkHttp timeout / 服务端不响应 | 无（业务层） | OkHttp 日志 `timeout` |
| 协程取消 | ViewModel 销毁，协程被取消 | 无（正常行为） | CancellationException，非错误 |

## Dispatchers 选择

```kotlin
class MyViewModel : ViewModel() {

    // 网络请求：viewModelScope 默认用 Dispatchers.Main + suspend 自动切换
    fun fetchData() {
        viewModelScope.launch {
            val data = apiService.getData()  // Retrofit suspend 自动后台
            _items.value = data              // 恢复 Main 线程更新 UI
        }
    }

    // 耗时计算：显式指定 Dispatchers.Default
    fun processLargeData() {
        viewModelScope.launch(Dispatchers.Default) {
            val result = heavyComputation(input)
            withContext(Dispatchers.Main) {
                _items.value = result
            }
        }
    }

    // 数据库：Room suspend 函数自动在后台线程执行
    fun queryDB() {
        viewModelScope.launch {
            val list = dao.getAll()  // Room suspend 自动后台
            _items.value = list
        }
    }
}
```

