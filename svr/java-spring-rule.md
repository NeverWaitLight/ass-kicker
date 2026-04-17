# Java Spring 后端项目规范

## 依赖注入

- 所有 Bean 注入必须使用**构造方法注入**
- 配合 `@RequiredArgsConstructor` 注解简化构造方法代码
- 禁止使用字段注入（`@Autowired` 字段）

```java
@RequiredArgsConstructor
@Service
public class TemplateService {
    private final TemplateRepository templateRepository;
    private final LanguageTemplateRepository languageTemplateRepository;
}
```

## Lombok 使用规范

- 使用 `@Data` 简化 getter/setter/toString/equals/hashCode
- 使用 `@Builder` 提供构建者模式
- 使用 `@NoArgsConstructor` 和 `@AllArgsConstructor` 提供构造方法
- 使用 `@RequiredArgsConstructor` 配合 final 字段生成构造方法注入

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Template {
    private Long id;
    private String name;
    private String description;
}
```

## 分层架构规范

### 模型层关系

```
Template (高层次抽象模型)
    ├── TemplateService (核心服务层)
    │   ├── TemplateRepository (直接对应 Repository)
    │   └── LanguageTemplateRepository (一组相关对象的 Repository)
    └── 其他 Service (只能引用 TemplateService)
```

### 服务层依赖规则

1. **TemplateService** 可以直接引用：
   - `TemplateRepository`
   - `LanguageTemplateRepository`
   - 其他与 Template 直接相关的 Repository

2. **其他 Service**（如 UserService、ProjectService 等）：
   - ❌ 禁止直接引用 `TemplateRepository`
   - ❌ 禁止直接引用 `LanguageTemplateRepository`
   - ✅ 只能引用 `TemplateService` 进行 Template 相关操作

```java
// ✅ 正确 - TemplateService 可直接使用 Repository
@RequiredArgsConstructor
@Service
public class TemplateService {
    private final TemplateRepository templateRepository;
    private final LanguageTemplateRepository languageTemplateRepository;
}

// ✅ 正确 - 其他 Service 通过 TemplateService 访问
@RequiredArgsConstructor
@Service
public class ProjectService {
    private final TemplateService templateService;
}

// ❌ 错误 - 其他 Service 不能直接使用 Repository
@RequiredArgsConstructor
@Service
public class ProjectService {
    private final TemplateRepository templateRepository; // 禁止
}
```

## 代码示例

### Entity 类

```java
@Entity
@Table(name = "templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Template {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
}
```

### Repository 层

```java
public interface TemplateRepository extends JpaRepository<Template, Long> {
    // 自定义查询方法
}

public interface LanguageTemplateRepository extends JpaRepository<LanguageTemplate, Long> {
    // 自定义查询方法
}
```

### Service 层

```java
@RequiredArgsConstructor
@Service
@Transactional
public class TemplateService {
    private final TemplateRepository templateRepository;
    private final LanguageTemplateRepository languageTemplateRepository;

    public List<Template> findAll() {
        return templateRepository.findAll();
    }

    public Template findById(Long id) {
        return templateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
    }
}
```

## 参数校验国际化规范

### 校验注解消息国际化

所有 `@Validated` 相关校验注解的错误消息必须使用国际化，禁止硬编码消息文本。

#### 配置文件

国际化消息存放在 `src/main/resources/` 目录下：
- `ValidationMessages.properties` - 默认（英文）消息
- `ValidationMessages_zh_CN.properties` - 中文消息

#### 命名规范

消息 key 采用 `{模块}.{字段}.{校验类型}` 格式：
- 模块：如 `user`、`apikey`、`auth` 等
- 字段：如 `name`、`password`、`expiresIn` 等
- 校验类型：如 `notblank`、`notnull`、`size`、`pattern` 等

```properties
# ValidationMessages.properties
apikey.name.notblank=API Key name cannot be empty
apikey.name.size=API Key name cannot exceed 100 characters
apikey.expiresIn.notnull=Expiration time cannot be empty

# ValidationMessages_zh_CN.properties
apikey.name.notblank=API Key 名称不能为空
apikey.name.size=API Key 名称不能超过100个字符
apikey.expiresIn.notnull=过期时间不能为空
```

#### DTO 校验示例

```java
public record CreateApiKeyDTO(
        @NotBlank(message = "{apikey.name.notblank}")
        @Size(max = 100, message = "{apikey.name.size}")
        String name,
        @NotNull(message = "{apikey.expiresIn.notnull}")
        ExpiresIn expiresIn
) {
}
```

#### Controller 校验示例

```java
@Validated
@RestController
public class ApiKeyController {

    @PostMapping
    public Mono<Resp<CreateApiKeyVO>> create(
            @RequestBody @Validated CreateApiKeyDTO request) {
        // ...
    }

    @DeleteMapping("/{id}")
    public Mono<Void> revoke(
            @PathVariable @NotBlank(message = "{apikey.id.notblank}") String id) {
        // ...
    }
}
```

## Update 接口规范

### 参数传递规则

**所有 update 语义的接口（PUT/PATCH）必须使用 RequestBody 传参，禁止使用 URL 路径参数（@PathVariable）传递业务数据。**

- `id` 等标识字段必须包含在 DTO 中，通过 `@RequestBody` 传递
- DTO 中的 `id` 字段必须添加 `@NotBlank` 校验注解
- URL 仅用于资源定位，不传递业务参数

```java
// ✅ 正确 - id 通过 body 传递
@PutMapping
public Mono<Resp<TemplateDTO>> update(@RequestBody @Validated TemplateDTO request) {
    return templateService.update(request.getId(), request);
}

// ❌ 错误 - id 通过 URL 传递
@PutMapping("/{id}")
public Mono<Resp<TemplateDTO>> update(
        @PathVariable String id,
        @RequestBody @Validated TemplateDTO request) {
    return templateService.update(id, request);
}
```

### DTO 设计规范

update 操作的 DTO 忺须包含 `id` 字段：

```java
public record UpdateApiKeyDTO(
        @NotBlank(message = "{apikey.id.notblank}")
        String id,
        @NotBlank(message = "{apikey.name.notblank}")
        @Size(max = 100, message = "{apikey.name.size}")
        String name
) {
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateDTO {
    @NotBlank(message = "{template.id.notblank}")
    private String id;

    @NotBlank(message = "{template.code.notblank}")
    private String code;

    // 其他字段...
}
```
