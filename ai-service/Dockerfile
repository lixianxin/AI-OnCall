# ── 构建阶段：JDK21 + Gradle 标准构建 ─────────────────────────────
# 发行包默认走腾讯镜像（仓库 wrapper 同源，官方源在受限网络不稳定，2026-10-25 实测）；
# 官方源：--build-arg GRADLE_DIST_URL=https://services.gradle.org/distributions/gradle-9.5.1-bin.zip
# 依赖仓库镜像：--build-arg USE_MIRROR=off 可关闭（默认 on，走阿里云）
FROM eclipse-temurin:21-jdk AS build
ARG GRADLE_DIST_URL=https://mirrors.cloud.tencent.com/gradle/gradle-9.5.1-bin.zip
ARG USE_MIRROR=on
WORKDIR /app

# 先拷构建脚本利用 Docker 层缓存
COPY ai-service/gradle/ ai-service/gradle/
COPY ai-service/gradlew ai-service/gradlew.bat ai-service/build.gradle ai-service/settings.gradle ai-service/
WORKDIR /app/ai-service
# 防御性去 CRLF：Windows 检出可能把 gradlew 转成 CRLF，Linux 容器内 shebang 带 \r 会 exec 失败
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew \
    && sed -i "s|distributionUrl=.*|distributionUrl=${GRADLE_DIST_URL}|" gradle/wrapper/gradle-wrapper.properties
# 预下载 Gradle 发行包进独立缓存层：只改源码重建时无需重新下载
RUN ./gradlew --version > /dev/null 2>&1 || true

# 镜像 init 脚本放预下载层之后：改 init 脚本不触发发行包重新下载
COPY ai-service/docker/mirror-init.gradle /tmp/mirror-init.gradle
RUN if [ "${USE_MIRROR}" = "on" ]; then mkdir -p /root/.gradle/init.d && cp /tmp/mirror-init.gradle /root/.gradle/init.d/mirror.gradle; fi

COPY ai-service/src/ /app/ai-service/src/
# 知识库文档（DocumentStore 启动加载）
COPY ai-service/docs/ /app/ai-service/docs/

WORKDIR /app/ai-service
RUN ./gradlew bootJar --console=plain --no-daemon

# ── 运行阶段：最小 JRE21 镜像 ─────────────────────────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/ai-service/build/libs/*.jar app.jar

ENV SERVER_PORT=8081 \
    DEEPSEEK_API_KEY= \
    ONCALL_DOCS_PATH=/app/docs
EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD curl -sf http://localhost:8081/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
