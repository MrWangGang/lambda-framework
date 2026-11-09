# RSocket 连接、身份传递与虚拟线程

[返回总览](../README.md) · [用户体系](USER-AND-SECURITY.md) · [维护手册](OPERATIONS.md)

本章记录源码结构、只读检查结论和待实现方案。**本轮没有修改 RSocket 或虚拟线程源码，没有将跨服务故障恢复列为已通过测试。**

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

## 3. 拦截器与握手的关系

[RSocketAcceptorInterceptor](../lambda-framework-rsocket/src/main/java/org/lambda/framework/rsocket/adapter/RSocketAcceptorInterceptor.java) 在 accept 时将 SETUP 交给下游 SocketAcceptor，再包装返回的 RSocket。

随后处理 requestResponse、requestStream、fireAndForget、requestChannel、metadataPush：

- 从请求元数据查找 JSON 身份信息。
- 解析为 SecurityStash。
- 用 contextWrite 写入 Reactor Context。
- 委托实际业务 RSocket。
- 通过框架异常处理器转换部分错误。

该类位于连接接受链中，但大量逻辑发生在每次请求处理时。业务请求失败、身份失败和握手失败应分别定位。

## 4. 已识别的连接管理风险

| 编号 | 当前实现 | 条件与影响 |
|---|---|---|
| RPC-01 | 多实例 LoadbalanceTarget 使用同一 serviceId 作为 key | 同服务多个地址身份冲突，滚动更新可能保留旧目标 |
| RPC-02 | requester 缓存先 get 再 put | 并发首次调用可能重复创建客户端，缺少统一释放 |
| RPC-03 | 复用可变 Builder，缓存未区分 MIME | 并发构造可能相互影响 |
| RPC-04 | 同步 DiscoveryClient 查询在缓存判断前 | 已有连接仍依赖发现查询，可能阻塞调用线程 |
| RPC-05 | 周期发现流无错误恢复；为空时不发空列表 | 更新可能终止；全部下线时旧目标未及时清空 |
| RPC-06 | 注册使用配置端口而非事件实际端口 | 随机端口或混合协议服务可能登记错误端口 |

这是源码分析，实际现象需要对应场景验证。不能断言所有连接都一定不稳定。

LoadbalanceTarget 的 key 用于唯一标识目标，应该区分实例地址和必要身份。[RSocket API](https://javadoc.io/static/io.rsocket/rsocket-core/1.1.5/io/rsocket/loadbalance/LoadbalanceTarget.html)

## 5. 元数据、异常与认证边界

- 固定按 CompositeMetadata 解析，需核对 SETUP 协商的 metadata MIME。
- 收到 principal JSON，不等于验证调用方身份和权限。
- 部分解析在返回 Mono / Flux 之前执行，错误可能绕过该方法的自定义 onErrorResume。
- RSocket 1.1.5 普通请求路径有上游异常包装，不能直接描述为“同步解析失败必然断开整条连接”。
- requestChannel 首个信号没有 Payload 时返回通用错误，可能覆盖上游原始错误。
- 全局异常处理器对没有 message 的异常仍调用 message.indexOf，存在二次空指针风险。
- ByteBuf / Payload 引用计数需专项验证；仅看到 wrappedBuffer 无 release 不能证明直接内存泄漏。

无显式 reconnect 配置也不等于 Spring RSocketRequester 永远不能重连。应按实际客户端复用、失效和重建行为测试，再补超时、退避、重试与幂等策略。

## 6. 排查顺序

1. 确认应用监听的是 TCP RSocket 端口。
2. 从调用方检查端口和 Nacos 登记的 IP / port / healthy。
3. 分别测试单实例直连和服务发现，缩小范围。
4. 核对数据 MIME、元数据 MIME、路由和两端 DTO。
5. 分开记录连接关闭原因、请求错误、认证失败、重试次数。
6. 模拟服务重启、实例替换、发现为空、短暂断网、畸形元数据。
7. 检查连接数、缓存释放、恢复时间以及写请求是否重复执行。
8. 验证取消和 channel/stream 的 Payload 释放。

现有实机验收是示例 HTTP 与中间件，不包括这些 RSocket 场景。

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
- Web 的工厂注册类型与实现接口不匹配。
- Gateway 的配置入口在非标准资源目录，使用 ApplicationContextInitializer，需核验打包及启动时序。
- Web / RSocket 还强制 web-application-type=none，可能影响 HTTP 应用。
- 开关生效不表示每条业务链都经过 boundedElastic。

后续应统一早期入口，允许应用覆盖，并用线程探针确认。当前未完成改造。

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
