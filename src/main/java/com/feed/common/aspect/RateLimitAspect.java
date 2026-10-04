package com.feed.common.aspect;

import com.feed.common.annotation.RateLimit;
import com.feed.common.context.BaseContext;
import com.feed.common.exception.BaseException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

/**
 * 接口限流切面（固定窗口计数器）：
 * key = feed:rate_limit:{用户id或IP}:{uri}，
 * 每次访问 INCR +1，第一次访问时设置过期时间（即窗口长度），
 * 计数超过 limit 抛业务异常。窗口到期 key 自动删除，计数归零。
 */
@Aspect
@Component
@Slf4j
public class RateLimitAspect {

    private static final String KEY_PREFIX = "feed:rate_limit:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes == null ? null : attributes.getRequest();
        String uri = request == null ? "unknown" : request.getRequestURI();

        // 已登录按用户限流，未登录（如登录接口本身）按来源 IP 限流
        Long userId = BaseContext.getCurrentId();
        String identity = userId != null ? String.valueOf(userId) : clientIp(request);
        String redisKey = KEY_PREFIX + identity + ":" + uri;

        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            // 第一次访问：设置窗口过期时间，之后窗口内的请求只加计数不再续期
            redisTemplate.expire(redisKey, Duration.ofSeconds(rateLimit.period()));
        }
        if (count != null && count > rateLimit.limit()) {
            log.warn("限流拦截：identity={}, uri={}, count={}/{}s", identity, uri, count, rateLimit.period());
            throw new BaseException(rateLimit.message());
        }

        return pjp.proceed();
    }

    /**
     * 取客户端真实 IP（经过反向代理时优先读 X-Forwarded-For 第一段）
     */
    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty() && !"unknown".equalsIgnoreCase(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
