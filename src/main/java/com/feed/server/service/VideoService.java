package com.feed.server.service;

import com.feed.pojo.dto.VideoPublishDTO;
import com.feed.pojo.vo.SearchVO;
import com.feed.pojo.vo.VideoVO;

import java.util.List;
import java.util.Map;

/**
 * 内容业务接口
 */
public interface VideoService {

    /**
     * 发布内容：写 t_video 与 t_outbox_msg 在同一事务内，保证事件不丢
     *
     * @param userId 发布者id
     * @param dto    发布请求
     * @return 新内容id
     */
    Long publishVideo(Long userId, VideoPublishDTO dto);

    /**
     * 视频详情（走三级缓存），不存在时抛业务异常
     *
     * @param id 内容id
     * @return 视频详情（含作者快照）
     */
    VideoVO getVideoDetail(Long id);

    /**
     * 关键词搜索视频（标题/正文模糊匹配，按 id 倒序分页），同时累计热搜词
     *
     * @param keyword 关键词
     * @param page    页码，从 1 开始
     * @param size    每页条数
     * @return 分页搜索结果
     */
    SearchVO searchVideos(String keyword, int page, int size);

    /**
     * 热搜词榜单（按搜索次数倒序）
     *
     * @param topN 取前 N 个
     * @return 热词列表
     */
    List<String> listHotKeywords(int topN);

    /**
     * 查询某个用户发布的内容列表（作者主页用）
     *
     * @param authorId 作者id
     * @param page     页码，从 1 开始
     * @param size     每页条数
     * @return 分页结果：records/total/page/size/pages
     */
    Map<String, Object> listByUser(Long authorId, int page, int size);

    /**
     * 删除内容（软删除，仅作者本人可删），删除后清除缓存
     *
     * @param userId  当前登录用户id
     * @param videoId 内容id
     */
    void deleteVideo(Long userId, Long videoId);
}
