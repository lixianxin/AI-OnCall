# DeepSeek API 调用失败

## 错误现象

服务端日志：
`
DeepSeek API error: status=XXX, body=...
`

## 常见错误码

| 状态码 | 含义 | 解决方案 |
|--------|------|---------|
| 401 | API Key 无效或过期 | 更新 application.yml 中的 deepseek.api-key |
| 402 | 账户余额不足 | 充值 |
| 429 | 请求频率超限 | 增加请求间隔，或升级套餐 |
| 500 | DeepSeek 服务端异常 | 等待后重试 |

## 排查步骤

1. 检查 application.yml 中 api-key 配置
2. 验证 API Key 是否有效：
   `ash
   curl https://api.deepseek.com/v1/models \
     -H ""Authorization: Bearer sk-xxxxxxxx""
   `
3. 检查账户余额

## AI OnCall 配置

`yaml
# /opt/ai-oncall/application.yml
deepseek:
  api-key: sk-207987ffc28548b497c903c5371ae8bd
  base-url: https://api.deepseek.com
  model: deepseek-chat
`

## 相关来源

api/ai-service-api.md
faq/how-to-deploy.md