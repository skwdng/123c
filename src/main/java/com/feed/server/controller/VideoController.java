package com.feed.server.controller;

import com.feed.common.annotation.RateLimit;
import com.feed.common.annotation.RepeatSubmit;
import com.feed.common.context.BaseContext;
import com.feed.common.result.Result;
import com.feed.pojo.dto.VideoPublishDTO;
import com.feed.pojo.vo.SearchVO;
import com.feed.pojo.vo.VideoVO;
import com.feed.server.service.VideoService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内容相关接口
 */
@RestController
@RequestMapping("/video")
@Slf4j
public class VideoController {

    @Autowired
    private VideoService videoService;

    /**
     * 发布内容
     *
     * @param dto 标题/正文/封面URL/视频URL
     * @return 新内容id
     */
    @PostMapping("/publish")
    @RepeatSubmit(interval = 3000, message = "发布太频繁，请稍后再试")
    public Result<Map<String, Object>> publish(@RequestBody @Valid VideoPublishDTO dto) {
        Long userId = BaseContext.getCurrentId();
        log.info("发布内容：userId={}, title={}", userId, dto.getTitle());
        Long videoId = videoService.publishVideo(userId, dto);
        Map<String, Object> data = new HashMap<>();
        data.put("videoId", videoId);
        return Result.success(data);
    }

    /**
     * 视频详情（走三级缓存，含作者信息与计数）
     *
     * @param id 内容id
     */
    @GetMapping("/{id}")
    public Result<VideoVO> detail(@PathVariable Long id) {
        log.info("查询视频详情：id={}", id);
        return Result.success(videoService.getVideoDetail(id));
    }

    /**
     * 搜索视频（标题/正文关键词，分页）
     *
     * @param keyword 关键词
     * @param page    页码，默认 1
     * @param size    每页条数，默认 10，最大 50
     */
    @GetMapping("/search")
    @RateLimit(limit = 30, period = 60, message = "搜索太频繁，请稍后再试")
    public Result<SearchVO> search(@RequestParam String keyword,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        log.info("搜索视频：keyword={}, page={}, size={}", keyword, page, size);
        return Result.success(videoService.searchVideos(keyword, page, size));
    }

    /**
     * 热搜词榜单
     *
     * @param topN 前 N 个，默认 10
     */
    @GetMapping("/search/hot")
    public Result<List<String>> hotKeywords(@RequestParam(defaultValue = "10") int topN) {
        return Result.success(videoService.listHotKeywords(topN));
    }

    /**
     * 查询某个用户发布的内容列表（游客可访问，用于作者主页）
     *
     * @param userId 作者id
     * @param page   页码，默认 1
     * @param size   每页条数，默认 20
     */
    @GetMapping("/user/{userId}")
    public Result<Map<String, Object>> listByUser(@PathVariable Long userId,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        log.info("查询用户作品列表：authorId={}, page={}, size={}", userId, page, size);
        return Result.success(videoService.listByUser(userId, page, size));
    }

    /**
     * 删除内容（软删除，仅作者本人可删）
     *
     * @param id 内容id
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        Long userId = BaseContext.getCurrentId();
        log.info("删除内容：userId={}, videoId={}", userId, id);
        videoService.deleteVideo(userId, id);
        return Result.success("删除成功");
    }
}
