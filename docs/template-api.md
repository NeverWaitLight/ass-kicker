# Template API 文档

本文档描述了模板管理相关的 API 端点。

## 基础 URL

所有 API 请求都基于以下基础 URL：
```
http://your-domain.com/api
```

## 认证

除非特别说明，所有端点都需要有效的认证令牌。请在请求头中包含：
```
Authorization: Bearer <your-token>
```

## 错误处理

所有 API 端点在发生错误时都会返回适当的 HTTP 状态码和错误信息。

通用错误响应格式：
```json
{
  "error": "错误信息",
  "timestamp": 1700000000000,
  "path": "/api/endpoint"
}
```

## 模板对象

API 返回的模板对象具有以下结构：

```json
{
  "id": 1,
  "name": "模板名称",
  "description": "模板描述",
  "content": "模板内容",
  "createdAt": 1700000000000,
  "updatedAt": 1700000000000
}
```

字段说明：
- `id`: 模板唯一标识符 (整数)
- `name`: 模板名称 (字符串, 必需, 最大255字符)
- `description`: 模板描述 (字符串, 可选, 最大1000字符)
- `content`: 模板内容 (字符串, 必需)
- `createdAt`: 创建时间 (13 位 UTC 时间戳，毫秒)
- `updatedAt`: 更新时间 (13 位 UTC 时间戳，毫秒)

---

## API 端点

### 创建模板

创建一个新的模板。

**请求**
```
POST /api/templates
```

**请求头**
```
Content-Type: application/json
Authorization: Bearer <token>
```

**请求体**
```json
{
  "name": "新模板",
  "description": "模板描述",
  "content": "模板内容..."
}
```

**参数说明**
- `name`: 模板名称 (必需, 字符串, 最大255字符)
- `description`: 模板描述 (可选, 字符串, 最大1000字符)
- `content`: 模板内容 (必需, 字符串)

**响应**
- `200 OK`: 模板创建成功 (注意：响应状态码已从201改为200以符合响应式编程最佳实践)
- `400 Bad Request`: 请求数据无效
- `401 Unauthorized`: 未认证

**响应示例**
```json
{
  "id": 1,
  "name": "新模板",
  "description": "模板描述",
  "content": "模板内容...",
  "createdAt": 1700000000000,
  "updatedAt": 1700000000000
}
```

---

### 获取单个模板

根据 ID 获取特定模板。

**请求**
```
GET /api/templates/{id}
```

**路径参数**
- `id`: 模板 ID (必需, 整数)

**请求头**
```
Authorization: Bearer <token>
```

**响应**
- `200 OK`: 成功返回模板
- `401 Unauthorized`: 未认证
- `404 Not Found`: 模板不存在

**响应示例**
```json
{
  "id": 1,
  "name": "模板1",
  "description": "第一个模板",
  "content": "模板内容...",
  "createdAt": 1700000000000,
  "updatedAt": 1700000000000
}
```

---

### 更新模板

根据 ID 更新特定模板。

**请求**
```
PUT /api/templates/{id}
```

**路径参数**
- `id`: 模板 ID (必需, 整数)

**请求头**
```
Content-Type: application/json
Authorization: Bearer <token>
```

**请求体**
```json
{
  "name": "更新后的模板名",
  "description": "更新后的描述",
  "content": "更新后的内容..."
}
```

**参数说明**
- `name`: 模板名称 (必需, 字符串, 最大255字符)
- `description`: 模板描述 (可选, 字符串, 最大1000字符)
- `content`: 模板内容 (必需, 字符串)

**响应**
- `200 OK`: 模板更新成功
- `400 Bad Request`: 请求数据无效
- `401 Unauthorized`: 未认证
- `404 Not Found`: 模板不存在

**响应示例**
```json
{
  "id": 1,
  "name": "更新后的模板名",
  "description": "更新后的描述",
  "content": "更新后的内容...",
  "createdAt": 1700000000000,
  "updatedAt": 1700003600000
}
```

---

### 删除模板

根据 ID 删除特定模板。

**请求**
```
DELETE /api/templates/{id}
```

**路径参数**
- `id`: 模板 ID (必需, 整数)

**请求头**
```
Authorization: Bearer <token>
```

**响应**
- `204 No Content`: 模板删除成功
- `401 Unauthorized`: 未认证
- `404 Not Found`: 模板不存在

---

## 技术实现说明

### 架构变化
- **实现模式**: 从传统的Spring MVC Controller模式重构为Spring WebFlux Router/Handler模式
- **响应式编程**: 后端现在使用响应式编程模型，提高了系统的响应性和吞吐量
- **非阻塞I/O**: 使用非阻塞I/O处理请求，提升了高并发场景下的性能

### 性能改进
- 改进了并发请求处理能力
- 减少了资源消耗
- 提高了系统的整体响应速度

### API 兼容性
- 所有API端点路径和契约保持不变，确保前端兼容性
- 响应格式保持一致

---

## 示例请求

### 使用 curl 创建模板

```bash
curl -X POST \
  http://your-domain.com/api/templates \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer your-token-here' \
  -d '{
    "name": "API测试模板",
    "description": "通过API创建的模板",
    "content": "这是一个测试模板的内容"
  }'
```

### 使用 JavaScript 获取模板列表

```javascript
fetch('http://your-domain.com/api/templates', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer your-token-here'
  }
})
.then(response => response.json())
.then(data => console.log(data))
.catch(error => console.error('Error:', error));
```

---

## 速率限制

所有 API 端点都受到速率限制保护。默认限制为每小时 1000 个请求。超过限制将返回 `429 Too Many Requests` 响应。

---

## 版本控制

当前 API 版本为 v1。所有端点都隐式使用 v1 版本。未来版本可能会引入新的端点或修改现有端点。
