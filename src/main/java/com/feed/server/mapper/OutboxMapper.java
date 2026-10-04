package com.feed.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.feed.pojo.entity.OutboxMsg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Outbox 发件箱 Mapper
 */
@Mapper
public interface OutboxMapper extends BaseMapper<OutboxMsg> {

    /**
     * 轮询器使用：捞出一批待投递消息（一次最多 100 条，按 id 顺序）
     *
     * @return pending 状态消息列表
     */
    @Select("SELECT * FROM t_outbox_msg WHERE status = 'pending' ORDER BY id LIMIT 100")
    List<OutboxMsg> selectPending();
}
