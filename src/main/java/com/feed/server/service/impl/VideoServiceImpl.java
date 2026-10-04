package com.feed.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.feed.common.constant.EventType;
import com.feed.common.constant.MessageConstant;
import com.feed.common.exception.BaseException;
import com.feed.pojo.dto.VideoPublishDTO;
import com.feed.pojo.entity.OutboxMsg;
import com.feed.pojo.entity.Video;
import com.feed.pojo.vo.SearchVO;
import com.feed.pojo.vo.VideoVO;
import com.feed.server.mapper.OutboxMapper;
import com.feed.server.mapper.VideoMapper;
import com.feed.server.service.VideoCacheService;
import com.feed.server.service.VideoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 内容业务实现：发布采用本地事务消息表（Outbox）模式，
 * video 业务数据与 outbox 事件同事务提交，轮询器再异步投递 MQ
 */
@Service
@Slf4j
public class VideoServiceImpl implements VideoService {

    private static final int MAX_PAGE_SIZE = 50;

    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private OutboxMapper outboxMapper;
    @Autowired
    private VideoCacheService videoCacheService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${feed.feed.hot-keyword-key}")
    private String hotKeywordKey;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishVideo(Long userId, VideoPublishDTO dto) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 写业务数据（id 由雪花算法在 insert 前填充）
        Video video = Video.builder()
                .userId(userId)
                .title(dto.getTitle())
                .content(dto.getContent())
                .coverUrl(dto.getCoverUrl())
                .videoUrl(dto.getVideoUrl())
                .likesCount(0)
                .commentsCount(0)
                .createTime(now)
                .build();
        videoMapper.insert(video);

        // 2. 同事务写 Outbox 事件（轮询器投递成功前一直保留，服务重启也不丢）
        OutboxMsg outbox = OutboxMsg.builder()
                .videoId(video.getId())
                .eventType(EventType.VIDEO_PUBLISH)
                .status(EventType.STATUS_PENDING)
                .createTime(now)
                .build();
        outboxMapper.insert(outbox);

        log.info("内容发布成功：videoId={}, userId={}", video.getId(), userId);
        return video.getId();
    }

    @Override
    public VideoVO getVideoDetail(Long id) {
        if (id == null) {
            throw new BaseException(MessageConstant.VIDEO_NOT_FOUND);
        }
        // 复用三级缓存批量接口，单个 id 同样适用（L1 -> L2 -> L3）
        List<VideoVO> videos = videoCacheService.getVideoByIds(List.of(id));
        if (videos.isEmpty()) {
            throw new BaseException(MessageConstant.VIDEO_NOT_FOUND);
        }
        return videos.get(0);
    }

    @Override
    public SearchVO searchVideos(String keyword, int page, int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BaseException(MessageConstant.KEYWORD_EMPTY);
        }
        keyword = keyword.trim();
        final String kw = keyword;
        int pageNo = Math.max(page, 1);
        int pageSize = size <= 0 ? 10 : Math.min(size, MAX_PAGE_SIZE);

        // 标题或正文模糊匹配，按 id 倒序（雪花 id 随时间递增，等价于最新优先）
        LambdaQueryWrapper<Video> wrapper = Wrappers.lambdaQuery();
        wrapper.and(w -> w.like(Video::getTitle, kw)
                        .or()
                        .like(Video::getContent, kw))
                .orderByDesc(Video::getId);

        Page<Video> videoPage = new Page<>(pageNo, pageSize);
        videoMapper.selectPage(videoPage, wrapper);

        List<Long> ids = videoPage.getRecords().stream().map(Video::getId).toList();
        List<VideoVO> records = videoCacheService.getVideoByIds(ids);

        // 累计热搜词：ZINCRBY，每搜一次 score +1
        redisTemplate.opsForZSet().incrementScore(hotKeywordKey, keyword, 1.0);

        return SearchVO.builder()
                .records(records)
                .total(videoPage.getTotal())
                .page(videoPage.getCurrent())
                .size(videoPage.getSize())
                .pages(videoPage.getPages())
                .build();
    }

    @Override
    public List<String> listHotKeywords(int topN) {
        int limit = topN <= 0 ? 10 : Math.min(topN, 50);
        Set<Object> members = redisTemplate.opsForZSet()
                .reverseRange(hotKeywordKey, 0, limit - 1L);
        if (members == null || members.isEmpty()) {
            return new ArrayList<>();
        }
        return members.stream().map(String::valueOf).toList();
    }

    @Override
    public Map<String, Object> listByUser(Long authorId, int page, int size) {
        int pageNo = Math.max(page, 1);
        int pageSize = size <= 0 ? 20 : Math.min(size, MAX_PAGE_SIZE);

        // @TableLogic 自动追加 deleted=0，只查未删除的作品，按时间倒序
        Page<Video> videoPage = new Page<>(pageNo, pageSize);
        videoMapper.selectPage(videoPage, Wrappers.lambdaQuery(Video.class)
                .eq(Video::getUserId, authorId)
                .orderByDesc(Video::getId));

        List<Long> ids = videoPage.getRecords().stream().map(Video::getId).toList();
        List<VideoVO> records = videoCacheService.getVideoByIds(ids);

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", videoPage.getTotal());
        result.put("page", videoPage.getCurrent());
        result.put("size", videoPage.getSize());
        result.put("pages", videoPage.getPages());
        return result;
    }

    @Override
    public void deleteVideo(Long userId, Long videoId) {
        // selectById 被 @TableLogic 自动追加 deleted=0，已删除的内容查不到
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            throw new BaseException(MessageConstant.VIDEO_NOT_FOUND);
        }
        // 权限校验：只能删自己发布的内容
        if (!video.getUserId().equals(userId)) {
            throw new BaseException(MessageConstant.VIDEO_DELETE_FORBIDDEN);
        }

        // deleteById 被 @TableLogic 自动转成 UPDATE deleted=1，数据不物理消失
        videoMapper.deleteById(videoId);
        // 清除 L1/L2 缓存，防止删除后还能读到旧数据
        videoCacheService.evict(videoId);
        log.info("内容已软删除：videoId={}, userId={}", videoId, userId);
    }
}
