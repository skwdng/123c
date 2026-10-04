package com.feed.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.feed.common.constant.MessageConstant;
import com.feed.common.exception.BaseException;
import com.feed.pojo.entity.User;
import com.feed.server.mapper.UserMapper;
import com.feed.server.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 短信验证码业务实现
 * 当前为「模拟短信」：验证码打印日志并随接口返回；
 * 接入阿里云/腾讯云短信后，只需把 sendSmsText() 替换为真实 SDK 调用。
 */
@Service
@Slf4j
public class SmsServiceImpl implements SmsService {

    // Redis key 前缀
    private static final String CODE_KEY_PREFIX = "sms:code:";
    private static final String TTL_KEY_PREFIX = "sms:ttl:";
    // 验证码有效期 5 分钟
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    // 两次发送间隔限制 60 秒
    private static final Duration SEND_INTERVAL = Duration.ofSeconds(60);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private UserMapper userMapper;

    @Override
    public String sendCode(String phone, String scene) {
        // 场景前置校验：注册时手机号不能已存在，登录时必须已存在
        boolean exists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, phone)
        ) > 0;
        if ("login".equals(scene)) {
            if (!exists) {
                throw new BaseException(MessageConstant.USER_NOT_EXISTS);
            }
        } else {
            if (exists) {
                throw new BaseException(MessageConstant.PHONE_EXISTS);
            }
        }

        // 60 秒发送频控
        String ttlKey = TTL_KEY_PREFIX + phone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(ttlKey))) {
            throw new BaseException("验证码发送过于频繁，请 60 秒后再试");
        }

        // 生成 6 位数字验证码（不足 6 位前补 0）
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));

        // 存入 Redis：验证码 5 分钟过期，频控标记 60 秒过期
        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + phone, code, CODE_TTL);
        redisTemplate.opsForValue().set(ttlKey, "1", SEND_INTERVAL);

        // 模拟发送短信（真实环境改为调用短信服务商 API）
        sendSmsText(phone, code);
        return code;
    }

    @Override
    public void verifyAndConsume(String phone, String code) {
        Object cached = redisTemplate.opsForValue().get(CODE_KEY_PREFIX + phone);
        if (cached == null) {
            throw new BaseException("验证码已过期，请重新获取");
        }
        if (!cached.toString().equals(code)) {
            throw new BaseException("验证码错误");
        }
        // 校验通过立即删除，保证一次性使用
        redisTemplate.delete(CODE_KEY_PREFIX + phone);
    }

    /**
     * 发送短信内容。模拟阶段只打日志；接入真实短信时替换本方法即可。
     */
    private void sendSmsText(String phone, String code) {
        log.info("【模拟短信】向手机号 {} 发送验证码：{}（5 分钟内有效）", phone, code);
    }
}
