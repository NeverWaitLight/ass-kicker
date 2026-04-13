# 项目规则

## ValidationMessages Key 命名规范

ValidationMessages 文件中的 key 遵循以下命名格式：

```
model.字段.错误原因
```

- `user.username.exists` - 用户模块 - 用户名字段 - 已存在
- `user.id.notFound` - 用户模块 - ID字段 - 未找到
- `user.password.empty` - 用户模块 - 密码字段 - 为空
- `user.oldPassword.incorrect` - 用户模块 - 旧密码字段 - 不正确
