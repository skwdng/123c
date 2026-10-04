package com.feed.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.feed.pojo.vo.UserVO;

/**
 * 关注业务接口（接口层只投递 MQ，落库由 SocialWorker 异步完成）
 */
public interface FollowService {

    /**
     * 关注
     *
     * @param userId       粉丝id
     * @param targetUserId 博主id
     */
    void follow(Long userId, Long targetUserId);

    /**
     * 取关
     *
     * @param userId       粉丝id
     * @param targetUserId 博主id
     */
    void unfollow(Long userId, Long targetUserId);

    /**
     * 我的关注列表（我关注了谁，按关注时间倒序）
     *
     * @param userId   用户id
     * @param page     页码
     * @param pageSize 每页条数
     */
    Page<UserVO> followList(Long userId, Integer page, Integer pageSize);

    /**
     * 我的粉丝列表（谁关注了我，按关注时间倒序）
     *
     * @param userId   用户id
     * @param page     页码
     * @param pageSize 每页条数
     */
    Page<UserVO> fansList(Long userId, Integer page, Integer pageSize);
}
