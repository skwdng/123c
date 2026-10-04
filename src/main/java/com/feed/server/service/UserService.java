package com.feed.server.service;

import com.feed.pojo.dto.UserUpdateDTO;
import com.feed.pojo.vo.LoginVO;
import com.feed.pojo.vo.UserVO;

/**
 * 用户业务接口
 */
public interface UserService {

    /**
     * 用户注册
     *
     * @param phone    手机号（唯一登录凭证）
     * @param password 明文密码
     * @param code     短信验证码
     * @param nickname 昵称（可空、允许重复）
     */
    void register(String phone, String password, String code, String nickname);

    /**
     * 用户登录
     *
     * @param account  登录账号（12 位 uid 或 11 位手机号）
     * @param password 明文密码
     * @return 登录视图对象（含 JWT）
     */
    LoginVO login(String account, String password);

    /**
     * 注销当前登录用户（删除账号）
     *
     * @param userId 当前登录用户id
     */
    void cancelAccount(Long userId);

    /**
     * 查询个人资料
     *
     * @param userId 用户id
     * @return 脱敏后的用户资料
     */
    UserVO getMe(Long userId);

    /**
     * 修改个人资料（昵称/头像，传哪个改哪个）
     *
     * @param userId 用户id
     * @param dto    修改内容
     */
    void updateMe(Long userId, UserUpdateDTO dto);
}
