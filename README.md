# Ass Kicker

## TODO

- [后端待办事项](backend/README.md#todo)
- [前端待办事项](frontend/README.md#todo)

## 后端启动

1. 准备 PostgreSQL 数据库并创建 `ass_kicker` 数据库。
2. 按需修改 `backend/src/main/resources/application.yml` 中的数据库连接信息。
3. 在 `backend` 目录运行：

```bash
mvn spring-boot:run
```

## Swagger / OpenAPI

启动后访问：

- `http://localhost:8080/swagger-ui.html`

## 渠道管理 API

渠道接口需要携带登录后的 Bearer Token。

- `POST /v1/channels`
- `GET /v1/channels`
- `GET /v1/channels/{id}`
- `PUT /v1/channels/{id}`
- `DELETE /v1/channels/{id}`

示例：

```bash
# 注册并登录获取 accessToken
curl -X POST http://localhost:8080/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"pass"}'

curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"pass"}'

# 创建渠道（将 accessToken 替换为实际值）
curl -X POST http://localhost:8080/v1/channels \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Email Channel",
    "type": "email",
    "description": "SMTP channel",
    "properties": {
      "protocol": "SMTP",
      "smtp": {
        "host": "smtp.example.com",
        "port": 465,
        "username": "user@example.com",
        "password": "pass"
      }
    }
  }'
```
