---
topic: 版本兼容策略与废弃流程
keywords: [SemanticVersion, 版本兼容, major, minor, patch, 向后兼容, 废弃, Deprecated, minContainerVersion, 兼容矩阵]
related_questions:
  - "版本号 major、minor、patch 分别代表什么？"
  - "什么情况该升级 major 版本？"
  - "老容器遇到新版本 Tab 怎么处理？"
  - "怎么废弃一个字段？"
  - "向后兼容是什么意思？"
  - "minContainerVersion 怎么用？"
  - "容器版本太低会怎样？"
---

# 版本兼容策略

## SemanticVersion 语义

```
major.minor.patch
```

| 版本位 | 含义 | 升级时机 |
|--------|------|---------|
| major | 不兼容变更 | 删除必填字段、改了回调签名、改变路由规则 |
| minor | 向后兼容新增 | 新增可选字段、新增回调方法、新增扩展点 |
| patch | 无行为变化 | 文档修正、Bug 修复、文案调整 |

## 向后兼容矩阵

| 场景 | 容器行为 |
|------|---------|
| 容器版本 < Tab.minContainerVersion | **拒绝加载**，Toast 提示"请升级客户端" |
| Tab 发送容器不认识的字段 | 容器**忽略**多余字段 |
| 容器新增字段，Tab 未提供 | 容器**使用默认值** |
| Tab 调用不存在的扩展点 | 容器**静默忽略** |

## 版本号管理建议

```
开发阶段（内部迭代）: 0.x.0 → 频繁变更，不做兼容承诺
正式发布（对外）:    1.0.0 → 遵守向后兼容承诺
```

## 废弃流程

```
v1.x 标记 @Deprecated → v2.0 正式移除
```

具体做法：
1. 确定要废弃的字段/方法
2. 在当前版本标记 `@Deprecated`，注释说明替代方案
3. 文档中标注"将在 v2.0 移除"
4. 下一个 major 版本正式删除

## minContainerVersion 用法

```kotlin
TabDefinition(
    id = "com.example.audit",
    // ...
    minContainerVersion = 2  // 低于容器 v2 不加载此 Tab
)
```

当容器版本为 1 时，此 Tab 被拒绝加载，提示用户升级。

