# 生产环境部署指南

## 部署到生产环境

本文档描述了如何将应用程序安全地部署到生产环境。

### 部署前检查清单

在部署到生产环境之前，请确保完成以下检查：

- [ ] 预发布环境测试已通过
- [ ] 所有功能按预期工作
- [ ] 性能测试结果符合要求
- [ ] 安全扫描无高危漏洞
- [ ] 数据库迁移脚本已测试
- [ ] 回滚计划已制定
- [ ] 相关团队已通知部署时间窗口
- [ ] 备份策略已确认

### 生产部署步骤

#### 1. 准备生产配置

创建生产环境的配置文件 `deploy/production/docker-compose.yml`：

```yaml
version: '3.8'

services:
  backend:
    image: ass-kicker-backend:${VERSION}
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=postgres-prod
      - DB_PORT=5432
      - DB_NAME=asskicker_prod
      - DB_USER=asskicker_user
      - DB_PASSWORD=${DB_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
      - SERVER_SERVLET_CONTEXT_PATH=/api
    depends_on:
      - postgres-prod
    networks:
      - app-network
    restart: unless-stopped
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"

  frontend:
    image: ass-kicker-frontend:${VERSION}
    ports:
      - "80:80"
    environment:
      - VUE_APP_API_URL=https://yourdomain.com/api
    depends_on:
      - backend
    networks:
      - app-network
    restart: unless-stopped
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"

  postgres-prod:
    image: postgres:13
    environment:
      - POSTGRES_DB=asskicker_prod
      - POSTGRES_USER=asskicker_user
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - /mnt/postgres_data_prod:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    networks:
      - app-network
    restart: unless-stopped
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"

volumes:
  postgres_data_prod:

networks:
  app-network:
    driver: bridge
```

#### 2. 生产环境部署脚本

创建生产部署脚本 `deploy/deploy-production.sh`：

```bash
#!/bin/bash

set -e  # 遇到错误时退出

DEPLOYMENT_TIME=$(date '+%Y-%m-%d %H:%M:%S')
VERSION=$1

if [ -z "$VERSION" ]; then
  echo "用法: $0 <version-tag>"
  echo "例如: $0 v1.2.0"
  exit 1
fi

echo "开始生产环境部署..."
echo "部署时间: $DEPLOYMENT_TIME"
echo "版本: $DEPLOYMENT_TIME"

# 记录部署开始
echo "[$DEPLOYMENT_TIME] 开始部署版本 $VERSION" >> deployment.log

# 检查是否在正确的目录
if [ ! -f "deploy/production/docker-compose.yml" ]; then
  echo "错误: 未找到生产环境部署配置文件"
  exit 1
fi

# 拉取最新的代码
echo "拉取最新代码..."
git fetch origin
git checkout $VERSION

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
docker build -t ass-kicker-backend:$VERSION ./backend
docker build -t ass-kicker-frontend:$VERSION ./frontend

# 推送镜像到仓库（如果有）
# docker tag ass-kicker-backend:$VERSION your-registry/ass-kicker-backend:$VERSION
# docker push your-registry/ass-kicker-backend:$VERSION

# 部署到生产环境
echo "部署到生产环境..."
cd deploy/production

# 创建临时环境文件
cat > .env << EOF
VERSION=$VERSION
DB_PASSWORD=${PROD_DB_PASSWORD}
JWT_SECRET=${PROD_JWT_SECRET}
EOF

# 停止当前服务
echo "停止当前服务..."
docker-compose down

# 拉取最新镜像
docker-compose pull

# 启动新服务
echo "启动新服务..."
docker-compose up -d

# 等待服务启动
echo "等待服务启动..."
sleep 60

# 运行健康检查
echo "运行健康检查..."
HEALTH_CHECK_URL="http://localhost:8080/health"
for i in {1..30}; do
  if curl -f $HEALTH_CHECK_URL > /dev/null 2>&1; then
    echo "服务健康检查通过"
    break
  fi
  echo "等待服务启动... ($i/30)"
  sleep 10
done

# 验证部署
echo "验证部署..."
if docker-compose ps | grep -q "Up"; then
  echo "部署成功!"
  docker-compose ps
  
  # 记录成功部署
  echo "[$DEPLOYMENT_TIME] 成功部署版本 $VERSION" >> ../deployment.log
  
  # 发送部署成功通知（如果有通知系统）
  # curl -X POST -H 'Content-type: application/json' --data '{"text":"Production deployment successful!"}' $SLACK_WEBHOOK_URL
else
  echo "部署可能存在问题，请检查日志:"
  docker-compose logs
  echo "[$DEPLOYMENT_TIME] 部署失败，版本 $VERSION" >> ../deployment.log
  exit 1
fi

echo "生产环境部署完成!"
echo "部署时间: $(date '+%Y-%m-%d %H:%M:%S')"
```

#### 3. 最终测试清单

部署完成后，执行以下最终测试：

1. **功能测试**
   - 验证所有核心功能正常工作
   - 测试模板的 CRUD 操作
   - 验证用户界面交互

2. **性能测试**
   - 验证页面加载时间
   - 测试并发用户访问
   - 检查数据库查询性能

3. **安全测试**
   - 验证身份验证和授权
   - 检查敏感信息是否正确隐藏
   - 验证输入验证

4. **监控检查**
   - 确认日志记录正常
   - 验证监控指标收集
   - 检查警报配置

#### 4. 回滚计划

如果生产部署出现问题，立即执行以下回滚步骤：

```bash
# 1. 记录问题
echo "$(date): 回滚到上一版本" >> rollback.log

# 2. 停止当前服务
cd deploy/production
docker-compose down

# 3. 启动上一版本
PREVIOUS_VERSION=$(cat previous_version.txt)
docker-compose up -d

# 4. 验证回滚
sleep 60
curl -f http://localhost:8080/health
```

#### 5. 部署后任务

- [ ] 通知利益相关者部署完成
- [ ] 监控系统日志和指标
- [ ] 验证所有服务正常运行
- [ ] 更新部署文档
- [ ] 记录部署过程中的经验教训

### 自动化 CI/CD 配置

如果使用 CI/CD 系统，以下是 GitHub Actions 示例配置：

```yaml
name: Production Deployment

on:
  push:
    tags:
      - 'v*'

jobs:
  deploy:
    runs-on: ubuntu-latest
    if: startsWith(github.ref, 'refs/tags/v')
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v2
      
    - name: Setup Node.js
      uses: actions/setup-node@v2
      with:
        node-version: '16'
        
    - name: Setup Java
      uses: actions/setup-java@v2
      with:
        distribution: 'temurin'
        java-version: '11'
        
    - name: Build backend
      run: |
        cd backend
        mvn clean package -DskipTests
        
    - name: Build frontend
      run: |
        cd frontend
        npm install
        npm run build
        
    - name: Build Docker images
      run: |
        docker build -t ass-kicker-backend:${{ github.ref_name }} ./backend
        docker build -t ass-kicker-frontend:${{ github.ref_name }} ./frontend
        
    - name: Deploy to production
      env:
        PROD_DB_PASSWORD: ${{ secrets.PROD_DB_PASSWORD }}
        PROD_JWT_SECRET: ${{ secrets.PROD_JWT_SECRET }}
      run: |
        chmod +x deploy/deploy-production.sh
        ./deploy/deploy-production.sh ${{ github.ref_name }}
```

### 注意事项

- 仅在预定的维护窗口内部署到生产环境
- 始终保留至少一个可工作的旧版本副本
- 部署过程中密切监控系统性能
- 准备好快速回滚方案
- 确保有足够的日志记录以便调试