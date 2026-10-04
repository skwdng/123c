package com.feed.server.service;

/**
 * 短信验证码业务
 */
public interface SmsService {

    /**
     * 发送验证码
     *
     * @param phone 手机号
     * @param scene 场景：register / login
     * @return 验证码（仅开发模拟阶段返回，接入真实短信后删除返回值）
     */
    String sendCode(String phone, String scene);

    /**
     * 校验验证码并立即作废（一次性使用）
     *
     * @param phone 手机号
     * @param code  用户填写的验证码
     */
    void verifyAndConsume(String phone, String code);
}
