package com.github.waitlight.asskicker.handlers;

import com.github.waitlight.asskicker.dto.user.CreateUserRequest;
import com.github.waitlight.asskicker.dto.user.ResetPasswordRequest;
import com.github.waitlight.asskicker.dto.user.UpdatePasswordRequest;
import com.github.waitlight.asskicker.dto.user.UpdateUsernameRequest;
import com.github.waitlight.asskicker.security.UserPrincipal;
import com.github.waitlight.asskicker.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Component
public class UserHandler {

    private final UserService userService;

    public UserHandler(UserService userService) {
        this.userService = userService;
    }

    public Mono<ServerResponse> createUser(ServerRequest request) {
        return request.bodyToMono(CreateUserRequest.class)
                .flatMap(userService::createUser)
                .flatMap(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(user))
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "创建用户失败" : ex.getReason()));
    }

    public Mono<ServerResponse> deleteUser(ServerRequest request) {
        return parseId(request)
                .flatMap(userService::deleteUser)
                .then(ServerResponse.noContent().build())
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "删除用户失败" : ex.getReason()));
    }

    public Mono<ServerResponse> resetPassword(ServerRequest request) {
        return parseId(request)
                .flatMap(id -> request.bodyToMono(ResetPasswordRequest.class)
                        .flatMap(body -> userService.resetPassword(id, body.newPassword())))
                .flatMap(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(user))
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "重置密码失败" : ex.getReason()));
    }

    public Mono<ServerResponse> listUsers(ServerRequest request) {
        int page = parseInt(request.queryParam("page").orElse("1"), 1);
        int size = parseInt(request.queryParam("size").orElse("10"), 10);
        String keyword = request.queryParam("keyword").orElse(null);
        return userService.listUsers(page, size, keyword)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response))
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "获取用户列表失败" : ex.getReason()));
    }

    public Mono<ServerResponse> getUserById(ServerRequest request) {
        return parseId(request)
                .flatMap(userService::getUserById)
                .flatMap(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(user))
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "获取用户失败" : ex.getReason()));
    }

    public Mono<ServerResponse> updateMeUsername(ServerRequest request) {
        return request.principal()
                .cast(Authentication.class)
                .map(Authentication::getPrincipal)
                .cast(UserPrincipal.class)
                .flatMap(principal -> request.bodyToMono(UpdateUsernameRequest.class)
                        .flatMap(body -> userService.updateUsername(principal.userId(), body)))
                .flatMap(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(user))
                .switchIfEmpty(ServerResponse.status(HttpStatus.UNAUTHORIZED).build())
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "更新用户名失败" : ex.getReason()));
    }

    public Mono<ServerResponse> updateMePassword(ServerRequest request) {
        return request.principal()
                .cast(Authentication.class)
                .map(Authentication::getPrincipal)
                .cast(UserPrincipal.class)
                .flatMap(principal -> request.bodyToMono(UpdatePasswordRequest.class)
                        .flatMap(body -> userService.updatePassword(principal.userId(), body)))
                .flatMap(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(user))
                .switchIfEmpty(ServerResponse.status(HttpStatus.UNAUTHORIZED).build())
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "更新密码失败" : ex.getReason()));
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private Mono<Long> parseId(ServerRequest request) {
        try {
            return Mono.just(Long.parseLong(request.pathVariable("id")));
        } catch (NumberFormatException ex) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "非法的用户ID"));
        }
    }
}
