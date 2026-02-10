-- Normalize channel timestamps to 13-digit UTC milliseconds

UPDATE t_channel
SET created_at = created_at * 1000
WHERE created_at IS NOT NULL
  AND created_at < 1000000000000;

UPDATE t_channel
SET updated_at = updated_at * 1000
WHERE updated_at IS NOT NULL
  AND updated_at < 1000000000000;
