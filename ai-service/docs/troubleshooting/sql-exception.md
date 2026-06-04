# SQLException

## 错误特征

SQLException、SQL Error、DataIntegrityViolationException、
数据库、ConstraintViolation、Deadlock、Table not found、
Column not found、Duplicate entry、sql、jdbc、
ORA-、MySQLSyntaxErrorException

## 根因

数据库操作失败。

常见原因：
- SQL 语法错误
- 表或字段不存在
- 唯一约束冲突（Duplicate entry）
- 死锁（Deadlock found）
- 连接池耗尽（Connection is not available）
- 主键冲突
- 外键约束失败

## 排查步骤

1. 查看完整异常信息：获取 SQL 语句和错误码
2. 确认表结构和字段是否存在
3. 检查是否违反唯一约束或外键约束
4. 查看数据库连接池状态
5. 检查慢查询日志

## 解决方案

```java
// 方案1：捕获并转换异常
try {
    userRepository.save(user);
} catch (DataIntegrityViolationException e) {
    throw new BusinessException("用户已存在");
}

// 方案2：事务重试
@Retryable(value = DeadlockLoserDataAccessException.class, maxAttempts = 3)
@Transactional
public void updateUser(User user) {
    userRepository.save(user);
}

// 方案3：连接池配置
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

## 相关来源

protocol > tab-register.md