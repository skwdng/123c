package com.feed.pojo.vo;

import com.feed.pojo.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户资料视图对象（不暴露密码等敏感字段，手机号脱敏）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 用户id
    private Long id;

    // 对外展示的唯一uid（12位随机数字）
    private String uid;

    // 昵称
    private String nickname;

    // 头像地址
    private String avatarUrl;

    // 脱敏手机号，如 138****0001
    private String phone;

    // 关注数
    private Integer followingCount;

    // 粉丝数
    private Integer followerCount;

    // 注册时间
    private LocalDateTime createTime;

    /**
     * 实体转视图对象（统一脱敏规则，避免各处重复组装）
     */
    public static UserVO from(User user) {
        if (user == null) {
            return null;
        }
        return UserVO.builder()
                .id(user.getId())
                .uid(user.getUid())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .phone(maskPhone(user.getPhone()))
                .followingCount(user.getFollowingCount())
                .followerCount(user.getFollowerCount())
                .createTime(user.getCreateTime())
                .build();
    }

    /**
     * 手机号脱敏：保留前 3 位和后 4 位，如 138****0001
     */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
