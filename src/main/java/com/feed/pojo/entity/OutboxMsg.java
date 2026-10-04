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
 * Outbox 发件箱消息实体，对应 t_outbox_msg 表
 * 业务数据与发件消息同事务写入，轮询器投递 MQ 成功后删除，保证事件不丢
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_outbox_msg")
public class OutboxMsg implements Serializable {

    private static final long serialVersionUID = 1L;

    // 主键
    @TableId(type = IdType.AUTO)
    private Long id;

    // 关联内容id
    private Long videoId;

    // 事件类型，如 VIDEO_PUBLISH
    private String eventType;

    // 状态：pending 待投递 / sent 已投递
    private String status;

    // 创建时间
    private LocalDateTime createTime;
}
