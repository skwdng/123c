package com.feed.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.feed.common.context.BaseContext;
import com.feed.common.exception.BaseException;
import com.feed.common.result.Result;
import com.feed.common.util.JwtUtil;
import com.feed.pojo.vo.NotificationVO;
import com.feed.server.service.NotificationService;
import com.feed.server.sse.SseEmitterManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

/**
 * 通知接口：SSE 实时推送 + 通知历史分页
 */
@RestController
@RequestMapping("/notification")
@Slf4j
public class NotificationController {

    @Autowired
    private SseEmitterManager sseEmitterManager;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private NotificationService notificationService;

    /**
     * 建立 SSE 长连接。
     * EventSource 无法自定义请求头，token 通过 query 参数传递：
     * GET /notification/stream?token=xxx
     *
     * @param token JWT 令牌
     * @return SseEmitter（永不超时）
     */
    @GetMapping(path = "/stream")
    public SseEmitter stream(@RequestParam String token) {
        if (token == null || !jwtUtil.verify(token)) {
            // 未认证：抛业务异常由全局异常处理器返回 code=0
            throw new BaseException("登录状态无效，请重新登录");
        }
        Long userId = jwtUtil.getUserId(token);
        BaseContext.setCurrentId(userId);
        try {
            return sseEmitterManager.connect(userId);
        } finally {
            BaseContext.removeCurrentId();
        }
    }

    /**
     * 分页查询通知列表（按时间倒序）
     *
     * @param isRead   已读状态：null 全部 / 0 未读 / 1 已读
     * @param page     页码，从 1 开始
     * @param pageSize 每页条数
     */
    @GetMapping("/list")
    public Result<Page<NotificationVO>> list(@RequestParam(required = false) Integer isRead,
                                             @RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) Integer pageSize) {
        Long userId = BaseContext.getCurrentId();
        return Result.success(notificationService.list(userId, isRead, page, pageSize));
    }

    /**
     * 标记单条通知已读
     *
     * @param id 通知id
     */
    @PostMapping("/read/{id}")
    public Result<String> markRead(@PathVariable Long id) {
        Long userId = BaseContext.getCurrentId();
        log.info("标记通知已读：userId={}, notificationId={}", userId, id);
        notificationService.markRead(userId, id);
        return Result.success("操作成功");
    }

    /**
     * 一键全部已读
     */
    @PostMapping("/read/all")
    public Result<String> markAllRead() {
        Long userId = BaseContext.getCurrentId();
        log.info("全部通知标记已读：userId={}", userId);
        notificationService.markAllRead(userId);
        return Result.success("操作成功");
    }

    /**
     * 未读通知数量（前端小红点角标）
     */
    @GetMapping("/unread/count")
    public Result<Map<String, Long>> unreadCount() {
        Long userId = BaseContext.getCurrentId();
        Map<String, Long> data = new HashMap<>();
        data.put("count", notificationService.unreadCount(userId));
        return Result.success(data);
    }
}
