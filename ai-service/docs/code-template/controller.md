# 生成 Controller — AI OnCall 项目风格

本项目使用 Spring Boot WebFlux（非 Spring MVC），所有 Controller 返回 Flux 或 Mono。

## 参考代码

`java
// ai-service/src/main/java/com/oncall/ai/controller/ChatController.java
@RestController
public class ChatController {

    @PostMapping(
        value = ""/api/chat/stream"",
        produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<ServerSentEvent<String>> streamChat(@RequestBody ChatRequest request) {
        return orchestrator.stream(conversationId, request.message())
            .map(event -> ServerSentEvent.<String>builder()
                .event(""message"")
                .data(serializeEvent(event))
                .build());
    }
}
`

## 生成规则

- 使用 @RestController（非 @Controller）
- 返回 Flux<T> 或 Mono<T>（WebFlux 响应式）
- SSE 端点需指定 produces = TEXT_EVENT_STREAM_VALUE
- CORS 在 WebFluxConfig 中统一配置，不在 Controller 上加

## 完整模板

`java
@RestController
@RequestMapping(""/api/"")
public class Controller {

    private final Service service;

    public Controller(Service service) {
        this.service = service;
    }

    @GetMapping
    public Flux<> list() {
        return service.findAll();
    }

    @GetMapping(""/{id}"")
    public Mono<> getById(@PathVariable String id) {
        return service.findById(id);
    }

    @PostMapping
    public Mono<> create(@RequestBody @Valid  dto) {
        return service.create(dto);
    }
}
`

## 相关来源

architecture/system-overview.md → ChatController.java
api/ai-service-api.md