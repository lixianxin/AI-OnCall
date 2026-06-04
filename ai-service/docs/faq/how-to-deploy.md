# 如何部署 AI OnCall

## 构建

`ash
cd ai-service
./gradlew bootJar
# 产物: build/libs/ai-service-0.0.1-SNAPSHOT.jar（约 23MB）
`

## 上传到服务器

服务器信息：
- 地址：121.40.241.161
- 用户：root
- 密码：123123
- 部署路径：/opt/ai-oncall/

`ash
# 上传 jar
scp ai-service.jar root@121.40.241.161:/opt/ai-oncall/

# 上传知识库
scp -r docs/* root@121.40.241.161:/opt/ai-oncall/docs/
`

## 启动

`ash
ssh root@121.40.241.161

cd /opt/ai-oncall

# 停旧
pkill -f ai-service

# 启新
nohup java -DONCALL_DOCS_PATH=/opt/ai-oncall/docs -jar ai-service.jar > app.log 2>&1 &

# 检查
sleep 5
tail -20 app.log
# 确认看到: DocumentStore ready: N chunks from M files
`

## 验证

`ash
curl -X POST http://localhost:8081/api/chat/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"message":"如何注册Tab","conversationId":"test"}'
`

## 日志位置

| 日志 | 路径 | 查看方式 |
|------|------|---------|
| 应用日志 | /opt/ai-oncall/app.log | tail -f app.log |
| 构建日志 | ai-service/stdout.log | 本地查看 |
| stderr | ai-service/stderr.log | 本地查看 |

## 相关来源

architecture/system-overview.md
api/ai-service-api.md