package com.feed.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.feed.pojo.dto.CommentDTO;
import com.feed.pojo.vo.CommentVO;

/**
 * 评论业务接口（发表走 MQ 异步落库，列表为同步查询）
 */
public interface CommentService {

    /**
     * 发表评论
     *
     * @param userId 评论用户id
     * @param dto    评论请求
     */
    void comment(Long userId, CommentDTO dto);

    /**
     * 分页查询某内容的评论列表（按时间倒序），冗余评论者昵称/头像
     *
     * @param videoId  内容id
     * @param page     页码，从 1 开始
     * @param pageSize 每页条数
     * @return 评论分页
     */
    Page<CommentVO> listByVideo(Long videoId, Integer page, Integer pageSize);
}
