## 1. 前端代码修改

- [x] 1.1 打开 `frontend/src/views/ChannelConfigPage.vue`
- [x] 1.2 找到 `protocolConfigs` 数组中的 `HTTP_API` 配置项
- [x] 1.3 将 `label: 'HTTP API'` 修改为 `label: 'HTTP'`

## 2. 验证

- [x] 2.1 启动前端开发服务器
- [x] 2.2 打开通道配置页面，验证下拉选项中显示 "HTTP" 而非 "HTTP API"
- [x] 2.3 创建新通道，选择 HTTP 类型，验证功能正常
- [x] 2.4 验证现有 HTTP_API 通道配置不受影响
