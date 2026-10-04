package com.feed.common.constant;

/**
 * RabbitMQ 交换机 / 队列名称常量
 * 与 application.yml 中 feed.rabbitmq 段保持一致
 */
public class MqConstants {

    // 发布事件 Fanout 交换机
    public static final String FEED_EXCHANGE = "feed.exchange";

    // 死信交换机
    public static final String FEED_DLX_EXCHANGE = "feed.dlx.exchange";

    // 五个业务队列
    public static final String TIMELINE_QUEUE = "feed.timeline.queue";
    public static final String LIKE_QUEUE = "feed.like.queue";
    public static final String COMMENT_QUEUE = "feed.comment.queue";
    public static final String SOCIAL_QUEUE = "feed.social.queue";
    public static final String NOTIFICATION_QUEUE = "feed.notification.queue";

    // 死信队列
    public static final String DLX_QUEUE = "feed.dlx.queue";
}
