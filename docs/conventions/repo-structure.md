# 仓库目录规划

## 目录结构

```
ass-kicker/
├── README.md                  # 项目总览、快速上手
├── LICENSE
├── Makefile                   # 顶层编排入口 (build/test/deploy 统一命令)
├── .gitignore
│
├── services/                  # ===== 后端服务 =====
│   ├── java/                  # Java 后端 (Spring Boot)
│   │   ├── pom.xml
│   │   └── src/
│   └── go/                    # Go 后端 (TODO)
│       ├── go.mod
│       └── cmd/
│
├── web/                       # ===== 前端 =====
│   ├── package.json
│   ├── src/
│   └── ...
│
├── infra/                     # ===== 基础设施 / 部署 =====
│   ├── docker/                # Docker Compose
│   │   ├── docker-compose-mongodb.yml
│   │   └── docker-compose-rocketmq.yml
│   ├── k8s/                   # Kubernetes manifests (如有)
│   └── scripts/               # 运维/调试脚本
│       └── debug.sh
│
├── testing/                   # ===== 测试 =====
│   ├── benchmark/             # 性能测试
│   │   ├── benchmark.py
│   │   └── benchmark-record.md
│   ├── e2e/                   # 端到端测试
│   └── integration/           # 集成测试
│
├── docs/                      # ===== 文档 =====
│   ├── Architecture.md        # 架构设计
│   ├── api/                   # API 文档 (OpenAPI/Swagger)
│   ├── conventions/           # 规范与约定 (如本文件)
│   │   └── repo-structure.md  # 仓库目录规划规范
│   ├── design/                # 设计文档 / RFC
│   └── decisions/             # ADR (Architecture Decision Records)
│
└── .github/                   # ===== CI/CD =====
    └── workflows/
        ├── ci-java.yml
        ├── ci-go.yml
        └── ci-web.yml
```

## 关键原则

| 原则                             | 说明                                                                       |
| -------------------------------- | -------------------------------------------------------------------------- |
| **按职责分顶层目录**             | `services/`、`web/`、`infra/`、`testing/`、`docs/` — 一眼看出仓库有什么    |
| **后端按语言分子目录**           | `services/java/`、`services/go/` — 语言不同，构建链完全不同，必须隔离      |
| **基础设施归一处**               | Docker、K8s、部署脚本统一放 `infra/`，不散落在各服务下                     |
| **测试独立于业务代码**           | 跨服务的 benchmark、e2e、集成测试放 `testing/`；**单元测试**留在各模块内部 |
| **文档按类型分**                 | 架构、API、设计决策(ADR) 各有归属，避免 docs 变垃圾场                      |
| **CI/CD 用 `.github/workflows`** | 每个服务一条 pipeline，独立触发                                            |

## Makefile 命令

| 命令                 | 说明                   |
| -------------------- | ---------------------- |
| `make build`         | 构建全部 (Java + Web)  |
| `make build-java`    | 仅构建 Java 后端       |
| `make build-web`     | 仅构建前端             |
| `make test`          | 运行全部测试           |
| `make test-java`     | 仅运行 Java 测试       |
| `make test-web`      | 仅运行前端测试         |
| `make run-java`      | 启动 Java 后端         |
| `make run-web`       | 启动前端开发服务器     |
| `make deps-mongo`    | 启动 MongoDB (Docker)  |
| `make deps-rocketmq` | 启动 RocketMQ (Docker) |
| `make clean`         | 清理构建产物           |

## 补充建议

1. **各服务保留自己的 `.dockerignore`** — 构建上下文隔离
2. **Go 后端** 尚未开始，先建 `services/go/` 占位
3. **`docs/decisions/`** 用 ADR 格式（`001-choose-rocketmq.md`）记录重大技术决策
