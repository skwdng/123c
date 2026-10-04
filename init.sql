-- ============================================================
-- Java Feed 流项目初始化脚本
-- 5 张核心表：t_user / t_video / t_follow / t_like / t_outbox_msg
-- 阶段5配套表：t_comment / t_notification
-- 字符集统一 utf8mb4，支持 emoji 昵称/内容
-- ============================================================

CREATE DATABASE IF NOT EXISTS cfeed
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE cfeed;

-- ------------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_user (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    uid             CHAR(12)     NOT NULL COMMENT '对外展示的唯一uid（12位随机数字，前补0）',
    phone           VARCHAR(20)  NOT NULL COMMENT '手机号（唯一，登录凭证）',
    password        VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密）',
    nickname        VARCHAR(50)  DEFAULT NULL COMMENT '昵称（允许重复）',
    avatar_url      VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
    following_count INT          NOT NULL DEFAULT 0 COMMENT '关注数',
    follower_count  INT          NOT NULL DEFAULT 0 COMMENT '粉丝数',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_uid (uid),
    UNIQUE KEY uk_phone (phone)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- ------------------------------------------------------------
-- 2. 内容（视频/动态）表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_video (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id        BIGINT       NOT NULL COMMENT '发布者id',
    title          VARCHAR(100) NOT NULL COMMENT '标题',
    content        VARCHAR(1000) DEFAULT NULL COMMENT '正文',
    cover_url      VARCHAR(255) DEFAULT NULL COMMENT '封面URL',
    video_url      VARCHAR(255) NOT NULL COMMENT '视频URL',
    likes_count    INT          NOT NULL DEFAULT 0 COMMENT '点赞数',
    comments_count INT          NOT NULL DEFAULT 0 COMMENT '评论数',
    deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除标记：0 正常 1 已删除',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    -- 个人主页作品列表：WHERE user_id=? ORDER BY create_time DESC（复合索引覆盖单列 user_id 查询）
    KEY idx_user_time (user_id, create_time),
    KEY idx_create_time (create_time),
    -- 点赞榜复合游标分页：(likes_count DESC, id DESC)
    KEY idx_likes (likes_count DESC, id DESC)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '内容表';

-- ------------------------------------------------------------
-- 3. 关注关系表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_follow (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    follower_id BIGINT   NOT NULL COMMENT '关注者id（粉丝）',
    followee_id BIGINT   NOT NULL COMMENT '被关注者id（博主）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    -- 防止重复关注
    UNIQUE KEY uk_follower_followee (follower_id, followee_id),
    -- 粉丝列表：WHERE followee_id=? ORDER BY create_time DESC
    KEY idx_followee_time (followee_id, create_time),
    -- 关注列表：WHERE follower_id=? ORDER BY create_time DESC
    KEY idx_follower_time (follower_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '关注关系表';

-- ------------------------------------------------------------
-- 4. 点赞表（一人一赞）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_like (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT   NOT NULL COMMENT '点赞用户id',
    video_id    BIGINT   NOT NULL COMMENT '内容id',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    -- 同一用户对同一内容只能点赞一次
    UNIQUE KEY uk_user_video (user_id, video_id),
    KEY idx_video (video_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '点赞表';

-- ------------------------------------------------------------
-- 5. Outbox 发件箱消息表（保证发布事件不丢）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_outbox_msg (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    video_id    BIGINT      NOT NULL COMMENT '内容id',
    event_type  VARCHAR(30) NOT NULL COMMENT '事件类型，如 VIDEO_PUBLISH',
    status      VARCHAR(10) NOT NULL DEFAULT 'pending' COMMENT '状态：pending 待投递 / sent 已投递',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    -- 供轮询器按状态+时间捞取待投递消息
    KEY idx_status_time (status, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Outbox发件箱消息表';

-- ------------------------------------------------------------
-- 6. 评论表（阶段5 Worker 异步落库）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_comment (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    video_id    BIGINT        NOT NULL COMMENT '内容id',
    user_id     BIGINT        NOT NULL COMMENT '评论用户id',
    content     VARCHAR(500)  NOT NULL COMMENT '评论内容',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_video_time (video_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '评论表';

-- ------------------------------------------------------------
-- 7. 通知表（阶段5 NotificationWorker 落库 + SSE 推送）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_notification (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id      BIGINT       NOT NULL COMMENT '接收通知的用户id',
    from_user_id BIGINT       DEFAULT NULL COMMENT '触发通知的用户id',
    type         VARCHAR(20)  NOT NULL COMMENT '类型：LIKE / COMMENT / FOLLOW',
    content      VARCHAR(255) DEFAULT NULL COMMENT '通知文案',
    video_id     BIGINT       DEFAULT NULL COMMENT '关联内容id',
    is_read      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：0 未读 1 已读',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_read_time (user_id, is_read, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知表';
