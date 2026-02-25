## 1. 删除 Schema 服务类

- [x] 1.1 删除 `EmailProtocolSchemaService.java` 文件
- [x] 1.2 删除 `EmailProtocolSchemaResponse.java` DTO 文件

## 2. 修改 ChannelHandler

- [x] 2.1 移除 `EmailProtocolSchemaService` 的依赖注入
- [x] 2.2 删除 `listEmailProtocols` 方法

## 3. 修改 ChannelRouter

- [x] 3.1 移除 `EmailProtocolSchemaResponse` 的导入
- [x] 3.2 移除 `/api/channels/email-protocols` 路由定义

## 4. 验证

- [x] 4.1 运行后端构建确保无编译错误
- [x] 4.2 运行相关测试确保功能正常
