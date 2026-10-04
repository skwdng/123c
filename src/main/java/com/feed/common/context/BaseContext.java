package com.feed.common.context;

/**
 * 基于 ThreadLocal 的当前登录用户上下文
 * 拦截器校验 JWT 通过后写入 userId，业务层随时取用，请求结束后移除
 */
public class BaseContext {

    private static final ThreadLocal<Long> THREAD_LOCAL = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        THREAD_LOCAL.set(id);
    }

    public static Long getCurrentId() {
        return THREAD_LOCAL.get();
    }

    public static void removeCurrentId() {
        THREAD_LOCAL.remove();
    }
}
