package com.feed.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.feed.pojo.vo.NotificationVO;

/**
 * 通知业务接口
 */
public interface NotificationService {

    /**
     * 分页查询当前用户通知（按时间倒序），可按已读状态过滤
     *
     * @param userId 接收者id
     * @param isRead 已读状态：null 全部 / 0 未读 / 1 已读
     * @param page   页码，从 1 开始
     * @param pageSize 每页条数
     * @return 通知分页
     */
    Page<NotificationVO> list(Long userId, Integer isRead, Integer page, Integer pageSize);

    /**
     * 标记单条通知已读（仅限本人通知，重复标记幂等）
     *
     * @param userId 当前用户id
     * @param id     通知id
     */
    void markRead(Long userId, Long id);

    /**
     * 一键标记当前用户全部未读通知为已读
     *
     * @param userId 当前用户id
     */
    void markAllRead(Long userId);

    /**
     * 统计当前用户未读通知数量（前端小红点角标）
     *
     * @param userId 当前用户id
     * @return 未读条数
     */
    Long unreadCount(Long userId);
}
