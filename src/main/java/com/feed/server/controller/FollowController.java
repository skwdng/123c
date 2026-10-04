package com.feed.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.feed.common.annotation.RepeatSubmit;
import com.feed.common.context.BaseContext;
import com.feed.common.result.Result;
import com.feed.pojo.vo.UserVO;
import com.feed.server.service.FollowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 关注/取关接口
 */
@RestController
@RequestMapping("/follow")
@Slf4j
public class FollowController {

    @Autowired
    private FollowService followService;

    /**
     * 关注（异步处理，接口立即返回）
     *
     * @param targetUserId 被关注用户id
     */
    @PostMapping("/{targetUserId}")
    @RepeatSubmit(interval = 3000, message = "操作太频繁，请稍后再试")
    public Result<String> follow(@PathVariable Long targetUserId) {
        Long userId = BaseContext.getCurrentId();
        log.info("关注：followerId={}, followeeId={}", userId, targetUserId);
        followService.follow(userId, targetUserId);
        return Result.success("操作成功");
    }

    /**
     * 取关（异步处理，接口立即返回）
     *
     * @param targetUserId 被关注用户id
     */
    @DeleteMapping("/{targetUserId}")
    public Result<String> unfollow(@PathVariable Long targetUserId) {
        Long userId = BaseContext.getCurrentId();
        log.info("取关：followerId={}, followeeId={}", userId, targetUserId);
        followService.unfollow(userId, targetUserId);
        return Result.success("操作成功");
    }

    /**
     * 我的关注列表（分页，按关注时间倒序）
     *
     * @param page     页码，从 1 开始
     * @param pageSize 每页条数，默认 10，最大 50
     */
    @GetMapping("/list")
    public Result<Page<UserVO>> followList(@RequestParam(required = false) Integer page,
                                           @RequestParam(required = false) Integer pageSize) {
        Long userId = BaseContext.getCurrentId();
        log.info("关注列表：userId={}, page={}, pageSize={}", userId, page, pageSize);
        return Result.success(followService.followList(userId, page, pageSize));
    }

    /**
     * 我的粉丝列表（分页，按关注时间倒序）
     *
     * @param page     页码，从 1 开始
     * @param pageSize 每页条数，默认 10，最大 50
     */
    @GetMapping("/fans")
    public Result<Page<UserVO>> fansList(@RequestParam(required = false) Integer page,
                                         @RequestParam(required = false) Integer pageSize) {
        Long userId = BaseContext.getCurrentId();
        log.info("粉丝列表：userId={}, page={}, pageSize={}", userId, page, pageSize);
        return Result.success(followService.fansList(userId, page, pageSize));
    }
}
