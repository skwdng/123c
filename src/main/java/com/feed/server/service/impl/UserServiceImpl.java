package com.feed.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.feed.common.constant.MessageConstant;
import com.feed.common.exception.BaseException;
import com.feed.common.util.JwtUtil;
import com.feed.pojo.dto.UserUpdateDTO;
import com.feed.pojo.entity.User;
import com.feed.pojo.vo.LoginVO;
import com.feed.pojo.vo.UserVO;
import com.feed.server.mapper.UserMapper;
import com.feed.server.service.SmsService;
import com.feed.server.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 用户业务实现
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    // uid 取值上界：10^12，生成 [0, 999999999999] 的随机数后格式化为 12 位
    private static final long UID_BOUND = 1_000_000_000_000L;
    // uid 冲突时的最大重试次数
    private static final int UID_MAX_RETRY = 5;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private SmsService smsService;

    /**
     * 注册：校验短信验证码 + 手机号查重后 BCrypt 加密入库；昵称可空、允许重复
     */
    @Override
    public void register(String phone, String password, String code, String nickname) {
        // 1. 校验验证码（通过后立即作废，不可重复使用）
        smsService.verifyAndConsume(phone, code);

        // 2. 判断手机号是否已注册
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, phone)
        );
        if (count > 0) {
            throw new BaseException(MessageConstant.PHONE_EXISTS);
        }

        User user = User.builder()
                .uid(generateUniqueUid())
                .phone(phone)
                .password(BCrypt.hashpw(password, BCrypt.gensalt()))
                .nickname(nickname != null ? nickname : phone)
                .followingCount(0)
                .followerCount(0)
                .build();
        userMapper.insert(user);
        log.info("注册成功：phone={}, uid={}", phone, user.getUid());
    }

    /**
     * 登录：支持 uid 或手机号，校验密码后签发 7 天有效期的 JWT
     */
    @Override
    public LoginVO login(String account, String password) {
        // 账号既可能是 uid 也可能是手机号，两个字段任一匹配即可
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUid, account)
                        .or()
                        .eq(User::getPhone, account)
        );
        if (user == null) {
            throw new BaseException(MessageConstant.USER_NOT_EXISTS);
        }

        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new BaseException(MessageConstant.PASSWORD_ERROR);
        }

        String token = jwtUtil.generateToken(user.getId());
        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .uid(user.getUid())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    /**
     * 注销：删除当前登录用户的账号记录
     */
    @Override
    public void cancelAccount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BaseException(MessageConstant.USER_NOT_EXISTS);
        }
        userMapper.deleteById(userId);
        log.info("用户注销：id={}, uid={}, phone={}", userId, user.getUid(), user.getPhone());
    }

    /**
     * 查询个人资料：密码等敏感字段不出库，手机号脱敏
     */
    @Override
    public UserVO getMe(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BaseException(MessageConstant.USER_NOT_EXISTS);
        }
        return UserVO.from(user);
    }

    /**
     * 修改个人资料：昵称/头像传哪个改哪个，都为空则拒绝
     */
    @Override
    public void updateMe(Long userId, UserUpdateDTO dto) {
        boolean updateNickname = StringUtils.hasText(dto.getNickname());
        boolean updateAvatar = StringUtils.hasText(dto.getAvatarUrl());
        if (!updateNickname && !updateAvatar) {
            throw new BaseException(MessageConstant.USER_UPDATE_EMPTY);
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BaseException(MessageConstant.USER_NOT_EXISTS);
        }
        if (updateNickname) {
            user.setNickname(dto.getNickname().trim());
        }
        if (updateAvatar) {
            user.setAvatarUrl(dto.getAvatarUrl().trim());
        }
        userMapper.updateById(user);
        log.info("修改个人资料：userId={}, nickname={}, avatarUrl={}",
                userId, updateNickname ? user.getNickname() : "-", updateAvatar ? user.getAvatarUrl() : "-");
    }

    /**
     * 生成 12 位随机数字 uid：随机 [0, 10^12) 后前补 0；撞号最多重试 5 次，
     * 数据库 uk_uid 唯一索引是最终兜底
     */
    private String generateUniqueUid() {
        for (int i = 0; i < UID_MAX_RETRY; i++) {
            long n = ThreadLocalRandom.current().nextLong(UID_BOUND);
            String uid = String.format("%012d", n);
            Long count = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getUid, uid)
            );
            if (count == 0) {
                return uid;
            }
            log.warn("uid 撞号，第 {} 次重试：{}", i + 1, uid);
        }
        throw new BaseException(MessageConstant.UID_GENERATE_FAILED);
    }
}
