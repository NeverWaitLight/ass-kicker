# Ass Kicker 架构概览

## 背景与目标

本项目用于建立现代化全栈开发基础架构，提供可运行的 Java 21 + Spring WebFlux 后端与 Vue 3 + Ant Design Vue 前端，帮助团队快速启动新项目开发。

## 后端架构

- 技术栈：Java 21、Spring Boot 3.x、Spring WebFlux
- 运行模型：响应式 WebFlux RouterFunction
- 入口类：`com.github.waitlight.asskicker.Application`
- 路由：
  - `GET /health` 返回服务健康状态
  - `GET /status` 返回服务状态摘要
- 构建工具：Maven

### 后端目录结构

- `backend/pom.xml`：依赖与构建配置
- `backend/src/main/java/com/github/waitlight/asskicker`：主代码包
- `backend/src/main/resources/application.yml`：运行配置（不包含通道配置，通道由页面配置并存储在数据库）

## 前端架构

- 技术栈：Vue 3、Vite、Ant Design Vue
- 入口文件：`frontend/src/main.js`
- UI 结构：使用 Ant Design Vue 的布局、卡片、排版等组件

### 前端目录结构

- `frontend/package.json`：依赖与脚本
- `frontend/vite.config.js`：Vite 配置
- `frontend/src/App.vue`：应用主布局
- `frontend/src/components/Home.vue`：首页组件
- `frontend/src/styles.css`：全局样式

## 运行与构建约定

- 后端运行：`mvn -f backend/pom.xml spring-boot:run`
- 前端运行：`npm --prefix frontend install` 后执行 `npm --prefix frontend run dev`
- 构建产物：后端输出在 `backend/target`，避免在仓库根目录执行 Maven 构建
- native 构建：`mvn -f backend/pom.xml -Pnative -DskipTests package`，输出在 `backend/target/native`
- native 约束与回滚说明：见 `docs/architecture/native-image.md`

## 关键决策

- 后端采用 WebFlux RouterFunction，便于扩展多路由风格并保持清晰分层。
- 前后端分离，目录结构独立，降低耦合并便于扩展。
- 采用 Vite 提升前端开发体验与构建效率。
