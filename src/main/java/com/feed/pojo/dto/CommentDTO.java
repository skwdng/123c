package com.feed.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 发表评论请求
 */
@Data
public class CommentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 内容id
    @NotNull(message = "内容id不能为空")
    private Long videoId;

    // 评论内容
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容最长500字")
    private String content;
}
