## Why

当前项目中的对象映射主要通过手动编写转换代码实现，这种方式容易出错且难以维护。引入MapStruct可以自动生成类型安全的对象映射代码，提高开发效率和代码质量。

## What Changes

- 引入MapStruct依赖到项目
- 删除现有的UserMapper类
- 创建MapStruct映射接口替代原有的手动映射代码
- 更新相关业务代码以使用MapStruct生成的映射器

## Capabilities

### New Capabilities
- `object-mapping`: 使用MapStruct实现自动对象映射功能

### Modified Capabilities

## Impact

- 需要在项目中添加MapStruct依赖
- 现有的手动对象映射代码将被替换
- 编译过程需要支持MapStruct注解处理器
- 代码可读性和维护性将得到提升