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
@RSocketRpcDiscorvery("catalog-service")
public interface CatalogApi {
    @RSocketRpcType
    Mono<String> findName();
}
~~~

这是 API 结构示意，当前示例没有 CatalogApi RPC 服务。实际使用还需要：

1. 服务端实现类使用 `@RSocketRpcApi` 并被相应扫描器发现。
2. 客户端字段使用 `@RSocketRpc`。
3. 配置 TCP 服务监听端口和 Nacos 服务信息。
4. 两端保持兼容的 DTO、路由生成约定和 MIME。
5. 校验身份传递和异常转换。

注解保留历史拼写 `Discorvery` 及属性 `MimeType`。当前代理按 Mono / Flux 与 MIME 选择调用方式，复杂嵌套泛型需要专门验证反序列化。

### 启用内部 Fory RPC

Fory 与 Jackson 使用各自的数据 MIME，Requester 按 MIME 分开缓存连接。外部 HTTP 网关继续发 JSON，身份元数据也继续使用 JSON。Java 调用方和服务端均引入 `lambda-framework-rsocket` 并使用 `RSocketStrategiesConfig`。

两端在应用配置中以 Spring Bean 声明本次 RPC 会交换的应用类型。例如 README 的 `ChatMessageFacade` 可以注册如下类型。

~~~java
@Bean
ForyRpcTypes messageForyTypes() {
    return new ForyRpcTypes(List.of(
        ChatMessageFacade.FindRequest.class,
        ChatMessageFacade.Detail.class
    ));
}
~~~

`ForyRpcTypes` 位于 `org.lambda.framework.rsocket.adapter`。将契约图中的自定义嵌套 record、枚举和分页包装类型一并注册。框架为 `ObjectId` 提供 12 字节编解码。应用类型按 Java 全限定名注册，两端使用同一份 Facade 契约，避免独立维护数字类型编号。Fory 使用 Java 原生兼容模式，保持类型注册校验。[Apache Fory 类型注册](https://fory.apache.org/docs/object-serialization/java/type-registration/)

在调用方项目的 Nacos Data ID 中配置：

~~~properties
lambda.rpc.data-mime-type=application/x-lambda-fory
~~~

省略该配置时，`@RSocketRpcType` 默认选用 Jackson JSON。也可给单个方法显式指定 `@RSocketRpcType(MimeType = RSocketRpcType.FORY_MIME_TYPE)`，或显式指定 `application/json` 供迁移期间使用。发布时先升级接收方并注册共享类型，再切换调用方配置。按启动期配置发布并重启应用。请求失败时沿用该次调用所选的协议。

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
