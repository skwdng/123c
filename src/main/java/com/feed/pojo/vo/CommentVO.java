package com.feed.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论视图对象（冗余评论者昵称/头像，列表页免二次查用户）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 评论id
    private Long id;

    // 内容id
    private Long videoId;

    // 评论用户id
    private Long userId;

    // 评论者昵称
    private String nickname;

    // 评论者头像
    private String avatarUrl;

    // 评论内容
    private String content;

    // 创建时间
    private LocalDateTime createTime;
}
