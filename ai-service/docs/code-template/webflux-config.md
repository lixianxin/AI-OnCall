# WebFlux 配置 — AI OnCall 项目

本项目使用 WebFlux（非 Spring MVC），CORS 统一在配置类中管理。

## 当前项目配置

`java
// ai-service/src/main/java/com/oncall/ai/config/WebFluxConfig.java
@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(""/api/**"")
            .allowedOrigins(""*"")
            .allowedMethods(""POST"", ""GET"", ""OPTIONS"")
            .allowedHeaders(""*"")
            .maxAge(3600);
    }
}
`

## 扩展：添加拦截器

`java
@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(""/api/**"")
            .allowedOrigins(""*"")
            .allowedMethods(""GET"", ""POST"", ""PUT"", ""DELETE"", ""OPTIONS"")
            .allowedHeaders(""*"")
            .maxAge(3600);
    }

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024); // 10MB
    }
}
`

## 相关来源

architecture/system-overview.md → WebFluxConfig.java