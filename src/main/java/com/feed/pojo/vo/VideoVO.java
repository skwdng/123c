package com.feed.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 视频/内容详情视图对象（内嵌作者快照，作为三级缓存的缓存载体）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 内容id
    private Long id;

    // 发布者id
    private Long userId;

    // 标题
    private String title;

    // 正文
    private String content;

    // 封面URL
    private String coverUrl;

    // 视频URL
    private String videoUrl;

    // 点赞数
    private Integer likesCount;

    // 评论数
    private Integer commentsCount;

    // 创建时间
    private LocalDateTime createTime;

    // 作者昵称（冗余快照，避免 Feed 列表再查一次用户表）
    private String authorNickname;

    // 作者头像
    private String authorAvatarUrl;
}
