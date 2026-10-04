package com.feed.server.interceptor;

import com.feed.common.context.BaseContext;
import com.feed.common.properties.JwtProperties;
import com.feed.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 登录拦截器：从 authentication 请求头解析 token，
 * 校验通过后把 userId 放入 BaseContext，请求结束清理 ThreadLocal
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader(jwtProperties.getTokenName());
        if (token == null || token.isEmpty()) {
            log.info("请求未携带 token：{}", request.getRequestURI());
            writeUnauthorized(response, "未登录");
            return false;
        }

        if (!jwtUtil.verify(token)) {
            log.info("token 无效或已过期：{}", request.getRequestURI());
            writeUnauthorized(response, "token无效或已过期");
            return false;
        }

        Long userId = jwtUtil.getUserId(token);
        BaseContext.setCurrentId(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 必须清理，避免 Tomcat 线程池复用导致用户身份串号
        BaseContext.removeCurrentId();
    }

    /**
     * 直接写出未登录的 JSON 响应
     */
    private void writeUnauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":0,\"msg\":\"" + msg + "\"}");
    }
}
