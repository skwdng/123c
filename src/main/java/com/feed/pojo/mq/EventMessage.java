package com.feed.pojo.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 各队列统一使用的事件消息体（Jackson JSON 序列化）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    // 事件类型，取值见 EventType
    private String eventType;

    // 关联内容id
    private Long videoId;

    // 操作者id（点赞/评论/关注的发起人）
    private Long userId;

    // 事件目标用户id（通知接收者：内容作者/被关注者）
    private Long targetUserId;

    // 关联评论id
    private Long commentId;

    // 文本内容（评论内容/通知文案等）
    private String content;
}
