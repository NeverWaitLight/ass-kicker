## ADDED Requirements

### Requirement: 提供 native 构建流程
系统 MUST 通过 Maven 提供可重复的 native 构建入口，并产出可执行文件。

#### Scenario: 本地构建产出可执行文件
- **WHEN** 执行 native 构建命令
- **THEN** 生成可执行文件且在无 JVM 环境可启动

### Requirement: 启用 Spring AOT 生成
native 构建流程 MUST 启用 Spring AOT，并使用 AOT 生成物参与 native 编译。

#### Scenario: AOT 生成阶段参与编译
- **WHEN** 执行 native 构建命令
- **THEN** AOT 生成阶段成功完成且 native 编译使用其输出

### Requirement: 禁止反射与运行时代理
系统运行时 MUST 不使用反射、JDK 动态代理或 CGLIB 等代理机制；构建 MUST 在检测到此类用法时失败。

#### Scenario: 检测到反射或代理即失败
- **WHEN** 构建分析发现反射或代理使用
- **THEN** native 构建失败并提示违规来源