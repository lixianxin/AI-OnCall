---
topic: 常见接入错误与排查方法
keywords: [排查, 调试, 常见问题, Tab不显示, 兜底页面, TitleBar不生效, 权限申请, 超时]
related_questions:
  - "Tab 注册了为什么不显示？"
  - "切换到我的 Tab 显示空白怎么办？"
  - "TitleBar 右侧按钮为什么不生效？"
  - "权限申请一直失败怎么回事？"
  - "日志里搜什么关键字排查？"
  - "onCreate 里写网络请求有问题吗？"
---

# 常见错误排查

## 问题 1：Tab 注册后不显示

| 可能原因 | 排查方法 |
|---------|---------|
| id 与其他 Tab 重复 | 检查日志 `TabRegistry`，搜索关键字 `1001` |
| minContainerVersion 设置过高 | 确认当前容器版本 ≥ Tab 的 minContainerVersion |
| 必填字段未填或格式错误 | 检查 id（反向域名格式）、route（以 `/` 开头） |
| registerTab 未被调用 | 在注册代码处打断点，确认走到了 |

## 问题 2：切换到我的 Tab 显示兜底页面

| 可能原因 | 排查方法 |
|---------|---------|
| 生命周期回调超时（> 5 秒） | 检查 `onCreate` / `onResume` 是否有同步耗时操作。**网络请求、数据库操作必须异步** |
| 回调抛出异常 | 查看 logcat，搜索错误码 `1005`，检查堆栈 |

## 问题 3：TitleBar 右侧配置不生效

| 可能原因 | 排查方法 |
|---------|---------|
| 同时设置了 rightIcon 和 menuItems | 互斥规则：rightIcon 不为 null 时 menuItems 被忽略。**只设置其中一个** |
| extension 字段为 null | 检查 TabDefinition 的 extension 是否传入 |

## 问题 4：权限申请失败

| 可能原因 | 排查方法 |
|---------|---------|
| AndroidManifest 未声明 | 对照 permissions 列表，检查 Manifest 中是否声明了对应 `<uses-permission>` |
| 用户拒绝且不再询问 | 引导用户到系统设置中手动授权 |

## 通用排查技巧

- 搜索 Logcat 关键字：`TabRegistry`、`1001`~`1006`
- 在 TabLifecycle 每个回调中加 `Log.d`，确认回调调用顺序
- 检查 `registerTab` 的三个参数是否都传入且非 null

