# MapStruct 使用说明

## 1. 依赖与注解处理器配置

后端使用 Maven 管理依赖，MapStruct 相关配置位于 `backend/pom.xml`：

- 运行时依赖：`org.mapstruct:mapstruct`
- 注解处理器：`org.mapstruct:mapstruct-processor`
- Lombok 兼容绑定：`org.projectlombok:lombok-mapstruct-binding`

## 2. Mapper 接口定义

在 `backend/src/main/java/com/github/waitlight/asskicker/mapper` 下创建接口，并使用 `@Mapper` 标注。
建议使用 `componentModel = "spring"` 以便 Spring 自动注入。

```java
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserView toView(User user);

    @Mapping(target = "passwordHash", ignore = true)
    User toEntity(UserView view);

    @AfterMapping
    default void clearPasswordHash(UserView view, @MappingTarget User user) {
        if (user != null) {
            user.setPasswordHash(null);
        }
    }
}
```

## 3. 使用方式

通过构造器注入使用：

```java
private final UserMapper userMapper;

public UserServiceImpl(..., UserMapper userMapper) {
    this.userMapper = userMapper;
}

return userRepository.save(user).map(userMapper::toView);
```

## 4. 自定义映射建议

- 对于 DTO 中缺失的敏感字段（如 `passwordHash`），使用 `@Mapping(ignore = true)` + `@AfterMapping` 进行显式处理。
- 若字段名称不一致或需要转换逻辑，可使用 `@Mapping` 搭配 `@Named` 或 `@AfterMapping` 实现。

## 5. 性能测试说明

提供 `UserMapperPerformanceTest` 用于对比 MapStruct 与手动映射的性能表现，默认不在常规测试中运行。

```bash
cd backend
mvn -DperfTest=true -Dtest=UserMapperPerformanceTest test
```
