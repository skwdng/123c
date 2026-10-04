package com.feed.server.worker;

import com.feed.common.constant.EventType;
import com.feed.common.constant.MqConstants;
import com.feed.pojo.entity.Notification;
import com.feed.pojo.mq.EventMessage;
import com.feed.server.mapper.NotificationMapper;
import com.feed.server.sse.SseEmitterManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 通知消费者：落库 t_notification，并通过 SSE 实时推送给在线用户。
 * 通知队列绑定在 Fanout 交换机上，所有事件都会广播过来，
 * 这里按 eventType 过滤 + 统一生成通知文案，各业务 Worker 不再单独转发通知。
 * 用户不在线时推送静默跳过，但通知已持久化，前端可通过列表接口补拉
 */
@Component
@Slf4j
public class NotificationWorker {

    // 需要产生通知的事件类型
    private static final Set<String> NOTIFIABLE = Set.of(EventType.LIKE, EventType.COMMENT, EventType.FOLLOW);

    // 通知文案里评论摘要的最大长度
    private static final int NOTIFY_CONTENT_MAX = 200;

    @Autowired
    private NotificationMapper notificationMapper;
    @Autowired
    private SseEmitterManager sseEmitterManager;

    @RabbitListener(queues = MqConstants.NOTIFICATION_QUEUE)
    public void onNotification(EventMessage event) {
        if (!NOTIFIABLE.contains(event.getEventType())) {
            return;
        }
        Long receiverId = event.getTargetUserId();
        Long senderId = event.getUserId();
        if (receiverId == null || senderId == null) {
            log.warn("通知事件缺少接收者，丢弃：{}", event);
            return;
        }
        // 自己对自己内容的操作（自赞/自评论/关注自己）不产生通知
        if (receiverId.equals(senderId)) {
            return;
        }

        Notification notification = Notification.builder()
                .userId(receiverId)
                .fromUserId(senderId)
                .type(event.getEventType())
                .content(buildContent(event))
                .videoId(event.getVideoId())
                .isRead(0)
                .createTime(LocalDateTime.now())
                .build();
        notificationMapper.insert(notification);
        log.info("通知落库成功：id={}, receiverId={}, type={}",
                notification.getId(), receiverId, event.getEventType());

        // SSE 实时推送（不在线则跳过）
        sseEmitterManager.sendToUser(receiverId, "notification", notification);
    }

    /**
     * 按事件类型生成通知文案
     */
    private String buildContent(EventMessage event) {
        switch (event.getEventType()) {
            case EventType.LIKE:
                return "赞了你的内容";
            case EventType.COMMENT:
                String raw = event.getContent() == null ? "" : event.getContent();
                String brief = raw.length() > NOTIFY_CONTENT_MAX ? raw.substring(0, NOTIFY_CONTENT_MAX) : raw;
                return "评论了你：" + brief;
            case EventType.FOLLOW:
                return "关注了你";
            default:
                return event.getContent();
        }
    }
}
