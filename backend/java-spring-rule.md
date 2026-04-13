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
