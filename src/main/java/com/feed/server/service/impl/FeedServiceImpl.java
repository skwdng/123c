package com.feed.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.feed.common.util.TimeUtils;
import com.feed.pojo.entity.Video;
import com.feed.pojo.vo.FeedVO;
import com.feed.pojo.vo.LikeRankVO;
import com.feed.pojo.vo.VideoVO;
import com.feed.server.mapper.VideoMapper;
import com.feed.server.service.FeedService;
import com.feed.server.service.VideoCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Feed 流业务实现：
 * Redis ZSet 承担最新 1000 条热数据，MySQL 承担全部冷数据，
 * 服务层按 watermark（ZSet 最低分）无缝缝合；Redis 故障自动降级为纯 MySQL
 */
@Service
@Slf4j
public class FeedServiceImpl implements FeedService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private VideoCacheService videoCacheService;

    @Value("${feed.feed.timeline-key}")
    private String timelineKey;

    @Override
    public FeedVO listLatest(Long cursor, Integer limit) {
        int pageSize = normalizeLimit(limit);

        // 保序、去重的最终 id 集合
        LinkedHashSet<Long> idSet = new LinkedHashSet<>();
        Double hotLowestScore = null;

        // watermark = ZSet 中最小 score（冷热分界线）；Redis 异常时视为无热数据
        Double watermark = peekWatermarkSafely();

        // 首页（cursor=null）或游标仍在热区（cursor > watermark）时先查 Redis
        boolean queryHot = cursor == null || (watermark != null && cursor > watermark);
        if (queryHot) {
            try {
                // 游标页上界取 nextDown(毫秒整数)，等价于 score < cursor 的严格小于，避免边界重复
                double maxScore = cursor == null ? Double.POSITIVE_INFINITY : Math.nextDown(cursor.doubleValue());
                Set<ZSetOperations.TypedTuple<Object>> tuples =
                        redisTemplate.opsForZSet().reverseRangeByScoreWithScores(
                                timelineKey, 0d, maxScore, 0L, (long) pageSize);

                if (tuples != null) {
                    for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
                        if (tuple.getValue() != null) {
                            idSet.add(Long.valueOf(tuple.getValue().toString()));
                            hotLowestScore = tuple.getScore();
                        }
                    }
                }
            } catch (Exception e) {
                // Redis 不可用：降级走 MySQL
                log.warn("Redis 热数据读取失败，降级 MySQL：{}", e.getMessage());
                hotLowestScore = null;
            }
        }

        // 热数据不足一页，用冷数据缝合
        if (idSet.size() < pageSize) {
            long boundary;
            if (hotLowestScore != null) {
                // 从本页热数据最低分继续往下取，严格小于避免边界重复
                boundary = hotLowestScore.longValue();
            } else if (cursor != null) {
                // 纯冷数据翻页：沿用上页游标
                boundary = cursor;
            } else if (watermark != null) {
                // 首页且 Redis 没查到：从 watermark 之下开始
                boundary = watermark.longValue();
            } else {
                // Redis 完全不可用/为空：从当前时间开始
                boundary = System.currentTimeMillis();
            }

            int need = pageSize - idSet.size();
            List<Long> coldIds = videoMapper.selectColdVideoIds(boundary, need);
            idSet.addAll(coldIds);
        }

        // 三级缓存批量取详情，天然按 idSet 顺序返回
        List<VideoVO> videos = videoCacheService.getVideoByIds(idSet);

        Long nextCursor = null;
        if (!videos.isEmpty()) {
            nextCursor = TimeUtils.toMillis(videos.get(videos.size() - 1).getCreateTime());
        }
        return FeedVO.builder()
                .videos(videos)
                .nextCursor(nextCursor)
                .hasMore(videos.size() >= pageSize)
                .build();
    }

    @Override
    public LikeRankVO listByLikes(Long lastLikesCount, Long lastId, Integer limit) {
        int pageSize = normalizeLimit(limit);

        // 复合游标：likes_count < X，或 likes_count = X 且 id < Y
        LambdaQueryWrapper<Video> wrapper = Wrappers.lambdaQuery();
        if (lastLikesCount != null && lastId != null) {
            wrapper.and(w -> w.lt(Video::getLikesCount, lastLikesCount)
                    .or(o -> o.eq(Video::getLikesCount, lastLikesCount).lt(Video::getId, lastId)));
        }
        wrapper.orderByDesc(Video::getLikesCount)
                .orderByDesc(Video::getId)
                // pageSize 已在服务端归一化为 1~50 的 int，不存在 SQL 注入
                .last("LIMIT " + pageSize);

        List<Video> videos = videoMapper.selectList(wrapper);
        List<Long> ids = videos.stream().map(Video::getId).toList();
        List<VideoVO> videoVOS = videoCacheService.getVideoByIds(ids);

        Long nextLikesCount = null;
        Long nextId = null;
        if (!videos.isEmpty()) {
            Video last = videos.get(videos.size() - 1);
            nextLikesCount = last.getLikesCount().longValue();
            nextId = last.getId();
        }
        return LikeRankVO.builder()
                .videos(videoVOS)
                .lastLikesCount(nextLikesCount)
                .lastId(nextId)
                .hasMore(videos.size() >= pageSize)
                .build();
    }

    /**
     * 读取 ZSet 最小 score；任何异常都返回 null（触发降级）
     */
    private Double peekWatermarkSafely() {
        try {
            Set<ZSetOperations.TypedTuple<Object>> tuples =
                    redisTemplate.opsForZSet().rangeWithScores(timelineKey, 0, 0);
            if (tuples == null || tuples.isEmpty()) {
                return null;
            }
            return tuples.iterator().next().getScore();
        } catch (Exception e) {
            log.warn("读取 watermark 失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 分页参数归一化：默认 10，最小 1，最大 50
     */
    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
