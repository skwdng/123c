package com.feed.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.feed.pojo.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知 Mapper
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
