package com.feed.server.worker;

import com.feed.common.constant.EventType;
import com.feed.common.constant.MqConstants;
import com.feed.pojo.entity.OutboxMsg;
import com.feed.pojo.entity.Video;
import com.feed.pojo.mq.EventMessage;
import com.feed.server.mapper.OutboxMapper;
import com.feed.server.mapper.VideoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Outbox 轮询器：每秒扫描 pending 消息投递到 MQ。
 * 投递成功 -> 删除记录；投递失败 -> 保留记录，下一轮自动重试
 */
@Component
@Slf4j
public class OutboxScheduler {

    @Autowired
    private OutboxMapper outboxMapper;
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * fixedDelay=1000：上一次执行结束后间隔 1 秒再执行，避免任务重叠
     */
    @Scheduled(fixedDelay = 1000)
    public void deliverPendingMessages() {
        List<OutboxMsg> pendingList = outboxMapper.selectPending();
        if (pendingList.isEmpty()) {
            return;
        }

        for (OutboxMsg outbox : pendingList) {
            try {
                Video video = videoMapper.selectById(outbox.getVideoId());
                if (video == null) {
                    // 内容不存在（异常数据），直接移除，避免毒消息反复重试
                    log.warn("Outbox 关联内容不存在，丢弃：{}", outbox.getVideoId());
                    outboxMapper.deleteById(outbox.getId());
                    continue;
                }

                EventMessage message = EventMessage.builder()
                        .eventType(outbox.getEventType() != null ? outbox.getEventType() : EventType.VIDEO_PUBLISH)
                        .videoId(video.getId())
                        .userId(video.getUserId())
                        .build();

                // Fanout 交换机广播给全部业务队列
                rabbitTemplate.convertAndSend(MqConstants.FEED_EXCHANGE, "", message);

                // 投递成功才删除；删除前若应用宕机，消息会被重新投递（至少一次）
                outboxMapper.deleteById(outbox.getId());
                log.info("Outbox 消息投递成功并删除：outboxId={}, videoId={}", outbox.getId(), video.getId());
            } catch (Exception e) {
                // 保留 pending 记录，下一秒重试
                log.error("Outbox 消息投递失败，保留待重试：outboxId={}, videoId={}, err={}",
                        outbox.getId(), outbox.getVideoId(), e.getMessage());
            }
        }
    }
}
