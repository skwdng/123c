package com.feed.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录成功返回给前端的视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // JWT 令牌
    private String token;

    // 用户id
    private Long userId;

    // 对外展示的唯一uid（12位随机数字）
    private String uid;

    // 昵称
    private String nickname;

    // 头像地址
    private String avatarUrl;
}
