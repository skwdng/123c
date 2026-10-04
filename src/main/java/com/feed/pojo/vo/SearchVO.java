package com.feed.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 视频搜索结果视图对象（分页）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 当前页数据
    private List<VideoVO> records;

    // 命中总数
    private long total;

    // 当前页码（从 1 开始）
    private long page;

    // 每页条数
    private long size;

    // 总页数
    private long pages;
}
