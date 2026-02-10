# Purpose
TBD: Define user management requirements including CRUD operations, role-based access control, and user profile management.

## Requirements

### Requirement: 管理员可以创建用户
系统 SHALL 允许 ADMIN 角色创建用户，且用户名必须唯一并保存密码哈希与角色。

#### Scenario: 管理员创建用户成功
- **WHEN** ADMIN 提交合法的用户名、密码与角色
- **THEN** 系统创建新用户并返回用户基础信息

#### Scenario: 用户名重复创建失败
- **WHEN** ADMIN 使用已存在的用户名创建用户
- **THEN** 系统返回冲突响应（HTTP 409 或等价）

### Requirement: 管理员可以删除用户
系统 SHALL 允许 ADMIN 角色删除指定用户。

#### Scenario: 管理员删除用户
- **WHEN** ADMIN 请求删除存在的用户
- **THEN** 系统删除该用户并返回成功响应

### Requirement: 管理员可以重置任意用户密码
系统 SHALL 允许 ADMIN 角色直接设置任意用户的新密码。

#### Scenario: 管理员重置用户密码
- **WHEN** ADMIN 提交目标用户与新密码
- **THEN** 系统更新该用户密码并返回成功响应

### Requirement: 管理员可以获取用户列表
系统 SHALL 提供用户列表接口，返回用户基础信息集合。

#### Scenario: 管理员获取用户列表
- **WHEN** ADMIN 请求用户列表
- **THEN** 系统返回包含用户基础信息的列表

### Requirement: 用户列表支持分页与搜索
系统 SHALL 支持用户列表分页与用户名搜索，默认每页 10 条。

#### Scenario: 管理员携带分页参数请求列表
- **WHEN** ADMIN 请求用户列表并提供 page 与 size 参数
- **THEN** 系统返回对应分页的用户列表与总数

#### Scenario: 管理员按用户名关键字搜索
- **WHEN** ADMIN 请求用户列表并提供 keyword 参数
- **THEN** 系统仅返回匹配用户名的用户列表

### Requirement: 用户可以修改自己的用户名
系统 SHALL 允许已登录用户修改自己的用户名，且新用户名必须唯一。

#### Scenario: 用户修改用户名成功
- **WHEN** USER 提交未被占用的新用户名
- **THEN** 系统更新用户名并返回最新用户信息

#### Scenario: 用户修改为已存在用户名
- **WHEN** USER 提交已存在的用户名
- **THEN** 系统返回冲突响应（HTTP 409 或等价）

### Requirement: 用户可以修改自己的密码
系统 SHALL 允许已登录用户修改自己的密码，且必须提供旧密码验证。

#### Scenario: 用户修改密码成功
- **WHEN** USER 提交正确旧密码与新密码
- **THEN** 系统更新密码并返回成功响应

#### Scenario: 用户旧密码错误
- **WHEN** USER 提交错误旧密码
- **THEN** 系统返回认证失败响应

### Requirement: 非管理员不得访问用户管理接口
系统 SHALL 拒绝 USER 角色访问管理员用户管理接口。

#### Scenario: 普通用户访问管理员接口被拒绝
- **WHEN** USER 访问管理员用户管理接口
- **THEN** 系统返回 HTTP 403