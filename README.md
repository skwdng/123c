# 123c 短视频平台

一个类抖音的短视频 Web 应用，支持全屏竖屏刷视频、点赞、评论、关注、发布作品、搜索、实时通知等核心功能。后端采用 Spring Boot 三层架构，前端为零依赖单文件 HTML，并已通过内网穿透实现公网多端访问。

## 🎬 演示视频

- 🖥️ [电脑端演示（36MB）](demo/123c-demo.mp4)
- 📱 [手机端演示（2.2MB）](demo/123c-mobile-demo.mp4)

> 视频文件较大，**GitHub 上请点右上角 Download 下载后观看**（Gitee 仓库支持在线播放）。
> 手机端针对竖屏做了全屏滑动、自适应操作栏等专门适配。

## ✨ 功能特性

**用户侧**

- 全屏竖屏刷视频，scroll-snap 一屏一滑，支持双击点赞
- 视频懒加载，滑出屏幕自动断开下载源，节省带宽
- 横屏视频自动 `object-fit: contain` 完整显示，上下留纯黑边
- 评论、点赞、关注 / 粉丝、个人主页、作品网格、搜索
- 视频发布（本地选视频 → 上传 → 填标题）、作者删除作品
- 点赞 / 评论 / 关注的 SSE 实时通知推送 + 未读红点
- 自动播放（静音兜底）+ 首次互动恢复声音、进度条点击跳转
- 手机端 100dvh 真全屏、顶部浮动栏、弹窗全屏化、刘海避让

**服务端**

- Feed 流：Caffeine → Redis → MySQL 三级缓存
- 点赞：Redis 分布式锁 + RabbitMQ 异步入库 + 消费幂等
- 消息可靠投递：本地消息表（Outbox）+ 死信队列兜底
- JWT 无状态登录、BCrypt 密码加密、自定义 `@RateLimit` 接口限流
- MyBatis-Plus 逻辑删除、雪花算法分布式 ID、复合索引优化

## 🧱 技术栈

| 分层 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.5、Spring AOP、Spring Validation |
| 安全 | JWT（java-jwt 4.4.0）、jBCrypt |
| ORM | MyBatis-Plus 3.5.7、MySQL 8.0 |
| 缓存 | Caffeine（L1）、Redis（L2，Lettuce 连接池） |
| 消息队列 | RabbitMQ（交换机 / 队列 / 死信队列） |
| 实时通信 | SSE（SseEmitter） |
| 接口文档 | springdoc-openapi（Swagger UI） |
| 前端 | 原生 HTML / CSS / JavaScript 单文件，无任何框架依赖 |
| 工具 | Hutool、Lombok、Maven |
| 部署 | Docker、docker-compose、多环境 profile |

## 🏗️ 架构设计

```
浏览器(单文件HTML)
      │  JWT(authentication 请求头) / SSE(query token)
      ▼
Controller ── AOP(限流/参数校验/全局异常)
      │
   Service ──► Caffeine(L1 本地热点)
      │           └ miss ─► Redis(L2 分布式缓存)
      │                          └ miss ─► MySQL
      │
      └─► 点赞/评论/关注 ─► RabbitMQ 异步 Worker 落库
                 │                  │
                 │           加 Redis 分布式锁 + 幂等判断
                 ▼
          同库事务写业务表 + t_outbox_msg（保证消息不丢）
                 ▼
          SSE 推送通知给在线用户；失败消息进死信队列
```

## 📁 目录结构

```
CJAVAfeeds/
├── src/main/java/com/feed/
│   ├── common/           # 注解、切面、常量、异常、工具(JWT/分布式锁/雪花ID)
│   ├── pojo/             # entity / dto / vo / mq 消息体
│   └── server/
│       ├── config/       # Redis、RabbitMQ、缓存、MVC、Swagger 配置
│       ├── controller/   # 视频/用户/点赞/评论/关注/通知/上传 接口
│       ├── interceptor/  # JWT 登录拦截器
│       ├── mapper/       # MyBatis-Plus Mapper
│       ├── service/      # 业务接口与实现
│       ├── sse/          # SSE 连接管理器
│       └── worker/       # MQ 消费者、Outbox 定时补偿
├── src/main/resources/
│   ├── static/123c.html  # 前端单页应用
│   ├── mapper/           # XML SQL
│   └── application*.yml  # dev / prod 多环境配置
├── sql/                  # 索引优化脚本
├── uploads/              # 本地上传目录(不入库)
├── demo/                 # 演示视频
├── init.sql              # 数据库初始化脚本(7 张表)
├── Dockerfile / docker-compose.yml
└── pom.xml
```

## 🚀 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0
- Redis（带密码）
- RabbitMQ

### 1. 初始化数据库

```sql
-- 自动创建 cfeed 库及 7 张表，字符集 utf8mb4
source init.sql;
```

### 2. 准备中间件

默认连接本地，配置见 `application-dev.yml`，按实际环境修改：

| 中间件 | 默认地址 | 账号 / 密码 |
|--------|----------|-------------|
| MySQL | localhost:3306/cfeed | root / 123456 |
| Redis | localhost:6379 | 密码 123456 |
| RabbitMQ | localhost:5672 | guest / guest |

### 3. 启动

```bash
mvn spring-boot:run
```

启动成功后：

- 应用首页：http://localhost:8080/123c.html
- 接口文档：http://localhost:8080/swagger-ui/index.html
- 健康检查：http://localhost:8080/actuator/health

> 切换生产环境：`java -jar app.jar --spring.profiles.active=prod`，敏感配置通过环境变量注入。

### 4. Docker 部署

```bash
docker-compose up -d
```

## 🔑 核心设计说明

**1. Feed 流三级缓存**
本地 Caffeine 扛热点读、Redis 做分布式共享、MySQL 兜底；视频详情缓存 1 小时，视频更新 / 删除时主动清除缓存。

**2. 高并发点赞**
Redis `setIfAbsent` 加锁 + Lua 脚本原子解锁防止并发重复点赞；请求只写 Redis / 发 MQ 即返回，Worker 异步落库，消费端基于 Redis 做幂等，保证最终一致。

**3. 消息不丢**
业务数据与 `t_outbox_msg` 在同一事务写入，由定时任务扫描投递，避免"本地事务成功但 MQ 消息丢失"；消费失败且不重入队的消息进入 `feed.dlx.queue` 死信队列。

**4. 实时通知**
基于 SseEmitter 维护在线用户长连接；通知接口用 query 参数传 token，绕过登录拦截器，浏览器原生即可建立连接。

**5. 安全与防护**
JWT 无状态认证 + BCrypt；自定义 `@RateLimit` 注解 + AOP，基于 Redis 计数器实现接口级频控（如登录 60s/5 次、搜索 60s/30 次）。

## 📝 说明

- 仓库仅包含源码与配置，`uploads/` 视频与编译产物通过 `.gitignore` 排除。
- 生产环境请通过环境变量覆盖数据库 / Redis / RabbitMQ 密码，勿使用开发默认值。
