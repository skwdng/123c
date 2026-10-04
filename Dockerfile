# ============================================================
# 多阶段构建：第一阶段用 Maven 编译打包，第二阶段只带 JRE 运行
# 好处：最终镜像不含源码和 Maven，体积小、更安全
# ============================================================

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# 先拷 pom 单独拉依赖，利用 Docker 分层缓存：代码没变时依赖层不重新下载
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 再拷源码打包（跳过测试，测试在 CI 里单独跑）
COPY src ./src
RUN mvn package -DskipTests -B

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# 只从构建阶段取打好的 fat jar
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 9090

# 默认 dev 环境，部署时用 -e SPRING_PROFILES_ACTIVE=prod 覆盖
ENTRYPOINT ["java", "-jar", "app.jar"]
