package com.feed.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.feed.pojo.entity.LikeRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 点赞记录 Mapper
 */
@Mapper
public interface LikeMapper extends BaseMapper<LikeRecord> {
}
