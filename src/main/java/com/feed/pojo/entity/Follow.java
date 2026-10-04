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
 * 关注关系实体，对应 t_follow 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_follow")
public class Follow implements Serializable {

    private static final long serialVersionUID = 1L;

    // 主键（雪花算法生成）
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    // 关注者id（粉丝）
    private Long followerId;

    // 被关注者id（博主）
    private Long followeeId;

    // 创建时间
    private LocalDateTime createTime;
}
