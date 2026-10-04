package com.feed.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解：同一用户在 period 秒内最多访问 limit 次，超限直接拒绝。
 * 底层用 Redis 计数器实现，多实例部署也生效。
 * 与 RepeatSubmit 的区别：RepeatSubmit 防的是"相同参数的重复提交"，
 * RateLimit 防的是"同一接口的访问总量"（不管参数是否相同）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 时间窗口内允许的最大访问次数
     */
    int limit() default 10;

    /**
     * 时间窗口长度（秒）
     */
    long period() default 60;

    /**
     * 超限时的提示语
     */
    String message() default "访问过于频繁，请稍后再试";
}
