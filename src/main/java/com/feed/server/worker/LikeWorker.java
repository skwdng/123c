package com.feed.server.worker;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.feed.common.constant.EventType;
import com.feed.common.constant.MqConstants;
import com.feed.common.util.RedisLockUtil;
import com.feed.pojo.entity.LikeRecord;
import com.feed.pojo.mq.EventMessage;
import com.feed.server.mapper.LikeMapper;
import com.feed.server.mapper.VideoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 点赞消费者：按事件类型分派
 * LIKE   -> 写 t_like（唯一键幂等）-> 点赞数 +1（通知由 NotificationWorker 统一处理）
 * UNLIKE -> 删 t_like -> 删除成功才点赞数 -1（幂等）
 */
@Component
@Slf4j
public class LikeWorker {

    // 点赞分布式锁 key 前缀：同一用户对同一内容的并发落库串行化
    private static final String LIKE_LOCK_PREFIX = "feed:like:lock:";

    @Autowired
    private LikeMapper likeMapper;
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private RedisLockUtil redisLockUtil;

    @RabbitListener(queues = MqConstants.LIKE_QUEUE)
    public void onLike(EventMessage event) {
        if (event == null) {
            return;
        }
        Long userId = event.getUserId();
        Long videoId = event.getVideoId();
        if (userId == null || videoId == null) {
            log.warn("点赞事件参数缺失，丢弃：{}", event);
            return;
        }
        if (EventType.LIKE.equals(event.getEventType())) {
            handleLike(event, userId, videoId);
        } else if (EventType.UNLIKE.equals(event.getEventType())) {
            handleUnlike(userId, videoId);
        }
    }

    /**
     * 点赞：分布式锁串行化 -> 写 t_like（唯一键幂等）-> 点赞数 +1
     * 锁失败不阻断：唯一键兜底幂等，最坏情况只是多一次冲突日志
     */
    private void handleLike(EventMessage event, Long userId, Long videoId) {
        String lockKey = LIKE_LOCK_PREFIX + userId + ":" + videoId;
        String lockValue = UUID.randomUUID().toString();
        // 最多等 10 次 x 100ms；锁 10 秒自动过期，防止持锁方宕机死锁
        boolean locked = redisLockUtil.tryLock(lockKey, lockValue, Duration.ofSeconds(10), 100, 10);
        if (!locked) {
            log.warn("点赞加锁失败，降级直接执行（唯一键兜底幂等）：userId={}, videoId={}", userId, videoId);
        }

        try {
            LikeRecord record = LikeRecord.builder()
                    .userId(userId)
                    .videoId(videoId)
                    .createTime(LocalDateTime.now())
                    .build();
            try {
                likeMapper.insert(record);
            } catch (DuplicateKeyException e) {
                // 消息重投/用户重复点赞：唯一键冲突即已点赞，幂等返回，不重复计数与通知
                log.info("重复点赞被幂等忽略：userId={}, videoId={}", userId, videoId);
                return;
            }

            videoMapper.incrementLikesCount(videoId);
            log.info("点赞落库成功：userId={}, videoId={}", userId, videoId);
        } finally {
            if (locked) {
                redisLockUtil.unlock(lockKey, lockValue);
            }
        }
    }

    /**
     * 取消点赞：删 t_like，删除成功才计数 -1，天然幂等
     */
    private void handleUnlike(Long userId, Long videoId) {
        LambdaQueryWrapper<LikeRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getVideoId, videoId);
        int deleted = likeMapper.delete(wrapper);
        if (deleted > 0) {
            videoMapper.decrementLikesCount(videoId);
            log.info("取消点赞落库成功：userId={}, videoId={}", userId, videoId);
        } else {
            // 无点赞记录：重复取消或消息重投，幂等忽略
            log.info("取消点赞幂等忽略（无点赞记录）：userId={}, videoId={}", userId, videoId);
        }
    }
}
