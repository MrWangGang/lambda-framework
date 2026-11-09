# RSocket 连接、身份传递与虚拟线程

[返回总览](../README.md) · [用户体系](USER-AND-SECURITY.md) · [维护手册](OPERATIONS.md)

本章记录当前 RSocket 路由、Channel 修复、网关桥接及线程模型。跨服务联调使用独立服务与网关，生产环境故障恢复需单独验证。

## 1. 调用链

~~~text
业务字段 @RSocketRpc → 客户端代理 → 服务名 / 直连地址解析
→ RSocketLoadbalance / RSocketRequester → Reactor Netty TCP
→ RSocketAcceptorInterceptor → 路由与业务方法 → Mono / Flux 响应
~~~

common 提供注解；rpc 生成代理/路由；loadbalance 管理 requester；rsocket 处理协议适配；nacos 提供发现与端口注册。

## 2. 注解与接口约定

~~~java
public interface CatalogApi {
    @MessageMapping({"catalog-service.catalogApi.findName", "/api/catalog/findName"})
    Mono<String> findName();
}
~~~

这是 API 结构示意，当前示例没有 CatalogApi RPC 服务。实际使用还需要：

1. 服务端以 `@Controller` 实现 Facade，继承 Facade 方法的映射。
2. 客户端字段使用 `@RSocketRpc`。
3. 配置 TCP 服务监听端口和 Nacos 服务信息。
4. 两端使用兼容的 Facade record 与路由约定。业务服务统一使用 `@MessageMapping`。
5. 校验身份传递和异常转换。

使用方法级 `@MessageMapping` 时，点分隔的路由首段对应服务发现名。同一个 Facade 使用同一服务名。旧项目可继续使用 `@RSocketRpcDiscorvery` 与约定路由。返回 `Mono` 是单次响应，普通请求返回 `Flux` 是单向流。双向流要求入参 `TFlux<I>` 和返回值 `TFlux<O>` 配对，错误签名在启动阶段拒绝。

### 内部 Java RPC 使用 Fory

原生 Java RPC 固定使用 `application/x-lambda-fory`。网关使用独立 JSON 连接访问以 `/` 开头的业务映射，身份元数据使用 JSON。Requester 按数据 MIME 分开缓存连接。Java 调用方和服务端均引入 `lambda-framework-rsocket` 并使用 `RSocketStrategiesConfig`。

框架从调用方已注入的 Facade 和服务端原生路由收集类型。已声明的 record、嵌套泛型、枚举和 `Paged<T>` 自动注册。框架为 `ObjectId` 提供 12 字节编解码，Fory 保持类型注册校验。旧项目使用非 record DTO 或未出现在签名中的实际子类型时，可以继续用 `ForyRpcTypes` 显式注册。

发布时两端使用兼容的 Facade 契约。业务网关使用 `rb://` 转发。服务端发布固定 `@MessageMapping` 路径的签名后，网关按 Mono、Flux、TFlux 自动选择交互。未发布路径返回 404，契约不可用返回 503。

业务服务导入 `RsocketInterceptorConfig`，设置 `spring.main.web-application-type=none` 和 `spring.rsocket.server.transport=tcp`。只把 reactive 网关的 HTTP 端口对外开放。服务端 TCP 端口限网关与授权服务访问。

## 3. 拦截器与握手的关系

[RSocketAcceptorInterceptor](../lambda-framework-rsocket/src/main/java/org/lambda/framework/rsocket/adapter/RSocketAcceptorInterceptor.java) 在 accept 时将 SETUP 交给下游 SocketAcceptor，再包装返回的 RSocket。

随后处理 requestResponse、requestStream、fireAndForget、requestChannel、metadataPush：

- 从请求元数据查找 JSON 身份信息。
- 解析为 SecurityStash。
- 用 contextWrite 写入 Reactor Context。
- 委托实际业务 RSocket。
- 通过框架异常处理器转换部分错误。

该类位于连接接受链中，但大量逻辑发生在每次请求处理时。业务请求失败、身份失败和握手失败应分别定位。

## 4. Requester 与服务发现

缓存按目标、端口、数据 MIME 和每实例连接数区分，首次构建使用 computeIfAbsent。应用关闭时统一释放客户端。服务发现从请求热路径移入周期刷新，实例 key 使用 host:port。临时发现异常保留最后一次成功结果并继续刷新，空结果会发布空目标列表。服务端和客户端的 Payload 重组上限默认 16 MiB。已建立的服务发现连接断开时，连接池先摘除旧连接，框架经 100–250 ms 随机延迟重发最近目标列表。连接池收到新请求后建立缺失连接。TCP 建连失败按退避重试，已发出的 RPC 请求不会自动重放。

发现、滚动替换和 Nacos 实际登记地址要结合部署验收。RSocket 端口与 HTTP 端口分开配置。应用可覆盖 RSocket 模块的默认运行类型。

## 5. 元数据、异常与取消

- 业务数据 MIME 与身份元数据分开，身份条目使用 JSON。身份内容应来自可信入口。
- requestChannel 区分首个数据、错误与完成信号，保留输入的首条消息和单次订阅。
- 统一异常转换覆盖整个 channel，并在空消息时保留原因和标准错误码。
- 正常、错误、按需请求、取消以及 Payload 引用计数通过本机真实 TCP 验证。
- 独立处理器拒绝数据时遵守其资源所有权。网关 NDJSON 流使用有界单条缓冲，后端请求与响应流保持可取消。

## 6. 排查顺序

1. 确认应用监听的是 TCP RSocket 端口。
2. 从调用方检查端口和 Nacos 登记的 IP / port / healthy。
3. 分别测试单实例直连和服务发现，缩小范围。
4. 核对数据 MIME、元数据 MIME、路由和两端 DTO。
5. 分开记录连接关闭原因、请求错误、认证失败、重试次数。
6. 模拟服务重启、实例替换、发现为空、短暂断网、畸形元数据。
7. 检查连接数、缓存释放、恢复时间以及写请求是否重复执行。
8. 验证取消和 channel/stream 的 Payload 释放。

现有验收覆盖双服务、网关、TCP 错误与取消。实例替换、长连接断网及生产代理配置需要按部署条件验收。

## 7. 虚拟线程接入

JDK 27 + Reactor 3.8.8 支持共享 boundedElastic 使用虚拟线程，业务继续返回 Mono / Flux。

~~~properties
spring.threads.virtual.enabled=true
~~~

Boot 4.1.2 的 ReactorEnvironmentPostProcessor 会在启动阶段设置：

~~~properties
reactor.schedulers.defaultBoundedElasticOnVirtualThreads=true
~~~

后者是 JVM 系统属性。脱离 Boot 时可用 `-D` 设置，必须早于 Schedulers 初始化；将同名键写入普通配置文件不等于设置 JVM 属性。[Boot 实现](https://raw.githubusercontent.com/spring-projects/spring-boot/v4.1.2/module/spring-boot-reactor/src/main/java/org/springframework/boot/reactor/ReactorEnvironmentPostProcessor.java)

## 8. 现有配置的待整理点

- Web / RSocket 已有 spring.threads.virtual.enabled 配置类。
- Gateway 的配置入口在非标准资源目录，使用 ApplicationContextInitializer，需核验打包及启动时序。
- 网关以 `spring.main.web-application-type=reactive` 启动。Web / RSocket 的线程配置由应用覆盖。
- 开关生效不表示每条业务链都经过 boundedElastic。

验证业务是否实际经过 boundedElastic，可通过线程探针确认。

## 9. 执行边界

| 工作 | 建议 |
|---|---|
| 同步 SDK、文件、可能阻塞的服务发现 | fromCallable / defer 包装，再调度到虚拟线程 |
| 响应式 MongoDB / Redis / HTTP I/O | 保留驱动机制，按需调度后续处理 |
| Netty 网络收发 | 保留事件循环 |
| 计算密集任务 | 限制并行度，使用适合计算的调度器 |
| RabbitMQ 已有 boundedElastic 任务 | 随统一开关启用；客户端内部线程另查 |

~~~java
return Mono.fromCallable(() -> discoveryClient.getInstances(serviceName))
    .subscribeOn(Schedulers.boundedElastic());
~~~

实际同步调用必须放进回调。`Mono.just(blockingCall())` 已在组装时执行调用，之后追加调度器不能转移已经发生的阻塞。

Reactor 没有承接所有操作符的总线程池；异步数据源回调也可能改变执行线程。入口加一次 subscribeOn 不保证整个请求始终在同一虚拟线程。[Reactor 调度说明](https://projectreactor.io/docs/core/release/reference/coreFeatures/schedulers.html)

## 10. 上下文和资源生命周期

- 身份与响应式事务继续使用 Reactor Context。
- 普通 ThreadLocal / MDC 不会自动跨响应式线程传递。
- 业务链不自行 subscribe，由 WebFlux / RSocket 边界订阅。
- 长生命周期 MQ 消费者可以独立订阅，但需持有 Disposable 并关闭；示例已有此处理。
- 超时和取消不保证远程副作用撤销，写操作仍需幂等。
- 虚拟线程数不等于连接数，需要限制外部服务并发与排队。
- 无须 fork Reactor，也不建议全局给每个操作符强制切线程。

## 11. 后续验收标准

1. 在适配层使用 `Thread.currentThread().isVirtual()` 验证。
2. 验证 HTTP → RPC → 数据库后的身份、事务上下文。
3. 覆盖超时、取消、错误传播、关闭释放。
4. 对照吞吐、P95/P99、CPU、内存、连接数和排队长度。
5. 模拟慢阻塞调用，确认不拖住 Netty 事件循环。
6. 核对 SDK 自带线程池，不宣称外层虚拟线程能替换其全部内部线程。
7. 验证数据库饱和、并发上限与拒绝策略。

这是验收计划，不是性能报告。虚拟线程不能代替第 4、5 节连接和异常问题的修复。
