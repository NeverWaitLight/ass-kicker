# 项目规则

## HTTP 入参字符串归一化

需要对请求体或查询参数中的字符串做规范化时例如去除首尾空白应在接收请求的 Controller 或等价 Web 层完成再调用下层 Service 假定入参已是归一化后的值不在 Service 内重复 trim 密码等敏感字段除非明确要求否则一般不做 trim

## ValidationMessages Key 命名规范

ValidationMessages 文件中的 key 遵循以下命名格式：

```
model.字段.错误原因
```

- `user.username.exists` - 用户模块 - 用户名字段 - 已存在
- `user.id.notFound` - 用户模块 - ID字段 - 未找到
- `user.password.empty` - 用户模块 - 密码字段 - 为空
- `user.oldPassword.incorrect` - 用户模块 - 旧密码字段 - 不正确

## 类内方法排列顺序

private 方法紧跟其服务的 public 方法之后，多个 public 方法共用的 private 方法放在类的底部

```java
// public 方法
public Mono<UserEntity> create(UserEntity u) { ... }

// 仅服务于 create 的 private 方法，紧随其后
private UserEntity initNewUser(UserEntity u) { ... }

// 下一个 public 方法
public Mono<Void> delete(String id) { ... }

// 仅服务于 delete 的 private 方法
private UserEntity markAsDeleted(UserEntity user) { ... }

// 多个 public 方法共用的 private 方法，放在类底部
private void invalidateUserCaches(UserEntity before, UserEntity after) { ... }
```

## Reactor 管道惯用写法

判断记录是否存在时使用 flatMap + switchIfEmpty 而非 hasElement + if/else

```java
// 存在则报错，不存在则执行
return repository.findByUsername(username)
        .flatMap(existing -> Mono.<T>error(new ConflictException("...")))
        .switchIfEmpty(Mono.defer(() -> repository.save(entity)));

// 不存在则报错，存在则继续
return repository.findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("...")))
        .flatMap(entity -> ...);
```

将 lambda 内的字段变更逻辑提取到私有方法，保持管道主流程语义清晰

```java
// 主流程只描述步骤
public Mono<Void> delete(String id) {
    return repository.findById(id)
            .switchIfEmpty(Mono.error(new NotFoundException("...")))
            .flatMap(entity -> repository.save(markAsDeleted(entity))
                    .doOnSuccess(saved -> invalidateCaches(entity, saved)))
            .then();
}

// 变更细节封装在私有方法
private UserEntity markAsDeleted(UserEntity user) {
    long now = Instant.now().toEpochMilli();
    user.setDeletedAt(now);
    user.setUpdatedAt(now);
    return user;
}
