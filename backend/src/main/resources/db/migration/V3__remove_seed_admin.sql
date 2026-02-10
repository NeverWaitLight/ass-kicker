-- Remove seeded admin account if it is the only user

DELETE FROM t_user
WHERE username = 'admin'
  AND role = 'ADMIN'
  AND status = 'ACTIVE'
  AND password_hash = '.Da8CJI4.Z/FK2K7XK'
  AND (SELECT COUNT(*) FROM t_user) = 1;
