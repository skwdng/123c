package com.feed.server.controller;

import com.feed.common.annotation.RateLimit;
import com.feed.common.context.BaseContext;
import com.feed.common.result.Result;
import com.feed.pojo.dto.LoginRequest;
import com.feed.pojo.dto.RegisterRequest;
import com.feed.pojo.dto.UserUpdateDTO;
import com.feed.pojo.vo.LoginVO;
import com.feed.pojo.vo.UserVO;
import com.feed.server.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<?> register(@RequestBody @Valid RegisterRequest req) {
        userService.register(req.getPhone(), req.getPassword(), req.getCode(), req.getNickname());
        return Result.success("注册成功");
    }

    @PostMapping("/login")
    @RateLimit(limit = 5, period = 60, message = "登录尝试过于频繁，请稍后再试")
    public Result<LoginVO> login(@RequestBody @Valid LoginRequest req) {
        LoginVO vo = userService.login(req.getAccount(), req.getPassword());
        return Result.success(vo);
    }

    /**
     * 注销账号：删除当前登录用户（需携带 authentication）
     */
    @DeleteMapping
    public Result<?> cancelAccount() {
        Long userId = BaseContext.getCurrentId();
        log.info("注销账号请求：userId={}", userId);
        userService.cancelAccount(userId);
        return Result.success("注销成功");
    }

    /**
     * 查询当前登录用户的个人资料
     */
    @GetMapping("/me")
    public Result<UserVO> me() {
        Long userId = BaseContext.getCurrentId();
        log.info("查询个人资料：userId={}", userId);
        return Result.success(userService.getMe(userId));
    }

    /**
     * 修改当前登录用户的个人资料（昵称/头像，传哪个改哪个）
     */
    @PutMapping("/me")
    public Result<String> updateMe(@RequestBody @Valid UserUpdateDTO dto) {
        Long userId = BaseContext.getCurrentId();
        log.info("修改个人资料：userId={}", userId);
        userService.updateMe(userId, dto);
        return Result.success("修改成功");
    }

    /**
     * 查看任意用户的公开资料（游客可访问，用于作者主页）
     *
     * @param userId 目标用户id
     */
    @GetMapping("/{userId}")
    public Result<UserVO> profile(@PathVariable Long userId) {
        log.info("查看用户主页：targetUserId={}", userId);
        return Result.success(userService.getMe(userId));
    }
}
