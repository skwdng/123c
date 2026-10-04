package com.feed.server.service.impl;

import com.feed.common.constant.EventType;
import com.feed.common.constant.MessageConstant;
import com.feed.common.constant.MqConstants;
import com.feed.common.exception.BaseException;
import com.feed.pojo.entity.Video;
import com.feed.pojo.mq.EventMessage;
import com.feed.server.mapper.VideoMapper;
import com.feed.server.service.LikeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 点赞业务实现：接口只做参数校验 + 广播事件，立即返回，落库与计数由 Worker 异步处理
 */
@Service
@Slf4j
public class LikeServiceImpl implements LikeService {

    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void like(Long userId, Long videoId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            throw new BaseException(MessageConstant.VIDEO_NOT_FOUND);
        }

        EventMessage message = EventMessage.builder()
                .eventType(EventType.LIKE)
                .videoId(videoId)
                .userId(userId)
                .targetUserId(video.getUserId())
                .build();
        // 广播到 Fanout，各队列按 eventType 自行过滤
        rabbitTemplate.convertAndSend(MqConstants.FEED_EXCHANGE, "", message);
        log.info("点赞事件已投递：userId={}, videoId={}", userId, videoId);
    }

    @Override
    public void cancelLike(Long userId, Long videoId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            throw new BaseException(MessageConstant.VIDEO_NOT_FOUND);
        }

        EventMessage message = EventMessage.builder()
                .eventType(EventType.UNLIKE)
                .videoId(videoId)
                .userId(userId)
                .build();
        // 同样走 Fanout 广播，LikeWorker 按 eventType 分派处理
        rabbitTemplate.convertAndSend(MqConstants.FEED_EXCHANGE, "", message);
        log.info("取消点赞事件已投递：userId={}, videoId={}", userId, videoId);
    }
}
