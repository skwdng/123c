package com.feed.common.constant;

/**
 * Feed 系统全部异步事件类型
 */
public class EventType {

    // 新内容发布（Outbox -> Fanout，TimelineWorker 消费）
    public static final String VIDEO_PUBLISH = "VIDEO_PUBLISH";

    // 点赞
    public static final String LIKE = "LIKE";

    // 取消点赞
    public static final String UNLIKE = "UNLIKE";

    // 评论
    public static final String COMMENT = "COMMENT";

    // 关注
    public static final String FOLLOW = "FOLLOW";

    // 取关
    public static final String UNFOLLOW = "UNFOLLOW";

    // Outbox 消息状态
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_SENT = "sent";
}
