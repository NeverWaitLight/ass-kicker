## Why

当前系统在初始化时需要预先设置admin账号，这不够灵活。我们需要修改登录逻辑，使得第一个注册的用户自动成为admin账号，无需预先设置。

## What Changes

- 修改后端用户注册逻辑，使第一个注册的用户获得admin权限
- 调整前端登录界面和流程以配合新的逻辑
- 移除初始化时预设admin账号的功能
- 更新相关的认证和授权机制

## Capabilities

### New Capabilities
- `admin-promotion`: 定义如何将第一个注册的用户提升为管理员
- `dynamic-role-assignment`: 实现基于条件的角色分配机制

### Modified Capabilities

## Impact

- 后端用户服务模块
- 前端登录/注册页面
- 数据库用户表结构
- 认证和授权中间件