## Why

当前系统中的Template对象包含了模板的具体内容，这不符合设计要求。根据需求，Template应该是抽象的模板，不包含具体内容，而具体的模板内容应该通过语言进行区分和管理。因此需要重新设计数据模型，将模板内容与语言分离。

## What Changes

- 修改现有的Template实体，移除content字段，添加code唯一索引字段
- 创建新的TemplateLanguage枚举，支持简体中文(zh-CN)、繁体中文(zh-TW)、英文(en)、法语(fr)、德语(de)
- 创建新的LanguageTemplate实体，存储特定语言的模板内容
- 建立Template与LanguageTemplate之间的一对多关系
- 修改时间戳存储方式，使用64位整数存储UTC时间戳
- 更新相关的Repository、Service、Handler和Router实现

## Capabilities

### New Capabilities
- `template-language-support`: 实现模板多语言支持功能，允许为同一模板提供多种语言版本的内容

### Modified Capabilities

## Impact

- 后端数据模型需要重构，包括Template和新增LanguageTemplate实体
- 数据库表结构需要更新，包括templates表的修改和language_templates表的新增
- API接口需要相应调整以支持新的数据结构
- 前端界面可能需要更新以支持多语言模板的选择和编辑