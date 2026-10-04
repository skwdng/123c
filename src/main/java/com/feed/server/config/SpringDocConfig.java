package com.feed.server.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger 接口文档配置（springdoc-openapi）。
 * 启动后访问：http://localhost:9090/swagger-ui.html
 */
@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI feedOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Java Feed 流系统接口文档")
                .description("用户 / 内容 / Feed 流 / 互动（点赞评论关注）/ 通知 全量接口")
                .version("v1.0.0"));
    }
}
