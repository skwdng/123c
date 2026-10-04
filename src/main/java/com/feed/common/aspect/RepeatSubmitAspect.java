package com.feed.common.aspect;

import cn.hutool.crypto.digest.DigestUtil;
import com.feed.common.annotation.RepeatSubmit;
import com.feed.common.context.BaseContext;
import com.feed.common.exception.BaseException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.Arrays;

/**
 * 防重复提交切面：
 * key = 用户id + 请求URI + 参数摘要(MD5)，
 * Redis SETNX 成功才放行，并设置过期时间；间隔内第二次提交 key 已存在 → 拒绝。
 */
@Aspect
@Component
@Slf4j
public class RepeatSubmitAspect {

    private static final String KEY_PREFIX = "feed:repeat_submit:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Around("@annotation(repeatSubmit)")
    public Object around(ProceedingJoinPoint pjp, RepeatSubmit repeatSubmit) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String uri = attributes == null ? "unknown" : attributes.getRequest().getRequestURI();

        Long userId = BaseContext.getCurrentId();
        // 请求体里的文件/原生 request 不参与指纹，只对业务参数做摘要
        String argsDigest = DigestUtil.md5Hex(Arrays.toString(filterArgs(pjp.getArgs())));

        String redisKey = KEY_PREFIX + userId + ":" + uri + ":" + argsDigest;

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "1", Duration.ofMillis(repeatSubmit.interval()));
        if (!Boolean.TRUE.equals(acquired)) {
            log.warn("重复提交被拦截：userId={}, uri={}", userId, uri);
            throw new BaseException(repeatSubmit.message());
        }

        return pjp.proceed();
    }

    private Object[] filterArgs(Object[] args) {
        return Arrays.stream(args)
                .filter(arg -> !(arg instanceof ServletRequest)
                        && !(arg instanceof ServletResponse)
                        && !(arg instanceof MultipartFile))
                .toArray();
    }
}
