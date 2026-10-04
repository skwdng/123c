package com.feed.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.feed.pojo.entity.Video;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 内容 Mapper
 */
@Mapper
public interface VideoMapper extends BaseMapper<Video> {

    /**
     * 冷数据查询：取发布时间（毫秒）早于 watermark 的内容id，按时间倒序
     *
     * @param watermark 时间戳游标（毫秒）
     * @param limit     最多返回条数
     * @return 内容id列表（时间倒序）
     */
    List<Long> selectColdVideoIds(@Param("watermark") Long watermark, @Param("limit") Integer limit);

    /**
     * 点赞数 +1（Worker 异步调用）
     *
     * @param id 内容id
     * @return 受影响行数
     */
    @Update("UPDATE t_video SET likes_count = likes_count + 1 WHERE id = #{id}")
    int incrementLikesCount(@Param("id") Long id);

    /**
     * 评论数 +1（Worker 异步调用）
     *
     * @param id 内容id
     * @return 受影响行数
     */
    @Update("UPDATE t_video SET comments_count = comments_count + 1 WHERE id = #{id}")
    int incrementCommentsCount(@Param("id") Long id);

    /**
     * 点赞数 -1（Worker 异步调用，likes_count > 0 防止减成负数）
     *
     * @param id 内容id
     * @return 受影响行数
     */
    @Update("UPDATE t_video SET likes_count = likes_count - 1 WHERE id = #{id} AND likes_count > 0")
    int decrementLikesCount(@Param("id") Long id);
}
