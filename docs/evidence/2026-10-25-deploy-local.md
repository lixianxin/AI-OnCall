# P1-1 部署验证报告（2026-10-25，本地 Docker 段）

> 目标：L1'「干净环境可部署」的本地段验证。云端服务器段待用户提供环境后补做。
> 诚实口径：**本次容器运行镜像的 jar 由宿主机构建**（宿主 Gradle 依赖缓存齐全），
> 容器内多阶段构建（含 Gradle 下载 + 依赖解析）在本机受限网络下未走通——
> 过程中发现并修复了 2 个仓库级缺陷 + 新增受限网络构建能力，详见下文。

## 验证结果（部署核心链路，全部通过）

| 验证项 | 结果 |
|---|---|
| 运行镜像构建（JRE21 + jar + docs） | ✅ |
| 容器内应用启动 | ✅ 1.4s 启动，DocumentStore 494 chunks / 43 文件全量加载，4 工具注册 |
| Dockerfile HEALTHCHECK | ✅ curl 8.18.0 存在于镜像，容器状态 `healthy` |
| 容器端口映射 | ✅ 宿主 8081 → 容器 8081 |
| 跨网络端到端 SSE | ✅ 宿主 curl → 容器 → `host.docker.internal` 回宿主 mock 上游 → 完整事件链 intent/tool/sources/content/done（raw：`retry-injection-raw/deploy_container_e2e.sse`） |
| Token 用量入指标 | ✅ llm_prompt 128 / llm_completion 9 |

复现（镜像 `ai-oncall-runtime:latest`，jar 需先 `./gradlew bootJar`）：

```bash
docker build -f Dockerfile.runtime -t ai-oncall-runtime:latest .
docker run -d --name aioncall -p 8081:8081 \
  -e DEEPSEEK_API_KEY=<key> \
  -e DEEPSEEK_BASE_URL=<上游地址，默认官方> \
  ai-oncall-runtime:latest
curl http://localhost:8081/health   # {"status":"UP","tools_registered":4}
```

## 过程中发现并修复的仓库级缺陷（干净环境构建的价值证明）

1. **CRLF 污染破坏 Linux 容器构建**（首次 `docker build` 于 Step 10 失败）：
   Windows 检出（autocrlf）把 `gradlew` 转成 CRLF，容器内 shebang `#!/bin/sh\r` 无法执行；
   且旧 Dockerfile 的 `./gradlew --version ... || true` 把同类失败静默吞掉，问题被推迟到更深处。
   - 修复：新增 `.gitattributes`（`gradlew`/`*.sh` 强制 LF、`*.bat` CRLF）治本；
     Dockerfile 增加防御性 `sed -i 's/\r$//' gradlew` 对任意检出配置免疫；移除静默 `|| true`。
2. **Dockerfile 缓存层与 ARG 作用域问题**：
   `FROM` 前声明的 ARG 在 stage 内不可见（须在 stage 内重新声明）；Gradle 发行包预下载
   与 init 脚本 COPY 的层序错误会导致改 init 脚本触发 130MB 重下载。均已修正。
3. **受限网络构建能力（新增）**：本机容器对 gradle/maven 官方仓库存在 DNS/连接抖动
   （`services.gradle.org` 连接失败、`plugins-artifacts.gradle.org` DNS 解析失败、
   Gradle Central 插件解析超时，均为多次构建实测）。
   - 新增 `--build-arg GRADLE_DIST_URL=<镜像URL>`（Gradle 发行包镜像）与
     `--build-arg USE_MIRROR=on`（`docker/mirror-init.gradle`：pluginManagement 与
     buildscript/project repositories 双路径切阿里云——**只覆盖 buildscript 不够**，
     `plugins {}` DSL 走 pluginManagement，是第二次实测踩坑）；
   - 新增 `.dockerignore`（剔除 25M `.git` 与无关目录）；
   - 默认参数仍是官方源，镜像行为对常规环境不变。

## 遗留

- [ ] **容器内多阶段构建走通**：本机网络对官方/镜像源的连通性均不稳定（阿里云源也超时），
  该验证项移至云服务器执行（预期网络条件更好）；届时 P1-1 完整闭环。
- [ ] 云服务器部署 + 公网可达性验证（等环境）。
- [ ] `docker compose` 插件本机不可用（独立版 Docker），compose 文件的编排验证随云端一起做。
