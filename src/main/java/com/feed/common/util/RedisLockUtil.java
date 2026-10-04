package com.feed.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 基于 Redis 的分布式锁工具
 * 加锁：SETNX（setIfAbsent）+ 过期时间 —— 过期时间防止持锁方宕机后锁永远不释放
 * 解锁：Lua 脚本"先比较值再删除"—— 防止误删别人的锁
 *       （A 的锁过期后被 B 拿到，A 醒来直接 del 会删掉 B 的锁，比较值就能避免）
 */
@Component
@Slf4j
public class RedisLockUtil {

    // 解锁 Lua 脚本：值匹配才删除，返回 1 删除成功 / 0 锁已不属于自己
    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else return 0 end",
            Long.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 尝试加锁（只试一次）
     *
     * @param key     锁的 key
     * @param value   持锁标识（解锁时校验，一般传 UUID）
     * @param timeout 锁的自动过期时间
     * @return 是否加锁成功
     */
    public boolean tryLock(String key, String value, Duration timeout) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, value, timeout);
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * 尝试加锁（失败后按间隔重试）
     *
     * @param key        锁的 key
     * @param value      持锁标识
     * @param timeout    锁的自动过期时间
     * @param waitMillis 每次重试的等待毫秒数
     * @param retryTimes 最大重试次数
     * @return 是否加锁成功
     */
    public boolean tryLock(String key, String value, Duration timeout, long waitMillis, int retryTimes) {
        for (int i = 0; i < retryTimes; i++) {
            if (tryLock(key, value, timeout)) {
                return true;
            }
            try {
                Thread.sleep(waitMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * 解锁：只释放自己持有的锁
     *
     * @param key   锁的 key
     * @param value 加锁时用的持锁标识
     * @return 是否成功释放
     */
    public boolean unlock(String key, String value) {
        Long result = redisTemplate.execute(UNLOCK_SCRIPT, List.of(key), value);
        return result != null && result == 1L;
    }
}
