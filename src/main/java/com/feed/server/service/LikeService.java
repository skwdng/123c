package com.feed.server.service;

/**
 * 点赞业务接口（接口层只投递 MQ，落库由 LikeWorker 异步完成）
 */
public interface LikeService {

    /**
     * 点赞
     *
     * @param userId  点赞用户id
     * @param videoId 内容id
     */
    void like(Long userId, Long videoId);

    /**
     * 取消点赞
     *
     * @param userId  点赞用户id
     * @param videoId 内容id
     */
    void cancelLike(Long userId, Long videoId);
}
