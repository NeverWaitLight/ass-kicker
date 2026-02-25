## ADDED Requirements

### Requirement: 分层键值编辑器

定义分层键值编辑器组件：

- 支持分层/嵌套 KV 结构
- 支持不同层级的值填充
- 支持添加/删除 KV 对

#### Scenario: 编辑嵌套 KV 结构
- **WHEN** 用户在编辑器中展开嵌套层级
- **THEN** 显示该层级下的所有 KV 对

#### Scenario: 添加新的 KV 对
- **WHEN** 用户点击添加按钮并输入键值
- **THEN** 新的 KV 对被添加到结构中

---

### Requirement: 按钮标签简化

定义按钮标签的简化规则：

- 创建按钮使用"新建"（而非"新建通道"等具体描述）
- 导航按钮使用"返回"（而非"返回列表"）
- 提供 tooltip 保留上下文含义

#### Scenario: 显示创建按钮
- **WHEN** 页面显示创建操作按钮
- **THEN** 按钮文本为"新建"，hover 时显示 tooltip

#### Scenario: 显示返回按钮
- **WHEN** 页面显示导航返回按钮
- **THEN** 按钮文本为"返回"，tooltip 说明返回目标

---

### Requirement: 暗色模式颜色调整

定义暗色模式的颜色对比度规范：

- 背景色为 #073642 时，文本颜色使用 #faf4e8
- 符合 WCAG AA 对比度标准

#### Scenario: 暗色模式文本显示
- **WHEN** 应用启用暗色模式
- **THEN** 文本颜色与背景色满足 WCAG AA 对比度要求
