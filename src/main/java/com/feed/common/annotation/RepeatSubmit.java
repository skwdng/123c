package com.feed.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 防重复提交注解：同一用户在 interval 毫秒内对同一接口提交相同参数，第二次直接拒绝。
 * 底层用 Redis SETNX（setIfAbsent）实现，多实例部署也生效。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RepeatSubmit {

    /**
     * 间隔时间（毫秒），默认 5 秒
     */
    long interval() default 5000;

    /**
     * 重复提交时的提示语
     */
    String message() default "操作太频繁，请勿重复提交";
}
