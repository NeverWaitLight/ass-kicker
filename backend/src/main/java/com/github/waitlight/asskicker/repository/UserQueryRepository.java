package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.User;
import com.github.waitlight.asskicker.model.UserRole;
import com.github.waitlight.asskicker.model.UserStatus;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class UserQueryRepository {

    private final DatabaseClient databaseClient;

    public UserQueryRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public Flux<User> findPage(String keyword, int limit, int offset) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, username, password_hash, role, status, created_at, updated_at, last_login_at
                FROM t_user
                """);
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (hasKeyword) {
            sql.append(" WHERE username LIKE :keyword");
        }
        sql.append(" ORDER BY id DESC LIMIT :limit OFFSET :offset");

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString())
                .bind("limit", limit)
                .bind("offset", offset);
        if (hasKeyword) {
            spec = spec.bind("keyword", "%" + keyword + "%");
        }

        return spec.map((row, meta) -> {
            User user = new User();
            user.setId(row.get("id", Long.class));
            user.setUsername(row.get("username", String.class));
            user.setPasswordHash(row.get("password_hash", String.class));
            String role = row.get("role", String.class);
            String status = row.get("status", String.class);
            user.setRole(role == null ? null : UserRole.valueOf(role));
            user.setStatus(status == null ? null : UserStatus.valueOf(status));
            user.setCreatedAt(row.get("created_at", Long.class));
            user.setUpdatedAt(row.get("updated_at", Long.class));
            user.setLastLoginAt(row.get("last_login_at", Long.class));
            return user;
        }).all();
    }

    public Mono<Long> count(String keyword) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS cnt FROM t_user");
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (hasKeyword) {
            sql.append(" WHERE username LIKE :keyword");
        }

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString());
        if (hasKeyword) {
            spec = spec.bind("keyword", "%" + keyword + "%");
        }

        return spec.map((row, meta) -> row.get("cnt", Long.class))
                .one()
                .defaultIfEmpty(0L);
    }
}
