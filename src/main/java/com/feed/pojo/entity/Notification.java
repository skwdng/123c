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
 * 通知实体，对应 t_notification 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_notification")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    // 主键（雪花算法生成）
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    // 接收通知的用户id
    private Long userId;

    // 触发通知的用户id
    private Long fromUserId;

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
