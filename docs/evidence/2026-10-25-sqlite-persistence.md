# P2-1 ConversationStore SQLite 持久化验证报告（2026-10-25）

> 目标：解决审计发现的真实缺陷「会话状态全内存，重启即失」（代码注释自述"后续可替换为 Redis"）。
> 语义边界（先说清楚这项工作证明什么）：**只证持久化**——重启后会话历史/摘要/Agent 工作记忆/已处理消息标识可恢复。
> **不承诺幂等**（外部工具防重复执行需任务级设计）、**不是 Checkpoint**（无执行级状态恢复）。

## 设计

- **内存为主读路径 + SQLite 写透**：读取行为与旧内存版完全一致（ConcurrentHashMap），`save`/`markProcessed`/`remove` 同步落库，启动时全量加载。
- **表结构**：`conversations`（元数据 + 摘要 + 最近工具/来源 JSON）、`messages`（conv_id, seq, role, content, ts, type）、`processed_messages`（去重标识）。
- **故障语义（故意设计）**：SQLite 任何读写失败**降级为纯内存行为**（ERROR 日志 + 继续服务）——持久化是增强不是依赖，存储故障不阻塞对话主链路。启动失败同样降级。
- **依赖**：`org.xerial:sqlite-jdbc:3.53.4.0`（版本取自阿里云仓库元数据最新 release，非凭记忆）。单连接 + 锁串行写 + WAL，符合单实例低流量场景（多实例才需要换 Redis/PG——当前无此需求，不为简历词引组件）。
- 配置：`oncall.db-path`（默认 `./data/aioncall.db`，已加 .gitignore）。

## 验证结果（重启恢复三验收，全部通过）

| 验收项 | 方法 | 结果 |
|---|---|---|
| 写入落库 | 发送 1 条消息后直接查库 | ✅ conversations=1 / messages=2（user+assistant）/ processed=1 |
| 已处理消息标识跨重启 | 杀进程 → 重启 → 重放同一 messageId | ✅ 返回 `DUPLICATE_MESSAGE`（启动日志：`恢复会话 1 个, 已处理消息 1 条`） |
| 会话历史跨重启 | 重启后发新消息，检查 LLM 请求体日志 | ✅ prompt 的【最近对话】包含重启前消息 `persistence-marker-消息一` |
| Agent 工作记忆跨重启 | 同上（recentTools/summary 恢复） | ✅ 随会话元数据一并恢复（机制相同，记录于启动日志计数） |

原始证据：`p21_run1.sse`（首跑成功）、重启后 m2 请求的 SSE（`p21_run2_m2.sse`）、应用日志（prompt 含重启前历史）。

## 过程中发现并修复的缺陷（本任务自己的"发现→修复→回归"）

1. **JDBC 批处理静默空操作**：首次实现循环绑定参数后直接 `executeBatch()`，漏了 `addBatch()`——
   结果是**无报错、无异常、事务正常提交，但 0 行写入**（同一事务里 conversations 的
   `executeUpdate` 正常，造成"元数据有、消息无"的迷惑状态）。靠直接查库发现。
   - 教训：executeBatch 不等于"把绑定的参数批量执行"；这类静默失败只有对账（写后读）能抓住。
2. 修复后重跑完整重启恢复序列，三验收全过（上表）。

## 兼容性与回归

- 路由链路回归：diagnosis_v2 全量 200 条（mock，throttle 2.5s）——
  Intent / Tool / No-Tool / Fallback 预期与基线逐位一致（写库在请求路径上但失败不阻塞），
  结果见 `eval/results/p2_sqlite_regression_20261025.json.json`。
- 原 JSON `/metrics`、SSE 协议、评测 harness 均未改动。

## 遗留

- [ ] 幂等/Checkpoint 明确不做（见语义边界）；如未来做多实例，迁移到 PG/Redis 并引入真正的任务级恢复设计。
- [ ] Docker 镜像的 data 卷声明（`VOLUME /app/data`）随云端部署一起加。
