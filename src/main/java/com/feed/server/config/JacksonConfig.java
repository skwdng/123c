package com.feed.server.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 配置：
 * 雪花 id 是 19 位 Long（最大 9223372036854775807），
 * 而 JavaScript 安全整数只有 2^53（约 16 位），数字返回前端会精度丢失，
 * 所以统一把 Long/long 序列化为字符串（前端原样传回时 Jackson 能自动转回 Long）。
 * 注意：用 serializerByType 追加，不能用 modules() 整体替换，否则会顶掉
 * Spring Boot 自动注册的 JavaTimeModule，导致 LocalDateTime 序列化失败。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
