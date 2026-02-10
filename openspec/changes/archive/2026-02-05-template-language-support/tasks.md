## 1. 数据模型设计

- [x] 1.1 创建TemplateLanguage枚举类，包含zh-CN、zh-TW、en、fr、de语言代码
- [x] 1.2 修改Template实体类，移除content字段，添加code唯一索引字段
- [x] 1.3 创建LanguageTemplate实体类，包含template_id、language、content等字段
- [x] 1.4 更新Template和LanguageTemplate的时间戳字段为Long类型存储UTC时间戳

## 2. 数据访问层实现

- [x] 2.1 创建LanguageTemplateRepository接口
- [x] 2.2 更新TemplateRepository以适应新的Template实体
- [x] 2.3 实现LanguageTemplate的CRUD操作

## 3. 业务逻辑层实现

- [x] 3.1 更新TemplateService接口以支持新的数据结构
- [x] 3.2 实现TemplateService中与LanguageTemplate相关的业务逻辑
- [x] 3.3 创建LanguageTemplateService接口及其实现

## 4. 控制器层实现

- [x] 4.1 更新TemplateHandler以支持新的API接口
- [x] 4.2 创建LanguageTemplateHandler处理多语言模板内容相关请求
- [x] 4.3 更新TemplateRouter以映射新的路由
- [x] 4.4 创建LanguageTemplateRouter处理多语言模板内容相关路由

## 5. 数据库迁移

- [x] 5.1 编写数据库迁移脚本，修改templates表结构
- [x] 5.2 编写数据库迁移脚本，创建language_templates表
- [x] 5.3 测试数据库迁移脚本的正确性和回滚能力

## 6. 测试验证

- [x] 6.1 编写单元测试验证Template实体
- [x] 6.2 编写单元测试验证LanguageTemplate实体
- [x] 6.3 编写集成测试验证API端点
- [x] 6.4 验证时间戳正确转换为UTC时间戳