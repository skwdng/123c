package com.feed.server.controller;

import com.feed.common.result.Result;
import com.feed.pojo.dto.SendCodeRequest;
import com.feed.server.service.SmsService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短信验证码接口（无需登录）
 */
@RestController
@RequestMapping("/sms")
@Slf4j
public class SmsController {

    @Autowired
    private SmsService smsService;

    /**
     * 发送验证码
     */
    @PostMapping("/sendCode")
    public Result<String> sendCode(@RequestBody @Valid SendCodeRequest req) {
        log.info("发送短信验证码：phone={}, scene={}", req.getPhone(), req.getScene());
        String code = smsService.sendCode(req.getPhone(), req.getScene());
        // 模拟阶段把验证码返回给前端方便测试；接真实短信后改为 Result.success()
        return Result.success(code);
    }
}
