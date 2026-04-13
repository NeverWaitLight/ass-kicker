# 项目规则

## HTTP 入参字符串归一化

需要对请求体或查询参数中的字符串做规范化时例如去除首尾空白应在接收请求的 Controller 或等价 Web 层完成再调用下层 Service 假定入参已是归一化后的值不在 Service 内重复 trim 密码等敏感字段除非明确要求否则一般不做 trim

## ValidationMessages Key 命名规范

ValidationMessages 文件中的 key 遵循以下命名格式：

```
model.字段.错误原因
```

- `user.username.exists` - 用户模块 - 用户名字段 - 已存在
- `user.id.notFound` - 用户模块 - ID字段 - 未找到
- `user.password.empty` - 用户模块 - 密码字段 - 为空
- `user.oldPassword.incorrect` - 用户模块 - 旧密码字段 - 不正确
