package com.feed.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.feed.common.constant.EventType;
import com.feed.common.constant.MessageConstant;
import com.feed.common.constant.MqConstants;
import com.feed.common.exception.BaseException;
import com.feed.pojo.entity.Follow;
import com.feed.pojo.entity.User;
import com.feed.pojo.mq.EventMessage;
import com.feed.pojo.vo.UserVO;
import com.feed.server.mapper.FollowMapper;
import com.feed.server.mapper.UserMapper;
import com.feed.server.service.FollowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 关注业务实现：接口只广播事件，关注关系与计数由 SocialWorker 异步处理
 */
@Service
@Slf4j
public class FollowServiceImpl implements FollowService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private FollowMapper followMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void follow(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new BaseException(MessageConstant.FOLLOW_YOURSELF);
        }
        if (userMapper.selectById(targetUserId) == null) {
            throw new BaseException(MessageConstant.USER_NOT_EXISTS);
        }

        EventMessage message = EventMessage.builder()
                .eventType(EventType.FOLLOW)
                .userId(userId)
                .targetUserId(targetUserId)
                .build();
        rabbitTemplate.convertAndSend(MqConstants.FEED_EXCHANGE, "", message);
        log.info("关注事件已投递：followerId={}, followeeId={}", userId, targetUserId);
    }

    @Override
    public void unfollow(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new BaseException(MessageConstant.FOLLOW_YOURSELF);
        }
        if (userMapper.selectById(targetUserId) == null) {
            throw new BaseException(MessageConstant.USER_NOT_EXISTS);
        }

        EventMessage message = EventMessage.builder()
                .eventType(EventType.UNFOLLOW)
                .userId(userId)
                .targetUserId(targetUserId)
                .build();
        rabbitTemplate.convertAndSend(MqConstants.FEED_EXCHANGE, "", message);
        log.info("取关事件已投递：followerId={}, followeeId={}", userId, targetUserId);
    }

    /**
     * 我的关注列表：查我发出的关注关系，再批量查对方资料
     */
    @Override
    public Page<UserVO> followList(Long userId, Integer page, Integer pageSize) {
        LambdaQueryWrapper<Follow> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Follow::getFollowerId, userId)
                .orderByDesc(Follow::getCreateTime)
                .orderByDesc(Follow::getId);
        return pageUsers(wrapper, page, pageSize, Follow::getFolloweeId);
    }

    /**
     * 我的粉丝列表：查指向我的关注关系，再批量查对方资料
     */
    @Override
    public Page<UserVO> fansList(Long userId, Integer page, Integer pageSize) {
        LambdaQueryWrapper<Follow> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Follow::getFolloweeId, userId)
                .orderByDesc(Follow::getCreateTime)
                .orderByDesc(Follow::getId);
        return pageUsers(wrapper, page, pageSize, Follow::getFollowerId);
    }

    /**
     * 分页查关注关系 → 批量查用户 → 组装 UserVO（关注/粉丝列表共用）
     *
     * @param wrapper     关注关系查询条件
     * @param page        页码
     * @param pageSize    每页条数
     * @param idExtractor 从关注关系中提取对方用户id（关注列表取 followeeId，粉丝列表取 followerId）
     */
    private Page<UserVO> pageUsers(LambdaQueryWrapper<Follow> wrapper, Integer page, Integer pageSize,
                                   Function<Follow, Long> idExtractor) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        Page<Follow> entityPage = followMapper.selectPage(new Page<>(pageNo, size), wrapper);

        // 批量查用户信息，避免逐条查库
        Set<Long> userIds = entityPage.getRecords().stream()
                .map(idExtractor)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty()
                ? new HashMap<>()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        Page<UserVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<UserVO> voList = entityPage.getRecords().stream()
                .map(f -> UserVO.from(userMap.get(idExtractor.apply(f))))
                .filter(vo -> vo != null)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }
}
