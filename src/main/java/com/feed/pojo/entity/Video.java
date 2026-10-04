package com.feed.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 内容（视频/动态）实体，对应 t_video 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_video")
public class Video implements Serializable {

    private static final long serialVersionUID = 1L;

    // 主键（雪花算法生成，IdType.ASSIGN_ID 走自定义 SnowflakeIdGenerator）
    @TableId(type = IdType.ASSIGN_ID)
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

    // 软删除标记：0 正常 1 已删除
    // @TableLogic 让 MyBatis-Plus 自动处理：查询自动带 deleted=0，deleteById 自动变 UPDATE
    @TableLogic
    private Integer deleted;

    // 创建时间（同时作为 Feed 排序与游标依据）
    private LocalDateTime createTime;
}
