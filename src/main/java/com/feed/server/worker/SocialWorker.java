package com.feed.server.worker;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.feed.common.constant.EventType;
import com.feed.common.constant.MqConstants;
import com.feed.pojo.entity.Follow;
import com.feed.pojo.mq.EventMessage;
import com.feed.server.mapper.FollowMapper;
import com.feed.server.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 社交消费者：处理关注/取关。
 * 关注：写 t_follow（唯一键幂等）-> 双方计数 +1（通知由 NotificationWorker 统一处理）
 * 取关：删 t_follow -> 双方计数 -1（SQL 防负）
 */
@Component
@Slf4j
public class SocialWorker {

    @Autowired
    private FollowMapper followMapper;
    @Autowired
    private UserMapper userMapper;

    @RabbitListener(queues = MqConstants.SOCIAL_QUEUE)
    public void onSocialEvent(EventMessage event) {
        Long followerId = event.getUserId();
        Long followeeId = event.getTargetUserId();
        if (followerId == null || followeeId == null) {
            log.warn("社交事件参数缺失，丢弃：{}", event);
            return;
        }

        if (EventType.FOLLOW.equals(event.getEventType())) {
            handleFollow(followerId, followeeId);
        } else if (EventType.UNFOLLOW.equals(event.getEventType())) {
            handleUnfollow(followerId, followeeId);
        }
    }

    private void handleFollow(Long followerId, Long followeeId) {
        Follow follow = Follow.builder()
                .followerId(followerId)
                .followeeId(followeeId)
                .createTime(LocalDateTime.now())
                .build();
        try {
            followMapper.insert(follow);
        } catch (DuplicateKeyException e) {
            // 消息重投/重复关注：幂等返回，不重复计数与通知
            log.info("重复关注被幂等忽略：followerId={}, followeeId={}", followerId, followeeId);
            return;
        }

        userMapper.updateFollowingCount(followerId, 1);
        userMapper.updateFollowerCount(followeeId, 1);
        log.info("关注落库成功：followerId={}, followeeId={}", followerId, followeeId);
    }

    private void handleUnfollow(Long followerId, Long followeeId) {
        LambdaQueryWrapper<Follow> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Follow::getFollowerId, followerId)
                .eq(Follow::getFolloweeId, followeeId);
        int deleted = followMapper.delete(wrapper);

        if (deleted > 0) {
            userMapper.updateFollowingCount(followerId, -1);
            userMapper.updateFollowerCount(followeeId, -1);
            log.info("取关落库成功：followerId={}, followeeId={}", followerId, followeeId);
        } else {
            // 本来就没关注（消息重投），幂等返回
            log.info("取关时关系不存在，幂等忽略：followerId={}, followeeId={}", followerId, followeeId);
        }
    }
}
