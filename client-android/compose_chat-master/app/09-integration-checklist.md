---
topic: 接入方自查清单
keywords: [对接清单, 检查清单, 接入验收, 上线前检查]
related_questions:
  - "接入完成前要检查哪些项？"
  - "怎么确认我的 Tab 接入正确？"
  - "上线前有什么注意事项？"
  - "Composable 页面有什么限制？"
---

# 接入方自查清单

接入方完成开发后，逐项自查：

## 元信息
- [ ] TabDefinition 所有必填字段已填写（id / displayName / icon / route / version）
- [ ] id 使用反向域名格式，全局唯一
- [ ] displayName ≤ 16 字符
- [ ] route 以 `/` 开头，全局唯一
- [ ] version 使用 SemanticVersion(major, minor, patch)

## 生命周期
- [ ] TabLifecycle 四个回调全部实现（onCreate / onResume / onPause / onDestroy）
- [ ] onResume 不做超过 5 秒的耗时操作
- [ ] onDestroy 正确释放了全部资源
- [ ] 每个回调有基本的异常保护（try-catch 包裹关键逻辑）

## 页面 Composable
- [ ] Composable 页面是主内容，**不包含自己的 Scaffold / TopBar / BottomBar**（容器已提供）
- [ ] 不要在 Composable 里设置 WindowInsets（容器已处理）
- [ ] 页面能在不同屏幕尺寸下正常展示

## 扩展点
- [ ] 如使用 TitleBar 扩展，rightIcon 和 menuItems 不同时设置（互斥规则）
- [ ] 扩展点 callback 中不做耗时操作

## 权限
- [ ] 如声明 permissions，AndroidManifest.xml 中对应 `<uses-permission>` 已添加

## 测试
- [ ] Tab 切换正常（从其他 Tab 切到你的 Tab，再切回去）
- [ ] 横竖屏切换不崩溃
- [ ] 杀进程重进，Tab 能正常恢复
- [ ] 至少在一台真机上验证通过

