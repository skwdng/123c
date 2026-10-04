package com.feed.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.feed.common.annotation.RepeatSubmit;
import com.feed.common.context.BaseContext;
import com.feed.common.result.Result;
import com.feed.pojo.dto.CommentDTO;
import com.feed.pojo.vo.CommentVO;
import com.feed.server.service.CommentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论接口
 */
@RestController
@RequestMapping("/comment")
@Slf4j
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * 发表评论（异步处理，接口立即返回）
     *
     * @param dto 内容id + 评论内容
     */
    @PostMapping
    @RepeatSubmit(interval = 3000, message = "评论太频繁，请稍后再试")
    public Result<String> comment(@RequestBody @Valid CommentDTO dto) {
        Long userId = BaseContext.getCurrentId();
        log.info("评论：userId={}, videoId={}", userId, dto.getVideoId());
        commentService.comment(userId, dto);
        return Result.success("操作成功");
    }

    /**
     * 分页查询某内容的评论列表（按时间倒序）
     *
     * @param videoId  内容id
     * @param page     页码，从 1 开始
     * @param pageSize 每页条数，默认 10，最大 50
     */
    @GetMapping("/video/{videoId}")
    public Result<Page<CommentVO>> list(@PathVariable Long videoId,
                                        @RequestParam(required = false) Integer page,
                                        @RequestParam(required = false) Integer pageSize) {
        log.info("评论列表：videoId={}, page={}, pageSize={}", videoId, page, pageSize);
        return Result.success(commentService.listByVideo(videoId, page, pageSize));
    }
}
