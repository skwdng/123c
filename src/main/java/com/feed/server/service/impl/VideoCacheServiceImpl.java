package com.feed.server.service.impl;

import com.feed.pojo.entity.User;
import com.feed.pojo.entity.Video;
import com.feed.pojo.vo.VideoVO;
import com.feed.server.mapper.UserMapper;
import com.feed.server.mapper.VideoMapper;
import com.feed.server.service.VideoCacheService;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 视频详情三级缓存实现
 */
@Service
@Slf4j
public class VideoCacheServiceImpl implements VideoCacheService {

    @Autowired
    private Cache<Long, VideoVO> videoL1Cache;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private UserMapper userMapper;

    @Value("${feed.feed.video-cache-prefix}")
    private String cachePrefix;
    @Value("${feed.feed.video-cache-ttl-hours}")
    private long cacheTtlHours;

    // singleflight：同一个视频正在回源时，后续请求挂到同一个 Future 上
    private final ConcurrentHashMap<Long, CompletableFuture<VideoVO>> inflight = new ConcurrentHashMap<>();

    // 异步回写 L1/L2 的单线程池，避免回写拖慢主请求
    private final ExecutorService writeBackExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "video-cache-writeback");
        t.setDaemon(true);
        return t;
    });

    @Override
    public List<VideoVO> getVideoByIds(Collection<Long> rawIds) {
        if (rawIds == null || rawIds.isEmpty()) {
            return Collections.emptyList();
        }
        // 去重，保持顺序
        List<Long> ids = new ArrayList<>(new LinkedHashSetSafe<>(rawIds).get());
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, VideoVO> result = new HashMap<>();

        // ---------- L1：Caffeine 本地缓存 ----------
        Map<Long, VideoVO> l1 = videoL1Cache.getAllPresent(ids);
        result.putAll(l1);
        List<Long> missL1 = ids.stream().filter(id -> !l1.containsKey(id)).collect(Collectors.toList());
        if (missL1.isEmpty()) {
            return orderByIds(ids, result);
        }

        // ---------- L2：Redis 批量 MGet ----------
        List<String> redisKeys = missL1.stream().map(id -> cachePrefix + id).collect(Collectors.toList());
        List<Object> l2Raw;
        try {
            l2Raw = redisTemplate.opsForValue().multiGet(redisKeys);
        } catch (Exception e) {
            // Redis 异常时降级直接走 MySQL，不影响接口可用性
            // 置 null 而非空列表：下方 if(l2Raw != null) 会整体跳过，避免对空列表按下标取值
            log.warn("L2 Redis MGet 失败，降级直查 MySQL：{}", e.getMessage());
            l2Raw = null;
        }
        if (l2Raw != null) {
            for (int i = 0; i < missL1.size(); i++) {
                Object cached = l2Raw.get(i);
                if (cached instanceof VideoVO vo) {
                    result.put(missL1.get(i), vo);
                    // 回填 L1
                    videoL1Cache.put(missL1.get(i), vo);
                }
            }
        }
        List<Long> missL2 = missL1.stream().filter(id -> !result.containsKey(id)).collect(Collectors.toList());
        if (missL2.isEmpty()) {
            return orderByIds(ids, result);
        }

        // ---------- L3：MySQL + singleflight 防穿透 ----------
        // 每个未命中 id 对应一个 Future；首次出现的由当前线程负责这一批 DB 查询
        Map<Long, CompletableFuture<VideoVO>> ownFutures = new HashMap<>();
        Map<Long, CompletableFuture<VideoVO>> allFutures = new HashMap<>();
        for (Long id : missL2) {
            CompletableFuture<VideoVO> future = new CompletableFuture<>();
            CompletableFuture<VideoVO> existing = inflight.putIfAbsent(id, future);
            if (existing == null) {
                ownFutures.put(id, future);
                allFutures.put(id, future);
            } else {
                // 已有其他线程在回源同一个视频，挂到它的 Future 上等待
                allFutures.put(id, existing);
            }
        }

        if (!ownFutures.isEmpty()) {
            // 同步执行这批 DB 查询，内部会 complete 自己持有的 Future
            loadFromDbAndComplete(ownFutures);
        }
        allFutures.forEach((id, future) -> {
            try {
                VideoVO vo = future.get(2, TimeUnit.SECONDS);
                if (vo != null) {
                    result.put(id, vo);
                }
            } catch (Exception e) {
                log.error("等待视频回源结果失败：videoId={}, err={}", id, e.getMessage());
            }
        });

        return orderByIds(ids, result);
    }

    @Override
    public void evict(Long videoId) {
        if (videoId == null) {
            return;
        }
        // 先清 L1 本地缓存，再删 L2 Redis；Redis 异常只告警不阻断（L2 最多 1 小时也会自然过期）
        videoL1Cache.invalidate(videoId);
        try {
            redisTemplate.delete(cachePrefix + videoId);
        } catch (Exception e) {
            log.warn("清除 L2 缓存失败：videoId={}, err={}", videoId, e.getMessage());
        }
        log.info("视频缓存已清除：videoId={}", videoId);
    }

    /**
     * 一批 id 一次性查 MySQL（视频 + 作者），完成后异步回写缓存
     */
    private void loadFromDbAndComplete(Map<Long, CompletableFuture<VideoVO>> ownFutures) {
        Set<Long> ownIds = ownFutures.keySet();
        try {
            List<Video> videos = videoMapper.selectBatchIds(ownIds);
            // 批量补作者信息
            Set<Long> authorIds = videos.stream().map(Video::getUserId).collect(Collectors.toSet());
            Map<Long, User> authorMap = authorIds.isEmpty()
                    ? Collections.emptyMap()
                    : userMapper.selectBatchIds(authorIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

            Map<Long, VideoVO> loaded = new HashMap<>();
            for (Video video : videos) {
                User author = authorMap.get(video.getUserId());
                VideoVO vo = VideoVO.builder()
                        .id(video.getId())
                        .userId(video.getUserId())
                        .title(video.getTitle())
                        .content(video.getContent())
                        .coverUrl(video.getCoverUrl())
                        .videoUrl(video.getVideoUrl())
                        .likesCount(video.getLikesCount())
                        .commentsCount(video.getCommentsCount())
                        .createTime(video.getCreateTime())
                        .authorNickname(author == null ? null : author.getNickname())
                        .authorAvatarUrl(author == null ? null : author.getAvatarUrl())
                        .build();
                loaded.put(video.getId(), vo);
            }

            // 完成 Future（数据库不存在的 id 用 null 完成）
            ownIds.forEach(id -> {
                VideoVO vo = loaded.get(id);
                ownFutures.get(id).complete(vo);
            });

            // 异步回写 L2(1h) 与 L1(3s)
            if (!loaded.isEmpty()) {
                writeBackExecutor.submit(() -> writeBack(loaded));
            }
        } catch (Exception e) {
            log.error("L3 MySQL 批量查询视频失败：ids={}", ownIds, e);
            ownFutures.values().forEach(f -> f.completeExceptionally(e));
        } finally {
            ownIds.forEach(inflight::remove);
        }
    }

    /**
     * 回写 Redis（1 小时）与 Caffeine（3 秒）
     */
    private void writeBack(Map<Long, VideoVO> loaded) {
        try {
            loaded.forEach((id, vo) -> {
                try {
                    redisTemplate.opsForValue().set(cachePrefix + id, vo, Duration.ofHours(cacheTtlHours));
                    videoL1Cache.put(id, vo);
                } catch (Exception ex) {
                    log.warn("视频缓存回写失败：videoId={}, err={}", id, ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("缓存回写异常：{}", e.getMessage());
        }
    }

    /**
     * 按入参 id 顺序输出
     */
    private List<VideoVO> orderByIds(List<Long> ids, Map<Long, VideoVO> result) {
        List<VideoVO> ordered = new ArrayList<>(ids.size());
        for (Long id : ids) {
            VideoVO vo = result.get(id);
            if (vo != null) {
                ordered.add(vo);
            }
        }
        return ordered;
    }

    /**
     * 去重保序小工具
     */
    private static class LinkedHashSetSafe<T> {
        private final List<T> list;

        LinkedHashSetSafe(Collection<T> source) {
            Set<T> seen = new HashSet<>();
            list = new ArrayList<>();
            for (T t : source) {
                if (t != null && seen.add(t)) {
                    list.add(t);
                }
            }
        }

        List<T> get() {
            return list;
        }
    }
}
