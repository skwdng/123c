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
 * 点赞记录实体，对应 t_like 表（一人对一内容仅一条）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_like")
public class LikeRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    // 主键（雪花算法生成）
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    // 点赞用户id
    private Long userId;

    // 内容id
    private Long videoId;

    // 创建时间
    private LocalDateTime createTime;
}
