# 部署指南

## 部署到预发布环境

本文档描述了如何将应用程序部署到预发布(staging)环境。

### 准备工作

在开始部署之前，请确保：

1. 本地代码已提交并推送到远程仓库
2. 所有单元测试和集成测试均已通过
3. Docker 和 Docker Compose 已安装
4. 具有预发布环境的访问权限

### 部署步骤

#### 1. 构建后端镜像

```bash
cd backend
mvn clean package -DskipTests
docker build -t ass-kicker-backend:latest .
```

#### 2. 构建前端镜像

```bash
cd frontend
npm install
npm run build
docker build -t ass-kicker-frontend:latest .
```

#### 3. 准备部署配置

创建预发布环境的配置文件 `deploy/staging/docker-compose.yml`：

```yaml
version: '3.8'

services:
  backend:
    image: ass-kicker-backend:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=staging
      - DB_HOST=postgres-staging
      - DB_PORT=5432
      - DB_NAME=asskicker_staging
      - DB_USER=asskicker_user
      - DB_PASSWORD=${DB_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
    depends_on:
      - postgres-staging
    networks:
      - app-network

  frontend:
    image: ass-kicker-frontend:latest
    ports:
      - "80:80"
    environment:
      - VUE_APP_API_URL=http://staging.yourdomain.com/api
    depends_on:
      - backend
    networks:
      - app-network

  postgres-staging:
    image: postgres:13
    environment:
      - POSTGRES_DB=asskicker_staging
      - POSTGRES_USER=asskicker_user
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data_staging:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    networks:
      - app-network

volumes:
  postgres_data_staging:

networks:
  app-network:
    driver: bridge
```

#### 4. 设置环境变量

在部署服务器上创建 `.env` 文件：

```bash
# 数据库密码
DB_PASSWORD=your_secure_password

# JWT 密钥
JWT_SECRET=your_jwt_secret_key

# 其他环境特定配置
SPRING_PROFILES_ACTIVE=staging
```

#### 5. 部署到预发布环境

```bash
# 导航到部署目录
cd deploy/staging

# 启动服务
docker-compose up -d

# 检查服务状态
docker-compose ps
```

#### 6. 验证部署

1. 检查所有服务是否正在运行：
   ```bash
   docker-compose ps
   ```

2. 检查日志输出：
   ```bash
   docker-compose logs backend
   docker-compose logs frontend
   ```

3. 访问预发布环境 URL 验证应用程序是否正常工作

### 回滚步骤

如果部署出现问题，可以执行以下回滚步骤：

1. 停止当前运行的服务：
   ```bash
   docker-compose down
   ```

2. 如果有备份，恢复到之前的版本：
   ```bash
   # 使用之前的镜像标签
   docker-compose pull
   docker-compose up -d
   ```

### 自动化部署脚本

创建自动化部署脚本 `deploy/deploy-staging.sh`：

```bash
#!/bin/bash

set -e  # 遇到错误时退出

echo "开始部署到预发布环境..."

# 检查是否在正确的目录
if [ ! -f "deploy/staging/docker-compose.yml" ]; then
  echo "错误: 未找到部署配置文件"
  exit 1
fi

# 构建后端
echo "构建后端..."
cd backend
mvn clean package -DskipTests
cd ..

# 构建前端
echo "构建前端..."
cd frontend
npm install
npm run build
cd ..

# 构建 Docker 镜像
echo "构建 Docker 镜像..."
docker build -t ass-kicker-backend:latest ./backend
docker build -t ass-kicker-frontend:latest ./frontend

# 部署到预发布环境
echo "部署到预发布环境..."
cd deploy/staging
docker-compose down
docker-compose pull
docker-compose up -d

# 等待服务启动
echo "等待服务启动..."
sleep 30

# 验证部署
echo "验证部署..."
if docker-compose ps | grep -q "Up"; then
  echo "部署成功!"
  docker-compose ps
else
  echo "部署可能存在问题，请检查日志:"
  docker-compose logs
  exit 1
fi

echo "部署完成!"
```

使脚本可执行：

```bash
chmod +x deploy/deploy-staging.sh
```

### 监控和维护

1. 定期检查日志：
   ```bash
   docker-compose logs -f --tail=100
   ```

2. 监控资源使用情况：
   ```bash
   docker stats
   ```

3. 备份数据库：
   ```bash
   docker exec postgres-staging pg_dump -U asskicker_user asskicker_staging > backup.sql
   ```

### 注意事项

- 在部署前备份重要数据
- 确保预发布环境的配置与生产环境尽可能相似
- 部署期间通知相关团队成员
- 部署后进行全面的功能测试