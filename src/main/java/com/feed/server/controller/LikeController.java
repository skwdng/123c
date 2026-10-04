package com.feed.server.controller;

import com.feed.common.annotation.RepeatSubmit;
import com.feed.common.context.BaseContext;
import com.feed.common.result.Result;
import com.feed.server.service.LikeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 点赞接口
 */
@RestController
@RequestMapping("/like")
@Slf4j
public class LikeController {

    @Autowired
    private LikeService likeService;

    /**
     * 点赞（异步处理，接口立即返回）
     *
     * @param videoId 内容id
     */
    @PostMapping("/video/{videoId}")
    @RepeatSubmit(interval = 3000, message = "操作太频繁，请稍后再试")
    public Result<String> like(@PathVariable Long videoId) {
        Long userId = BaseContext.getCurrentId();
        log.info("点赞：userId={}, videoId={}", userId, videoId);
        likeService.like(userId, videoId);
        return Result.success("操作成功");
    }

    /**
     * 取消点赞（异步处理，接口立即返回）
     *
     * @param videoId 内容id
     */
    @PostMapping("/cancel/{videoId}")
    @RepeatSubmit(interval = 3000, message = "操作太频繁，请稍后再试")
    public Result<String> cancelLike(@PathVariable Long videoId) {
        Long userId = BaseContext.getCurrentId();
        log.info("取消点赞：userId={}, videoId={}", userId, videoId);
        likeService.cancelLike(userId, videoId);
        return Result.success("操作成功");
    }
}
