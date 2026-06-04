# 回答格式规范

AI OnCall 回答必须使用以下 Markdown 格式输出，以在 Android 端获得最佳显示效果。

## 表格格式

涉及字段对比、配置说明、错误码等结构化信息时，必须使用 Markdown 表格：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | string | 是 | Tab 唯一标识 |
| displayName | string | 是 | 展示名称 |
| route | string | 是 | 路由路径 |

## 代码块格式

代码使用 ` 代码块，并标注语言：

`java
@RestController
public class ChatController {
    @PostMapping("/api/chat/stream")
    public Flux<ServerSentEvent<String>> stream() { }
}
`

`kotlin
// Android Kotlin
class OpenOnCallRepository(apiClient: OnCallApiClient)
`

`xml
<!-- XML 配置 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
`

## 命令行格式

命令使用 `ash：

`ash
# 构建
./gradlew bootJar

# 启动
nohup java -jar ai-service.jar > app.log 2>&1 &
`

## 路径和文件名

文件路径使用 反引号：

i-service/src/main/java/com/oncall/ai/controller/ChatController.java

## 列表格式

### 无序列表

- 使用 - 开头
- 同级缩进对齐
- 每行不超过 40 字

### 有序列表

1. 第一步：实现接口
2. 第二步：声明元信息
3. 第三步：注册 Tab

## 引用格式

引用知识库文档时使用 >：

> 来源：protocol/tab-register.md

## 层级标题

## 一级标题（大章节）
### 二级标题（子章节）
#### 三级标题（详细说明）

## 综合示例

当回答包含多种内容时：

### TabManifest 必填字段

| 字段 | 说明 |
|------|------|
| id | 唯一标识 |
| displayName | 展示名称 |

### 示例代码

`kotlin
val manifest = TabManifest(
    id = "approval",
    displayName = "审批中心"
)
`

### 操作步骤

1. 在服务端添加 TabManifest 配置
2. 在 Android 端注册路由
3. 重启应用

> 来源：protocol/tab-manifest.md

## 相关来源

mock-server-api.md > 回答格式规范