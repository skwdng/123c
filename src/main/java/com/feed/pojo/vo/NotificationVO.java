package com.feed.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知视图对象（冗余触发者昵称/头像）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 通知id
    private Long id;

    // 接收通知的用户id
    private Long userId;

    // 触发通知的用户id
    private Long fromUserId;

    // 触发者昵称
    private String fromNickname;

    // 触发者头像
    private String fromAvatarUrl;

    // 类型：LIKE / COMMENT / FOLLOW
    private String type;

    // 通知文案
    private String content;

    // 关联内容id
    private Long videoId;

    // 是否已读：0 未读 / 1 已读
    private Integer isRead;

    // 创建时间
    private LocalDateTime createTime;
}
