# 编程规范

## 1. 代码风格约定

### 1.1 命名规范
- 使用有意义的变量、函数和类名
- 遵循驼峰命名法（camelCase）用于变量和函数
- 遵循帕斯卡命名法（PascalCase）用于类和构造函数
- 常量使用大写字母和下划线分隔（UPPER_SNAKE_CASE）

### 1.2 注释要求
- 函数和方法应包含JSDoc-style注释，说明参数和返回值
- 复杂逻辑应添加内联注释解释意图
- 类和模块应有简要描述其职责的注释

## 2. 时间字段处理规范

### 2.1 后端存储与传输
- 所有时间字段在后端存储和HTTP传输时必须使用13位UTC时间戳（毫秒级）
- 时间戳应为整数类型（Long型），表示自1970年1月1日00:00:00 UTC以来的毫秒数
- 数据库中时间字段也应存储为13位UTC时间戳

### 2.2 createdAt、updatedAt、lastLoginAt等系统时间字段处理
- createdAt、updatedAt、lastLoginAt这类非用户输入的时间字段，全部由后端自动获取和维护
- 前端在创建或更新资源时，不得传递这些字段给后端
- 后端在接收请求时，不能接收和使用从前端传来的这些字段值，必须由后端生成
- 创建资源时，后端设置createdAt和updatedAt为当前时间
- 更新资源时，后端仅更新updatedAt为当前时间
- 用户登录时，后端更新lastLoginAt为当前时间

### 2.3 前端展示
- 前端仅在展示时间信息时，将13位UTC时间戳转换为本地时间
- 转换后的格式应符合人类习惯（如：YYYY-MM-DD HH:mm:ss）
- 所有时间计算和传输过程保持使用UTC时间戳

### 2.4 示例
```javascript
// 前端发送数据到后端（不包含系统时间字段）
fetch('/api/users', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    username: 'example',
    password: 'password'
    // 注意：不包含createdAt、updatedAt、lastLoginAt等字段
  })
});

// 前端从后端获取时间戳并转换为本地时间展示
const response = await fetch('/api/users');
const users = await response.json();
users.forEach(user => {
  const createTime = new Date(user.createdAt).toLocaleString(); // 转换为本地时间格式
  const updateTime = new Date(user.updatedAt).toLocaleString();
});
```

```java
// 后端处理创建请求（系统时间字段由后端设置）
@PostMapping("/users")
public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
    User user = new User();
    long now = System.currentTimeMillis(); // 获取当前时间戳
    
    // 设置用户提供的字段
    user.setUsername(request.getUsername());
    user.setPasswordHash(request.getPassword());
    
    // 系统时间字段由后端设置，不使用前端传递的值
    user.setCreatedAt(now);
    user.setUpdatedAt(now);
    
    User savedUser = userService.save(user);
    return ResponseEntity.ok(savedUser);
}

// 后端处理更新请求（仅更新updatedAt）
@PutMapping("/users/{id}")
public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
    User user = userService.findById(id);
    
    // 更新用户提供的字段
    user.setUsername(request.getUsername());
    
    // 仅更新updatedAt，不使用前端传递的系统时间字段
    user.setUpdatedAt(System.currentTimeMillis());
    
    User updatedUser = userService.save(user);
    return ResponseEntity.ok(updatedUser);
}
```

## 3. API设计规范

### 3.1 HTTP方法
- GET：获取资源
- POST：创建资源
- PUT：完全更新资源
- PATCH：部分更新资源
- DELETE：删除资源

### 3.2 状态码
- 200 OK：请求成功
- 201 Created：资源创建成功
- 400 Bad Request：客户端错误
- 401 Unauthorized：未授权
- 404 Not Found：资源不存在
- 500 Internal Server Error：服务器错误

### 3.3 请求/响应格式
- 使用JSON作为主要数据交换格式
- 统一错误响应格式
- 分页数据应包含总数、页码等信息

## 4. 数据库设计规范

### 4.1 命名规范
- 表名使用复数形式，小写字母加下划线分隔
- 字段名使用小写字母加下划线分隔
- 主键统一命名为id
- 外键命名格式为：表名_id

### 4.2 数据类型
- 时间字段存储为BIGINT类型以支持13位时间戳
- 字符串字段根据实际需要选择VARCHAR长度
- 数值字段根据范围选择合适的数据类型

## 5. 代码组织结构

### 5.1 后端结构
- controller：处理HTTP请求
- service：业务逻辑
- repository：数据访问
- entity/model：数据模型
- dto：数据传输对象

### 5.2 前端结构
- components：可复用UI组件
- views/pages：页面组件
- services/api：API调用
- utils：工具函数
- store：状态管理（如果使用）

## 6. 对象映射规范

- 后端对象映射统一使用 MapStruct（见 `docs/mapstruct.md`）
- Mapper 接口放在 `com.github.waitlight.asskicker.mapper` 包内
- 新增 DTO 映射时优先补充对应的单元测试
