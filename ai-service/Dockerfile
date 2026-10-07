# ── 构建阶段：JDK21 + Gradle 标准构建 ─────────────────────────────
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 先拷构建脚本利用 Docker 层缓存
COPY ai-service/gradle/ ai-service/gradle/
COPY ai-service/gradlew ai-service/gradlew.bat ai-service/build.gradle ai-service/settings.gradle ai-service/
WORKDIR /app/ai-service
RUN chmod +x gradlew && ./gradlew --version > /dev/null 2>&1 || true

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
