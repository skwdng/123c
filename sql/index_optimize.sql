-- ============================================================
-- 索引优化脚本（可重复执行，已存在的索引自动跳过）
-- 思路：
--   1. 先查 information_schema.statistics 判断索引是否存在
--   2. 缺失才执行 ADD INDEX，冗余的单列索引被复合索引覆盖后删除
-- 优化点：
--   1) t_video   个人主页作品列表 WHERE user_id=? ORDER BY create_time DESC
--                 → 复合索引 (user_id, create_time)，原单列 idx_user_id 冗余删除
--   2) t_follow  关注列表 WHERE follower_id=? ORDER BY create_time DESC
--                 → 复合索引 (follower_id, create_time)，原单列 idx_follower 冗余删除
--   3) t_follow  粉丝列表 WHERE followee_id=? ORDER BY create_time DESC
--                 → 复合索引 (followee_id, create_time)，原单列 idx_followee 冗余删除
-- 说明：复合索引的最左前缀就是原单列字段，所以单列索引完全被覆盖，
--       留着只会白白增加写操作的维护成本，直接删掉
-- ============================================================

USE cfeed;

-- ------------------------------------------------------------
-- 1) t_video 增加 idx_user_time(user_id, create_time)
-- ------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 't_video' AND index_name = 'idx_user_time');
SET @ddl = IF(@idx_exists > 0,
              'SELECT ''skip: t_video.idx_user_time 已存在'' AS msg',
              'ALTER TABLE t_video ADD INDEX idx_user_time (user_id, create_time)');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- t_video 删除冗余单列索引 idx_user_id（被 idx_user_time 最左前缀覆盖）
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 't_video' AND index_name = 'idx_user_id');
SET @ddl = IF(@idx_exists = 0,
              'SELECT ''skip: t_video.idx_user_id 不存在'' AS msg',
              'ALTER TABLE t_video DROP INDEX idx_user_id');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 2) t_follow 增加 idx_follower_time(follower_id, create_time)
-- ------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 't_follow' AND index_name = 'idx_follower_time');
SET @ddl = IF(@idx_exists > 0,
              'SELECT ''skip: t_follow.idx_follower_time 已存在'' AS msg',
              'ALTER TABLE t_follow ADD INDEX idx_follower_time (follower_id, create_time)');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- t_follow 删除冗余单列索引 idx_follower
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 't_follow' AND index_name = 'idx_follower');
SET @ddl = IF(@idx_exists = 0,
              'SELECT ''skip: t_follow.idx_follower 不存在'' AS msg',
              'ALTER TABLE t_follow DROP INDEX idx_follower');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 3) t_follow 增加 idx_followee_time(followee_id, create_time)
-- ------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 't_follow' AND index_name = 'idx_followee_time');
SET @ddl = IF(@idx_exists > 0,
              'SELECT ''skip: t_follow.idx_followee_time 已存在'' AS msg',
              'ALTER TABLE t_follow ADD INDEX idx_followee_time (followee_id, create_time)');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- t_follow 删除冗余单列索引 idx_followee
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 't_follow' AND index_name = 'idx_followee');
SET @ddl = IF(@idx_exists = 0,
              'SELECT ''skip: t_follow.idx_followee 不存在'' AS msg',
              'ALTER TABLE t_follow DROP INDEX idx_followee');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 验证：查看优化后的索引
-- ------------------------------------------------------------
SELECT table_name, index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name IN ('t_video', 't_follow')
GROUP BY table_name, index_name
ORDER BY table_name, index_name;
