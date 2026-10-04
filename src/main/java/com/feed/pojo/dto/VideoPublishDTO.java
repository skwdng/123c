package com.feed.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 发布内容请求
 */
@Data
public class VideoPublishDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 标题
    @NotBlank(message = "标题不能为空")
    private String title;

    // 正文
    @NotBlank(message = "正文不能为空")
    private String content;

    // 封面URL
    @NotBlank(message = "封面URL不能为空")
    private String coverUrl;

    // 视频URL
    @NotBlank(message = "视频URL不能为空")
    private String videoUrl;
}
