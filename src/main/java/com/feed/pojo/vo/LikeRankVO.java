package com.feed.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 点赞榜分页结果（复合游标：likes_count + id）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeRankVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 当前页视频列表（按点赞数倒序）
    private List<VideoVO> videos;

    // 下一页游标：本页最后一条的点赞数
    private Long lastLikesCount;

    // 下一页游标：本页最后一条的内容id
    private Long lastId;

    // 是否还有更多
    private Boolean hasMore;
}
