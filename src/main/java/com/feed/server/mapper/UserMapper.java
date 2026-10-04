package com.feed.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.feed.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 增减关注数，WHERE 条件保证扣减后不会变成负数
     *
     * @param userId 用户id
     * @param delta  变化量（+1 / -1）
     * @return 受影响行数
     */
    @Update("UPDATE t_user SET following_count = following_count + #{delta} " +
            "WHERE id = #{userId} AND following_count + #{delta} >= 0")
    int updateFollowingCount(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 增减粉丝数，WHERE 条件保证扣减后不会变成负数
     *
     * @param userId 用户id
     * @param delta  变化量（+1 / -1）
     * @return 受影响行数
     */
    @Update("UPDATE t_user SET follower_count = follower_count + #{delta} " +
            "WHERE id = #{userId} AND follower_count + #{delta} >= 0")
    int updateFollowerCount(@Param("userId") Long userId, @Param("delta") int delta);
}
