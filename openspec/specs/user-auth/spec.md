# Purpose
TBD: Define authentication and authorization requirements for the system using JWT-based authentication.

## Requirements

### Requirement: 用户可以通过用户名密码登录获取 JWT
系统 SHALL 允许用户使用用户名与密码登录，并在成功时签发包含用户身份的 JWT。

#### Scenario: 成功登录
- **WHEN** 用户提交正确的用户名与密码到登录接口
- **THEN** 系统返回 HTTP 200
- **AND** 响应包含 JWT 与用户基础信息（id、username、role、status）

### Requirement: 系统签发 access 与 refresh token 并遵循有效期
系统 SHALL 在登录成功时签发 access token 与 refresh token，access 有效期 1 天，refresh 有效期 30 天。

#### Scenario: 登录返回双令牌
- **WHEN** 用户登录成功
- **THEN** 响应包含 access token 与 refresh token

### Requirement: 禁用用户不得登录
系统 SHALL 拒绝状态为 DISABLED 的用户登录。

#### Scenario: 禁用用户登录被拒绝
- **WHEN** 状态为 DISABLED 的用户提交正确的用户名与密码
- **THEN** 系统返回 HTTP 403 或等价的拒绝响应

### Requirement: 用户名匹配大小写敏感
系统 SHALL 以大小写敏感方式匹配用户名进行认证。

#### Scenario: 大小写不同的用户名无法登录
- **WHEN** 用户使用大小写不匹配的用户名提交登录请求
- **THEN** 系统返回认证失败响应

### Requirement: 登录成功更新最后一次登录时间
系统 SHALL 在用户成功登录后更新其最后一次登录时间。

#### Scenario: 登录后更新 last_login_at
- **WHEN** 用户登录成功
- **THEN** 系统更新该用户的 last_login_at 字段

### Requirement: 受保护接口必须校验 JWT
系统 SHALL 对受保护的业务接口校验 JWT 的签名与过期时间。

#### Scenario: 未携带 JWT 访问受保护接口
- **WHEN** 未携带 JWT 访问受保护接口
- **THEN** 系统返回 HTTP 401

#### Scenario: 携带过期 JWT 访问受保护接口
- **WHEN** 携带过期 JWT 访问受保护接口
- **THEN** 系统返回 HTTP 401

### Requirement: 系统提供刷新 access token 的接口
系统 SHALL 提供刷新接口使用 refresh token 获取新的 access token。

#### Scenario: 使用 refresh token 刷新成功
- **WHEN** 用户提交有效 refresh token 到刷新接口
- **THEN** 系统返回新的 access token（可同时返回新的 refresh token）

#### Scenario: refresh token 无效或过期
- **WHEN** 用户提交无效或过期的 refresh token
- **THEN** 系统返回 HTTP 401

### Requirement: 业务接口对已登录用户开放
系统 SHALL 允许 ADMIN 与 USER 角色访问所有非用户管理的业务接口。

#### Scenario: 普通用户访问业务接口
- **WHEN** USER 角色访问非用户管理业务接口
- **THEN** 系统返回成功响应