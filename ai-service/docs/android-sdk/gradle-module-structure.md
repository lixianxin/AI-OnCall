---
topic: Gradle 模块化接入
keywords: [Gradle, 模块化, 依赖, protocol包, 版本同步, 编译隔离, proguard]
related_questions:
  - "我的 Tab 怎么依赖 protocol 包？"
  - "Tab 做成独立模块还是放在 app 模块里？"
  - "protocol 包的版本怎么同步？"
  - "多个 Tab 各自是独立模块，能一起打包吗？"
  - "ProGuard 需要什么特殊配置吗？"
---

# Gradle 模块化接入

## 模块依赖层次

```
app (主模块)
├── protocol (协议定义，所有 Tab 模块都依赖它)
│   └── TabDefinition.kt, TabLifecycle.kt, TabExtension.kt ...
├── tab-audit (内容审核 Tab)
│   └── dependency: protocol
├── tab-dashboard (数据看板 Tab)
│   └── dependency: protocol
└── tab-groupchat (群聊 Tab)
    └── dependency: protocol
```

## protocol 模块 build.gradle.kts

```kotlin
// protocol/build.gradle.kts
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "github.leavesczy.compose_chat.protocol"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.material3:material3")
}
```

## Tab 模块 build.gradle.kts

```kotlin
// tab-audit/build.gradle.kts
plugins {
    id("com.android.library")       // 用 library（非 application）
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.aioncall.tab.audit"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
    buildFeatures { compose = true }
}

dependencies {
    // 协议依赖
    implementation(project(":protocol"))

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
}
```

## App 主模块集成

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":protocol"))
    implementation(project(":tab-audit"))
    implementation(project(":tab-dashboard"))
    // ...
}
```

App 模块在初始化时遍历所有 Tab 模块的注册入口：

```kotlin
// App 初始化
fun initContainer() {
    val container = TabContainerImpl()

    // 各 Tab 模块暴露的注册函数
    ContentAuditTab.register(container)
    DashboardTab.register(container)
}
```

## 模块 vs 直接放在 app 里

| 维度 | 独立模块 | 放在 app 里 |
|------|---------|------------|
| 编译速度 | 只按需编译变更模块 | 全量编译 |
| 团队协作 | 不同人负责不同模块，不冲突 | 容易 merge 冲突 |
| 版本隔离 | 可以独立发版（maven/aar） | 绑定 app 版本 |
| 复杂度 | 需管理模块间依赖 | 简单 |
| 建议 | **3 人以上团队推荐** | 2 人以下团队可用 |

## ProGuard / R8 配置

```proguard
# protocol/proguard-rules.pro

# 保留协议接口和数据类
-keep class github.leavesczy.compose_chat.protocol.** { *; }

# 保留 Compose 相关
-keep class androidx.compose.** { *; }

# 各 Tab 模块也需要保留 Composable 入口
-keep class com.aioncall.tab.** { *; }
```

在主模块的 `proguard-rules.pro` 中引用：

```proguard
# 收集所有模块的 proguard 规则
-include ../protocol/proguard-rules.pro
-include ../tab-audit/proguard-rules.pro
```

## 版本同步策略

protocol 模块的版本号应与协议文档版本号一致：

```kotlin
// protocol/build.gradle.kts
android {
    defaultConfig {
        versionName = "1.0.0"  // 与 SemanticVersion(1,0,0) 保持一致
    }
}

// 同时 TabDefinition.kt 中
val CURRENT = SemanticVersion(1, 0, 0)
```

当协议版本升级（如 v1.1.0），需同步更新：
1. `SemanticVersion.CURRENT`
2. protocol 模块 `versionName`
3. 协议文档 changelog

