package com.feed.server.worker;

import com.feed.common.constant.EventType;
import com.feed.common.constant.MqConstants;
import com.feed.pojo.entity.Comment;
import com.feed.pojo.mq.EventMessage;
import com.feed.server.mapper.CommentMapper;
import com.feed.server.mapper.VideoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 评论消费者：
 * 写 t_comment -> 评论数 +1（通知由 NotificationWorker 统一处理）
 */
@Component
@Slf4j
public class CommentWorker {

    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private VideoMapper videoMapper;

    @RabbitListener(queues = MqConstants.COMMENT_QUEUE)
    public void onComment(EventMessage event) {
        if (!EventType.COMMENT.equals(event.getEventType())) {
            return;
        }
        Long userId = event.getUserId();
        Long videoId = event.getVideoId();
        if (userId == null || videoId == null || event.getContent() == null) {
            log.warn("评论事件参数缺失，丢弃：{}", event);
            return;
        }

        Comment comment = Comment.builder()
                .videoId(videoId)
                .userId(userId)
                .content(event.getContent())
                .createTime(LocalDateTime.now())
                .build();
        commentMapper.insert(comment);

        videoMapper.incrementCommentsCount(videoId);
        log.info("评论落库成功：commentId={}, userId={}, videoId={}", comment.getId(), userId, videoId);
    }
}
