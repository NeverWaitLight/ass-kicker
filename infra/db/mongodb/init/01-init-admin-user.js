db = db.getSiblingDB('asskicker');

const now = Date.now();

// BCrypt(cost=10) hash of "12345678" — Spring Security accepts $2a/$2b/$2y prefixes.
db.users.insertOne({
    username: "admin",
    password: "$2y$10$a19nbIwIykJAki0xtmCCWuUoeu2fB9u6eUFOCTMqWZntkgln3Oa9W",
    role: "ADMIN",
    status: "ACTIVE",
    created_at: now,
    updated_at: now,
    last_login_at: null,
    kicked_out_at: null,
    deleted_at: 0
});
