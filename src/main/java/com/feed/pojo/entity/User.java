package com.feed.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体，对应 t_user 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    // 主键
    @TableId(type = IdType.AUTO)
    private Long id;

    // 对外展示的唯一uid（12位随机数字字符串，前补0）
    private String uid;

    // 手机号（唯一，登录凭证）
    private String phone;

    // 密码（BCrypt 加密）
    private String password;

    // 昵称
    private String nickname;

    // 头像地址
    private String avatarUrl;

    // 关注数
    private Integer followingCount;

    // 粉丝数
    private Integer followerCount;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;
}
