package com.feed.server.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.feed.pojo.vo.VideoVO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 本地一级缓存（Caffeine）配置
 * 视频详情写后 3 秒过期、最多 500 个键，用来挡住瞬时高并发读
 */
@Configuration
public class L1CacheConfig {

    @Bean
    public Cache<Long, VideoVO> videoL1Cache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(3, TimeUnit.SECONDS)
                .maximumSize(500)
                .build();
    }
}
