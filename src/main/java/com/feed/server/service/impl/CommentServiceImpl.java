package com.feed.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.feed.common.constant.EventType;
import com.feed.common.constant.MessageConstant;
import com.feed.common.constant.MqConstants;
import com.feed.common.exception.BaseException;
import com.feed.pojo.dto.CommentDTO;
import com.feed.pojo.entity.Comment;
import com.feed.pojo.entity.User;
import com.feed.pojo.entity.Video;
import com.feed.pojo.mq.EventMessage;
import com.feed.pojo.vo.CommentVO;
import com.feed.server.mapper.CommentMapper;
import com.feed.server.mapper.UserMapper;
import com.feed.server.mapper.VideoMapper;
import com.feed.server.service.CommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评论业务实现：发表只广播事件，落库与计数由 CommentWorker 异步处理；列表为同步分页查询
 */
@Service
@Slf4j
public class CommentServiceImpl implements CommentService {

    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void comment(Long userId, CommentDTO dto) {
        Video video = videoMapper.selectById(dto.getVideoId());
        if (video == null) {
            throw new BaseException(MessageConstant.VIDEO_NOT_FOUND);
        }

        EventMessage message = EventMessage.builder()
                .eventType(EventType.COMMENT)
                .videoId(dto.getVideoId())
                .userId(userId)
                .targetUserId(video.getUserId())
                .content(dto.getContent())
                .build();
        rabbitTemplate.convertAndSend(MqConstants.FEED_EXCHANGE, "", message);
        log.info("评论事件已投递：userId={}, videoId={}", userId, dto.getVideoId());
    }

    @Override
    public Page<CommentVO> listByVideo(Long videoId, Integer page, Integer pageSize) {
        if (videoMapper.selectById(videoId) == null) {
            throw new BaseException(MessageConstant.VIDEO_NOT_FOUND);
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);

        LambdaQueryWrapper<Comment> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Comment::getVideoId, videoId)
                .orderByDesc(Comment::getCreateTime)
                .orderByDesc(Comment::getId);
        Page<Comment> entityPage = commentMapper.selectPage(new Page<>(pageNo, size), wrapper);

        // 批量查评论者信息，避免逐条查库
        Set<Long> userIds = entityPage.getRecords().stream()
                .map(Comment::getUserId)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty()
                ? new HashMap<>()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        Page<CommentVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<CommentVO> voList = entityPage.getRecords().stream().map(c -> {
            User user = userMap.get(c.getUserId());
            return CommentVO.builder()
                    .id(c.getId())
                    .videoId(c.getVideoId())
                    .userId(c.getUserId())
                    .nickname(user == null ? null : user.getNickname())
                    .avatarUrl(user == null ? null : user.getAvatarUrl())
                    .content(c.getContent())
                    .createTime(c.getCreateTime())
                    .build();
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }
}
