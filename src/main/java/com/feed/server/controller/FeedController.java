package com.feed.server.controller;

import com.feed.common.result.Result;
import com.feed.pojo.vo.FeedVO;
import com.feed.pojo.vo.LikeRankVO;
import com.feed.server.service.FeedService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Feed 流接口：时间线（冷热分离）+ 点赞榜（复合游标）
 */
@RestController
@RequestMapping("/video/feed")
@Slf4j
public class FeedController {

    @Autowired
    private FeedService feedService;

    /**
     * 最新 Feed 列表
     *
     * @param cursor 上一页最后一条发布时间毫秒值，首页不传
     * @param limit  每页条数，默认 10，最大 50
     */
    @GetMapping("/listLatest")
    public Result<FeedVO> listLatest(@RequestParam(required = false) Long cursor,
                                     @RequestParam(required = false) Integer limit) {
        log.info("拉取最新 Feed：cursor={}, limit={}", cursor, limit);
        return Result.success(feedService.listLatest(cursor, limit));
    }

    /**
     * 点赞榜列表（复合游标分页）
     *
     * @param lastLikesCount 上一页最后一条点赞数，首页不传
     * @param lastId          上页最后一条内容id，首页不传
     * @param limit           每页条数，默认 10，最大 50
     */
    @GetMapping("/listByLikes")
    public Result<LikeRankVO> listByLikes(@RequestParam(required = false) Long lastLikesCount,
                                          @RequestParam(required = false) Long lastId,
                                          @RequestParam(required = false) Integer limit) {
        log.info("拉取点赞榜：lastLikesCount={}, lastId={}, limit={}", lastLikesCount, lastId, limit);
        return Result.success(feedService.listByLikes(lastLikesCount, lastId, limit));
    }
}
