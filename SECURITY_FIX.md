# 安全问题分析与修复报告

## 问题概述
发现数据库中存在包含 SQL 注入特征的用户数据，openid 包含以下恶意内容：
- `test_0b1kLSGa1FqSUK0whjHa1vVQWv0kLSGo' union select 1--`
- `test_")) OR (SELECT*FROM(SELECT(SLEEP(3)))ppam) limit 1#`

## 根本原因

### 1. 测试代码未移除
在 `WxAppController.java` 的以下接口中存在测试代码：
```java
String openid = getOpenidByCode(request.getCode());
if (openid == null || openid.isEmpty()) {
    // ❌ 危险：直接拼接用户输入
    openid = "test_" + request.getCode();
}
```

攻击者可以构造恶意的 `code` 参数，例如：
```
POST /wxapp/user/silent-register
{
    "code": "xxx' union select 1--",
    "inviteCode": ""
}
```

生成的 openid 将是：`test_xxx' union select 1--`

### 2. 实际 SQL 注入是否成功？

**好消息：SQL 注入攻击实际上失败了！**

因为使用了 MyBatis-Plus 的参数化查询：
```java
@Select("SELECT * FROM ums_user WHERE openid = #{openid} LIMIT 1")
UmsUser selectByOpenid(@Param("openid") String openid);
```

`#{openid}` 会被转换为预编译语句的参数，恶意代码会被当作字符串处理，不会执行。

但是，这些恶意字符串仍然作为普通数据被插入到数据库中，造成脏数据。

## 已实施的修复措施

### 1. 移除危险的测试代码
在 3 个接口中移除了 `openid = "test_" + request.getCode()` 逻辑：
- `/wxapp/user/login`
- `/wxapp/user/silent-register`
- `/wxapp/user/update-info`

现在如果获取 openid 失败，直接返回错误而不是使用假数据。

### 2. 添加参数验证
```java
// 验证 code 参数
if (request.getCode() == null || request.getCode().trim().isEmpty()) {
    return DefaultResponse.error("code 参数不能为空");
}

// 验证 openId 格式
if (!request.getOpenId().matches("^[a-zA-Z0-9_-]+$")) {
    return DefaultResponse.error("openId 格式不正确");
}
```

## 数据清理步骤

1. **备份数据库**（必须先执行）
   ```bash
   mysqldump -u root -p med_base > backup_$(date +%Y%m%d).sql
   ```

2. **执行清理脚本**
   使用提供的 `sql/cleanup_malicious_users.sql` 文件：
   - 先执行查询语句，确认要删除的数据
   - 确认无误后，取消注释 DELETE 语句并执行

3. **验证清理结果**
   ```sql
   SELECT COUNT(*) FROM ums_user WHERE openid LIKE '%union%';
   -- 应该返回 0
   ```

## 安全建议

### 短期措施（必须立即执行）
- [x] 移除测试代码
- [x] 添加参数验证
- [ ] 清理恶意数据
- [ ] 重新部署后端服务

### 长期措施
1. **代码审查**
   - 所有用户输入必须验证
   - 测试代码不应出现在生产环境
   - 使用代码审查工具（如 SonarQube）

2. **API 安全**
   - 添加请求频率限制（Rate Limiting）
   - 实施 IP 白名单（生产环境）
   - 添加请求日志和监控

3. **数据库安全**
   - 使用只读账号进行查询
   - 定期备份数据库
   - 监控异常 SQL 语句

4. **小程序端**
   - 验证 code 格式（微信 code 有固定格式）
   - 添加请求签名验证

## 为什么 MyBatis-Plus 能防止 SQL 注入？

```java
// ✅ 安全：使用 #{} 参数化查询
@Select("SELECT * FROM ums_user WHERE openid = #{openid}")
UmsUser selectByOpenid(@Param("openid") String openid);

// ❌ 危险：使用 ${} 字符串拼接（不要使用）
@Select("SELECT * FROM ums_user WHERE openid = '${openid}'")
```

`#{}` 的工作原理：
1. SQL 被编译为：`SELECT * FROM ums_user WHERE openid = ?`
2. 参数单独传递给数据库
3. 数据库将参数视为字符串值，而不是 SQL 代码

## 总结
- **攻击已被阻止**：SQL 注入未能执行
- **脏数据已产生**：需要手动清理
- **代码已修复**：移除了安全隐患
- **建议**：立即清理数据并重新部署
