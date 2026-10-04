package com.feed.server.service;

import com.feed.pojo.vo.FeedVO;
import com.feed.pojo.vo.LikeRankVO;

/**
 * Feed 流业务接口
 */
public interface FeedService {

    /**
     * 时间线 Feed（冷热分离）：
     * 游标大于 watermark 走 Redis ZSet，不足部分用 MySQL 冷数据缝合；
     * 游标小于等于 watermark 直接查 MySQL
     *
     * @param cursor 上一页最后一条的发布时间毫秒值；首页传 null
     * @param limit  每页条数
     * @return 视频列表 + 下一页游标
     */
    FeedVO listLatest(Long cursor, Integer limit);

    /**
     * 点赞榜（复合游标分页）：游标为 (likes_count, id)
     *
     * @param lastLikesCount 上一页最后一条点赞数；首页传 null
     * @param lastId          上一页最后一条内容id；首页传 null
     * @param limit           每页条数
     * @return 视频列表 + 下一页复合游标
     */
    LikeRankVO listByLikes(Long lastLikesCount, Long lastId, Integer limit);
}
