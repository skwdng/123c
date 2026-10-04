package com.feed.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.feed.common.constant.MessageConstant;
import com.feed.common.exception.BaseException;
import com.feed.pojo.entity.Notification;
import com.feed.pojo.entity.User;
import com.feed.pojo.vo.NotificationVO;
import com.feed.server.mapper.NotificationMapper;
import com.feed.server.mapper.UserMapper;
import com.feed.server.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通知业务实现：分页查询 + 触发者信息批量冗余填充
 */
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public Page<NotificationVO> list(Long userId, Integer isRead, Integer page, Integer pageSize) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);

        LambdaQueryWrapper<Notification> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Notification::getUserId, userId);
        if (isRead != null) {
            wrapper.eq(Notification::getIsRead, isRead);
        }
        wrapper.orderByDesc(Notification::getCreateTime);

        Page<Notification> entityPage = notificationMapper.selectPage(new Page<>(pageNo, size), wrapper);

        // 批量查触发者信息，避免逐条查库
        Set<Long> fromUserIds = entityPage.getRecords().stream()
                .map(Notification::getFromUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = fromUserIds.isEmpty()
                ? new HashMap<>()
                : userMapper.selectBatchIds(fromUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        Page<NotificationVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<NotificationVO> voList = entityPage.getRecords().stream().map(n -> {
            User fromUser = n.getFromUserId() == null ? null : userMap.get(n.getFromUserId());
            return NotificationVO.builder()
                    .id(n.getId())
                    .userId(n.getUserId())
                    .fromUserId(n.getFromUserId())
                    .fromNickname(fromUser == null ? null : fromUser.getNickname())
                    .fromAvatarUrl(fromUser == null ? null : fromUser.getAvatarUrl())
                    .type(n.getType())
                    .content(n.getContent())
                    .videoId(n.getVideoId())
                    .isRead(n.getIsRead())
                    .createTime(n.getCreateTime())
                    .build();
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public void markRead(Long userId, Long id) {
        Notification notification = notificationMapper.selectById(id);
        // 通知不存在或不属于当前用户：防止越权标记别人的通知
        if (notification == null || !userId.equals(notification.getUserId())) {
            throw new BaseException(MessageConstant.NOTIFICATION_NOT_FOUND);
        }
        if (notification.getIsRead() != null && notification.getIsRead() == 1) {
            // 已经是已读状态：幂等返回，不重复写库
            log.info("通知已是已读状态，幂等忽略：userId={}, notificationId={}", userId, id);
            return;
        }
        Notification update = new Notification();
        update.setId(id);
        update.setIsRead(1);
        notificationMapper.updateById(update);
        log.info("通知标记已读：userId={}, notificationId={}", userId, id);
    }

    @Override
    public void markAllRead(Long userId) {
        // 一条 SQL 批量标记，不逐条查库
        LambdaUpdateWrapper<Notification> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1);
        int count = notificationMapper.update(null, wrapper);
        log.info("全部通知标记已读：userId={}, 更新条数={}", userId, count);
    }

    @Override
    public Long unreadCount(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0);
        return notificationMapper.selectCount(wrapper);
    }
}
