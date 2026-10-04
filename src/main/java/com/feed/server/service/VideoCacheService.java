package com.feed.server.service;

import com.feed.pojo.vo.VideoVO;

import java.util.Collection;
import java.util.List;

/**
 * 视频详情三级缓存服务：
 * L1 Caffeine(3s) -> L2 Redis MGet(1h) -> L3 MySQL，
 * L3 命中通过 singleflight 合并同内容的并发穿透，异步回写 L1/L2
 */
public interface VideoCacheService {

    /**
     * 按 id 批量获取视频详情（含作者信息），保持入参顺序，不存在的 id 自动剔除
     *
     * @param ids 内容id集合
     * @return 视频详情列表
     */
    List<VideoVO> getVideoByIds(Collection<Long> ids);

    /**
     * 清除指定内容的 L1/L2 缓存（内容删除/修改后调用，防止读到旧数据）
     *
     * @param videoId 内容id
     */
    void evict(Long videoId);
}
