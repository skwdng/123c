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
 * 评论实体，对应 t_comment 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_comment")
public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;

    // 主键（雪花算法生成）
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    // 内容id
    private Long videoId;

    // 评论用户id
    private Long userId;

    // 评论内容
    private String content;

    // 创建时间
    private LocalDateTime createTime;
}
