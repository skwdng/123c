package com.feed.server.config;

import com.feed.server.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC 配置：注册 JWT 拦截器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    /**
     * 上传文件的静态访问映射：/uploads/** -> 运行目录下的 uploads/ 文件夹
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 必须以 / 结尾，否则 Spring 拼接文件路径时会丢掉 uploads 目录段，导致 404
        String uploadPath = Paths.get(System.getProperty("user.dir"), "uploads").toUri().toString();
        if (!uploadPath.endsWith("/")) {
            uploadPath = uploadPath + "/";
        }
        // 上传的文件名是 UUID 永不重复，可以放心缓存 7 天：看过一次后再看直接读本地，秒开
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath)
                .setCacheControl(CacheControl.maxAge(7, java.util.concurrent.TimeUnit.DAYS));

        // HTML 页面禁止缓存：样式和脚本经常迭代，避免手机/浏览器拿到旧版
        registry.addResourceHandler("/*.html")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 注册、登录、发送短信验证码放行
                        "/user/register",
                        "/user/login",
                        "/sms/sendCode",
                        // SSE 基于 EventSource，无法自定义请求头，token 走 query 参数，在 Controller 内校验
                        "/notification/stream",
                        // Swagger 接口文档放行
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        // actuator 健康检查放行
                        "/actuator/**",
                        // 本地测试页面与静态资源放行
                        "/test.html",
                        "/login.html",
                        "/register.html",
                        "/123c.html",
                        "/favicon.ico",
                        // 游客可浏览：Feed 流、搜索、评论列表、作者主页（互动类操作仍需登录）
                        "/video/feed/**",
                        "/video/search",
                        "/video/search/hot",
                        "/video/user/**",
                        "/user/{userId:[0-9]+}",
                        "/comment/video/**",
                        // 上传的视频/图片静态资源放行
                        "/uploads/**"
                );
    }
}
