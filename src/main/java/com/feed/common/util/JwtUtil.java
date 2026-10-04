package com.feed.common.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.feed.common.properties.JwtProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT 工具类：签发 / 解析 / 校验，密钥与有效期从 application.yml 读取
 */
@Component
public class JwtUtil {

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 签发 JWT，载荷中携带 userId
     *
     * @param userId 用户id
     * @return token
     */
    public String generateToken(Long userId) {
        return JWT.create()
                .withClaim("userId", userId)
                .withExpiresAt(new Date(System.currentTimeMillis() + jwtProperties.getTtl()))
                .sign(Algorithm.HMAC256(jwtProperties.getSecretKey()));
    }

    /**
     * 从 token 中解析 userId
     *
     * @param token 令牌
     * @return 用户id
     */
    public Long getUserId(String token) {
        DecodedJWT jwt = JWT.decode(token);
        return jwt.getClaim("userId").asLong();
    }

    /**
     * 校验 token 是否有效
     *
     * @param token 令牌
     * @return true 有效
     */
    public boolean verify(String token) {
        try {
            JWT.require(Algorithm.HMAC256(jwtProperties.getSecretKey())).build().verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
