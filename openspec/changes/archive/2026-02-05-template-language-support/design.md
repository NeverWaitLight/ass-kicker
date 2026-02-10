## Context

当前系统中的Template实体包含了content字段，导致模板内容与模板元数据混合在一起。根据需求，我们需要将模板内容与语言分离，使Template成为抽象的模板定义，而具体的模板内容通过LanguageTemplate实体存储，并通过语言进行区分。

## Goals / Non-Goals

**Goals:**
- 重新设计数据模型，将模板内容与语言分离
- 实现Template与LanguageTemplate之间的一对多关系
- 使用64位整数存储UTC时间戳
- 支持多语言模板内容管理
- 确保向后兼容性（如果可能）

**Non-Goals:**
- 重构前端UI组件
- 更改数据库连接池配置
- 实现国际化消息资源管理

## Decisions

1. **Template实体设计**：
   - 保留id、name字段
   - 添加code字段作为唯一索引，用于快速查找模板
   - 添加description字段用于描述模板
   - 移除content字段，因为内容将存储在LanguageTemplate中
   - createdAt和updatedAt使用Long类型存储UTC时间戳

2. **LanguageTemplate实体设计**：
   - id：主键
   - template_id：外键，关联到Template实体
   - language：枚举类型，表示模板语言
   - content：模板的具体内容
   - createdAt和updatedAt使用Long类型存储UTC时间戳

3. **TemplateLanguage枚举**：
   - 使用标准的国际语言代码：zh-CN(简体中文)、zh-TW(繁体中文)、en(英语)、fr(法语)、de(德语)
   - 每个枚举值包含code和displayName属性

4. **数据库设计**：
   - 修改templates表：移除content列，添加code列并建立唯一索引
   - 新增language_templates表：存储特定语言的模板内容
   - 在language_templates表中建立template_id外键约束

5. **API设计**：
   - 保持现有的Template CRUD操作接口
   - 添加LanguageTemplate相关的API接口
   - 提供按语言获取模板内容的方法

## Risks / Trade-offs

[Risk] 数据迁移过程中可能导致数据丢失
→ Mitigation: 在执行数据迁移前备份数据库，并编写回滚脚本

[Risk] 现有API调用者可能需要适配新的数据结构
→ Mitigation: 提供过渡期的兼容API，逐步引导用户迁移到新接口

[Risk] 查询特定语言的模板内容可能需要额外的JOIN操作，影响性能
→ Mitigation: 在language_templates表上建立适当的索引以优化查询性能