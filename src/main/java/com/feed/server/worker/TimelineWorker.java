package com.feed.server.worker;

import com.feed.common.constant.EventType;
import com.feed.common.constant.MqConstants;
import com.feed.common.util.TimeUtils;
import com.feed.pojo.entity.Video;
import com.feed.pojo.mq.EventMessage;
import com.feed.server.mapper.VideoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Timeline 消费者：新内容发布事件写入 Redis ZSet 全局 Timeline。
 * score = 发布时间毫秒值，member = 视频id；只保留最新的 N 条热数据
 */
@Component
@Slf4j
public class TimelineWorker {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private VideoMapper videoMapper;

    @Value("${feed.feed.timeline-key}")
    private String timelineKey;
    @Value("${feed.feed.timeline-max-size}")
    private long maxSize;

    @RabbitListener(queues = MqConstants.TIMELINE_QUEUE)
    public void onPublish(EventMessage event) {
        // Fanout 广播过来的其它事件直接忽略
        if (!EventType.VIDEO_PUBLISH.equals(event.getEventType())) {
            return;
        }
        Long videoId = event.getVideoId();
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            log.warn("Timeline 收到不存在的视频事件：videoId={}", videoId);
            return;
        }

        long score = TimeUtils.toMillis(video.getCreateTime());
        // member 统一用字符串，避免 JSON 反序列化 Long/Integer 歧义
        redisTemplate.opsForZSet().add(timelineKey, String.valueOf(videoId), score);

        // 淘汰旧数据：排名 0 ~ -(maxSize+1) 全部移除，只保留最新 maxSize 条
        redisTemplate.opsForZSet().removeRange(timelineKey, 0, -maxSize - 1);

        Long size = redisTemplate.opsForZSet().zCard(timelineKey);
        log.info("Timeline 已写入：videoId={}, score={}, 当前热数据={}条", videoId, score, size);
    }

    /**
     * 仅供排查：查看当前 ZSet 最小 score（冷热分界线 watermark）
     */
    public Double peekWatermark() {
        Set<ZSetOperations.TypedTuple<Object>> tuples =
                redisTemplate.opsForZSet().rangeWithScores(timelineKey, 0, 0);
        if (tuples == null || tuples.isEmpty()) {
            return null;
        }
        return tuples.iterator().next().getScore();
    }
}
