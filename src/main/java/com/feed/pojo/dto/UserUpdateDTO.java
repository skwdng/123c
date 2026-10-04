package com.feed.pojo.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改个人资料请求体：字段均可选，传哪个改哪个
 */
@Data
public class UserUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 新昵称（可空表示不修改）
    @Size(max = 30, message = "昵称不能超过30个字符")
    private String nickname;

    // 新头像地址（可空表示不修改）
    @Size(max = 255, message = "头像地址过长")
    private String avatarUrl;
}
