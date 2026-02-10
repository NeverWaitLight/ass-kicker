## MODIFIED Requirements

### Requirement: Temporary configuration management
系统 SHALL 在测试发送时创建临时通道配置并基于临时配置构建 sender，测试完成后清理临时配置与 sender 实例。

#### Scenario: Temporary configuration creation
- **WHEN** 用户提交测试发送请求
- **THEN** 系统 SHALL 创建临时通道配置，并基于配置与协议构建对应 sender 实例用于发送

#### Scenario: Temporary configuration cleanup
- **WHEN** 测试发送完成（成功或失败）
- **THEN** 系统 SHALL 销毁 sender 实例（可关闭则关闭）并移除临时配置

## ADDED Requirements

### Requirement: Test send logging
系统 SHALL 在测试发送流程中输出关键日志以便定位问题，且不得记录敏感配置明文。

#### Scenario: Logging on test send start
- **WHEN** 系统开始处理测试发送
- **THEN** 系统 SHALL 记录 userId、configId、channelType、protocol、target 等信息

#### Scenario: Logging on test send outcome
- **WHEN** 测试发送完成或失败
- **THEN** 系统 SHALL 记录 success、messageId 或 errorReason 等结果信息
