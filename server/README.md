# OpenTab Server

OpenTab 服务端使用 Go + Gin 实现，负责账号登录、Bearer Token 鉴权、Tab 动态下发、团队与权限管理、审批/日程/公告业务接口、AI OnCall 会话管理和 SSE 流式转接。

当前云服务器公网 IP：

```text
121.40.241.161
```

默认服务地址：

```text
http://121.40.241.161:8080
```

AI 服务地址：

```text
http://121.40.241.161:8081
```

## 技术栈

- Go + Gin
- PostgreSQL
- Redis
- GORM
- SSE

## 目录结构

```text
cmd/server              服务入口
internal/config         环境变量配置
internal/database       PostgreSQL 连接、表结构和种子数据
internal/middleware     鉴权、request_id、审计日志
internal/models         请求/响应/业务模型
internal/policies       权限判断策略
internal/repositories   Memory/PostgreSQL 数据访问实现
internal/routes         HTTP 路由
internal/services       业务逻辑
internal/cache          Redis 鉴权缓存
scripts                 部署、数据库初始化和演示数据脚本
```

## 本地运行

进入 `server` 目录：

```bash
go mod tidy
go run ./cmd/server
```

默认监听：

```text
http://127.0.0.1:8080
```

Android 模拟器访问本机服务：

```text
http://10.0.2.2:8080
```

## PostgreSQL 模式运行

示例环境变量：

```bash
APP_MODE=postgres \
DATABASE_URL="postgres://opentab:opentab123@localhost:5432/opentab?sslmode=disable" \
REDIS_URL="redis://localhost:6379/0" \
AI_SERVICE_BASE_URL="http://121.40.241.161:8081" \
HOST=0.0.0.0 \
PORT=8080 \
go run ./cmd/server
```

Windows PowerShell 示例：

```powershell
$env:APP_MODE="postgres"
$env:DATABASE_URL="postgres://opentab:opentab123@localhost:5432/opentab?sslmode=disable"
$env:REDIS_URL="redis://localhost:6379/0"
$env:AI_SERVICE_BASE_URL="http://121.40.241.161:8081"
$env:HOST="0.0.0.0"
$env:PORT="8080"
go run ./cmd/server
```

## 主要环境变量

| 变量 | 说明 | 默认值 |
|---|---|---|
| `APP_MODE` | 运行模式，`mock` 或 `postgres` | `mock` |
| `DATABASE_URL` | PostgreSQL 连接串 | 空 |
| `REDIS_URL` | Redis 连接串 | 空 |
| `AI_SERVICE_BASE_URL` | AI OnCall 服务地址 | `http://121.40.241.161:8081` |
| `HOST` | 服务监听地址 | `0.0.0.0` |
| `PORT` | 服务端口 | `8080` |
| `AUTH_USER_CONTEXT_TTL_SECONDS` | Redis 用户上下文缓存时间 | `300` |

## 数据库初始化

Linux 服务器可使用：

```bash
./scripts/init_postgres_linux.sh reset
```

该脚本会创建/重建 `opentab` 数据库和账号。服务启动后会通过 GORM 自动创建表结构和默认数据。

演示数据重置：

```bash
./scripts/reset_demo_data.sh
```

## 云服务器部署

在云服务器 `server` 目录下运行：

```bash
APP_MODE=postgres \
DATABASE_URL="postgres://opentab:opentab123@localhost:5432/opentab?sslmode=disable" \
REDIS_URL="redis://localhost:6379/0" \
AI_SERVICE_BASE_URL="http://121.40.241.161:8081" \
HOST=0.0.0.0 \
PORT=8080 \
./scripts/deploy_restart.sh
```

健康检查：

```bash
curl http://127.0.0.1:8080/health
```

公网检查：

```bash
curl http://121.40.241.161:8080/health
```

查看日志：

```bash
tail -n 100 server.out.log
tail -n 100 server.err.log
```

## 测试

```bash
go test ./...
```

## 演示账号

| 账号 | 密码 | 说明 |
|---|---|---|
| `opentab-admin` | `admin123` | 管理员账号 |
| `opentab-demo` | `demo123` | 普通演示账号 |
| `opentab-guest` | `guest123` | 访客演示账号 |

实际演示数据以数据库初始化脚本和种子数据为准。

## 主要接口

### 公开接口

```text
GET  /health
POST /auth/login
POST /auth/register
```

### 登录态接口

```text
POST /auth/logout
GET  /me
GET  /debug/status
```

### Tab 接口

```text
GET    /tabs
GET    /tabs/catalog
GET    /tabs/{tabId}
POST   /me/tabs
DELETE /me/tabs/{tabId}
PUT    /me/tabs/order
POST   /tabs
PUT    /tabs/{tabId}
DELETE /tabs/{tabId}
POST   /tabs/validate
POST   /tabs/{tabId}/actions/{actionId}
```

### 业务接口

```text
GET    /business/approval/summary
GET    /business/approval/items
GET    /business/approval/items/{itemId}
POST   /business/approval/items
POST   /business/approval/items/{itemId}/approve
POST   /business/approval/items/{itemId}/reject
POST   /business/approval/items/{itemId}/cancel

GET    /business/calendar/summary
GET    /business/calendar/events
GET    /business/calendar/events/{eventId}
POST   /business/calendar/events
PUT    /business/calendar/events/{eventId}
DELETE /business/calendar/events/{eventId}

GET    /business/announcements
GET    /business/announcements/{announcementId}
POST   /business/announcements
PUT    /business/announcements/{announcementId}
DELETE /business/announcements/{announcementId}
```

### 管理接口

```text
GET    /admin/teams
POST   /admin/teams
PUT    /admin/teams/{teamId}
DELETE /admin/teams/{teamId}
GET    /admin/teams/{teamId}/members
POST   /admin/teams/{teamId}/members
PUT    /admin/teams/{teamId}/members/{userId}
DELETE /admin/teams/{teamId}/members/{userId}
GET    /admin/users
GET    /admin/users/{userId}
PUT    /admin/users/{userId}/global-role
```

### AI OnCall 接口

```text
POST   /api/chat/stream
GET    /oncall/stream
POST   /oncall/sessions
GET    /oncall/sessions
DELETE /oncall/sessions/{sessionId}
POST   /oncall/sessions/{sessionId}/messages
GET    /oncall/sessions/{sessionId}/messages
GET    /oncall/sessions/{sessionId}/stream
POST   /oncall/sessions/{sessionId}/cancel
```

需要登录的接口统一使用：

```http
Authorization: Bearer <token>
```
