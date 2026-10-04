package com.feed.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性，对应 application.yml 中 feed.jwt 配置段
 */
@Data
@Component
@ConfigurationProperties(prefix = "feed.jwt")
public class JwtProperties {

    // 签名密钥
    private String secretKey;

    // 有效期（毫秒），7天 = 604800000
    private Long ttl;

    // 前端传递令牌的请求头名称
    private String tokenName;
}
