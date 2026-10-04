package com.feed.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Feed 时间线分页结果（时间戳游标）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 当前页视频列表（按发布时间倒序）
    private List<VideoVO> videos;

    // 下一页游标：本页最后一条的发布时间毫秒值；无更多数据时为 null
    private Long nextCursor;

    // 是否还有更多
    private Boolean hasMore;
}
