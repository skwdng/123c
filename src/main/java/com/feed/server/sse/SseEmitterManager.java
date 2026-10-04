package com.feed.server.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 连接管理器：一个用户维护一个长连接，
 * 通知产生时通过该连接推送到前端 EventSource
 */
@Component
@Slf4j
public class SseEmitterManager {

    // userId -> 长连接
    private final ConcurrentHashMap<Long, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    /**
     * 建立连接（0L 表示永不超时）。同用户重复连接时，旧连接被顶掉
     */
    public SseEmitter connect(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        SseEmitter old = emitterMap.put(userId, emitter);
        if (old != null) {
            try {
                old.complete();
            } catch (Exception ignore) {
                // 旧连接已失效，忽略
            }
        }

        emitter.onCompletion(() -> removeIfPresent(userId, emitter));
        emitter.onTimeout(() -> {
            removeIfPresent(userId, emitter);
            emitter.complete();
        });
        emitter.onError(e -> removeIfPresent(userId, emitter));

        try {
            // 建连成功回执
            emitter.send(SseEmitter.event().name("connected").data("connected"));
        } catch (IOException e) {
            removeIfPresent(userId, emitter);
            emitter.completeWithError(e);
        }
        log.info("SSE 连接建立：userId={}, 当前在线连接={}", userId, emitterMap.size());
        return emitter;
    }

    /**
     * 给指定用户推送一条事件；用户不在线时静默丢弃（通知已落库，前端拉列表仍可见）
     */
    public void sendToUser(Long userId, String eventName, Object payload) {
        SseEmitter emitter = emitterMap.get(userId);
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name(eventName).data(payload));
        } catch (Exception e) {
            log.warn("SSE 推送失败，移除连接：userId={}, err={}", userId, e.getMessage());
            removeIfPresent(userId, emitter);
            emitter.completeWithError(e);
        }
    }

    private void removeIfPresent(Long userId, SseEmitter emitter) {
        // 只移除自己，避免顶号场景误删新连接
        emitterMap.remove(userId, emitter);
    }
}
