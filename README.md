# Lambda Framework

面向 **JDK 27 + Spring Boot 4 + Project Reactor + RSocket + Apache Fory + Nacos** 的全响应式微服务研发框架。框架以零阻塞、零反射热路径、二进制内网传输、自适应负载均衡和严格领域契约工程化为核心，兼顾高并发低延迟与团队一致性。

> 本文档既是 **框架使用手册**，也内置了基于 Lambda Framework 构建业务的 **强制开发规范**（与框架仓库内置技能 [lambda-java-backend](./skills/lambda-java-backend/SKILL.md) 保持一致）。

---

## 目录

- [1. 运行基线与核心特性](#1-运行基线与核心特性)
- [2. 模块架构总览](#2-模块架构总览)
- [3. 快速开始与源码安装](#3-快速开始与源码安装)
- [4. 核心机制与高并发架构](#4-核心机制与高并发架构)
  - [4.1 Reactive 网关与 RSocket 零拷贝透传](#41-reactive-网关与-rsocket-零拷贝透传)
  - [4.2 P2C 自适应负载均衡与多通道隔离](#42-p2c-自适应负载均衡与多通道隔离)
  - [4.3 原生 RPC 与 Apache Fory 编解码](#43-原生-rpc-与-apache-fory-编解码)
  - [4.4 Redis RESP3 服务端追踪缓存与 JWT 会话](#44-redis-resp3-服务端追踪缓存与-jwt-会话)
- [5. 组件使用手册与配置说明](#5-组件使用手册与配置说明)
  - [5.1 启动引导与 Nacos 配置](#51-启动引导与-nacos-配置)
  - [5.2 RSocket、网关与 WebSocket 双向通信](#52-rsocket网关与-websocket-双向通信)
  - [5.3 安全认证与请求上下文 (`PrincipalFactory`)](#53-安全认证与请求上下文-principalfactory)
  - [5.4 MongoDB 响应式仓储、审计、聚合与分页](#54-mongodb-响应式仓储审计聚合与分页)
  - [5.5 Redis 缓存与分布式事务操作](#55-redis-缓存与分布式事务操作)
  - [5.6 Qdrant 向量检索](#56-qdrant-向量检索)
  - [5.7 分布式锁（Redisson / ZooKeeper）](#57-分布式锁redisson--zookeeper)
  - [5.8 消息队列（RabbitMQ 延迟消息 / Kafka）](#58-消息队列rabbitmq-延迟消息--kafka)
  - [5.9 第三方 HTTP 客户端 (`ReactiveHttpClient`)](#59-第三方-http-客户端-reactivehttpclient)
  - [5.10 无锁雪花 ID (`GuidFactory`) 与启动健康检查](#510-无锁雪花-id-guidfactory-与启动健康检查)
- [6. 内置开发规范（强制）](#6-内置开发规范强制)
  - [6.1 仓库、包名与模块化规范](#61-仓库包名与模块化规范)
  - [6.2 分层职责与依赖注入](#62-分层职责与依赖注入)
  - [6.3 接口契约、record 命名与数值精度](#63-接口契约record-命名与数值精度)
  - [6.4 MongoDB 建模、ObjectId、索引与原子并发](#64-mongodb-建模objectid索引与原子并发)
  - [6.5 空值处理与统一异常码契约](#65-空值处理与统一异常码契约)
  - [6.6 第三方接口与 Docker 交付禁令](#66-第三方接口与-docker-交付禁令)

---

## 1. 运行基线与核心特性

| 维度 | 栈与版本基线 |
|---|---|
| **JDK / JVM** | Java 27 (`<maven.compiler.release>27</maven.compiler.release>`)，生产推荐 ZGC (`-XX:+UseZGC -XX:+ZUncommit`) |
| **Core 框架** | Spring Boot `4.1.2`、Spring Cloud `2025.1.4`、Spring Cloud Alibaba `2025.1.0.0` |
| **服务注册/配置** | Nacos `3.3.0`（支持 gRPC 服务订阅、契约版本事件推送） |
| **RPC 通信与序列化** | RSocket TCP、Apache Fory `1.7.7`（内网二进制零反射 RPC）、Jackson 2/3（外网 JSON + ObjectId 自动转换） |
| **数据与向量** | MongoDB Reactive（BSON ObjectId、响应式审计、事务）、Redis Reactive（RESP3 广播失效）、Qdrant gRPC |
| **消息与锁** | Reactor RabbitMQ（`x-delayed-message` 延迟队列）、Reactor Kafka、Redisson `4.8.0`、Curator `5.9.0` |
| **线程模型** | Project Reactor 事件循环 + `RsocketVirtualThreadConfig` 自动启用 JDK 27 Virtual Threads |

**核心架构亮点：**

1. **全链路 Zero-Copy 网关透传**：HTTP JSON / NDJSON 请求载荷由 Netty `DataBuffer` 直接封装为零拷贝 `Payload` 送入 RSocket，后端返回 JSON 直接经网关注入统一 `ResponseTemplete` 外壳输出，无冗余字符串/对象反序列化。
2. **三通道 I/O 资源物理隔离**：网关到后端按交互模型拆分为 `UNARY`（单次请求）、`STREAM`（流式输出）、`CHANNEL`（双向流/WSS）三套独立 EventLoop 池，Linux 下优先使用 Netty 原生 `EpollEventLoopGroup`。
3. **P2C 自适应负载均衡 + 路由契约版本缓存**：多实例部署时使用 Power of Two Choices 随机双选 + 并发数与 EWMA 延迟联合评分；单实例自动切换零开销直连路径；客户端取消不误伤后端节点延迟统计；配合 `GatewayRouteVersionInterceptor` 与后台定时预热消除冷启动抖动。
4. **Redis RESP3 客户端本地会话缓存**：`PrincipalFactory` 基于 Lettuce 建立 RESP3 `CLIENT TRACKING ON BCAST NOLOOP` 推送失效链路，热请求 O(1) 本地命中 Guava 会话缓存并预编码 token 字节比对，异常断线后按 5 秒冷却窗口自动恢复。
5. **无锁雪花 ID 与高并发细节**：`GuidFactory` 采用单 `AtomicLong` 无锁 CAS 编码毫秒时间戳与序列号；`SecurityConfig` 对静态白名单执行 O(1) 哈希精确匹配；RSocket 服务端强制使用 `PathPatternRouteMatcher` 替代低效正则匹配。

---

## 2. 模块架构总览

框架采用 Maven 多模块管理（`org.lambda.framework:lambda-framework:1.0.0-SNAPSHOT`），共包含 **14 个功能子模块**：

| 模块 (`artifactId`) | 核心职责 |
|---|---|
| `lambda-framework-enum` | 全框架统一错误码契约 `ExceptionEnumFunction` 及各组件 `*ExceptionEnum` |
| `lambda-framework-common` | `@RSocketFacade`/`@RSocketRpc` 注解、`UnifyPO`、`ScopedLoginUser`、`UiFlux`/`BiFlux`、`JsonUtil`/`OptionalUtil`/`ReactiveValues`、`ComponentCheck`、`ObjectIdJsonModules` |
| `lambda-framework-rsocket` | RSocket 服务端拦截器、`RSocketStrategiesConfig`、`PathPatternRouteMatcher`、`ForyRpcCodec`、二进制 `IdentityStashEncoder/Decoder`、全局异常序列化 |
| `lambda-framework-loadbalance` | `AdaptiveRSocketPool`、`RSocketLoadbalance`、三类流量 EventLoop 隔离、Netty Keepalive 与心跳保活 |
| `lambda-framework-rpc` | `@RSocketRpc` 声明式客户端动态代理（编译期契约校验与 MethodHandle/Direct 调度） |
| `lambda-framework-gateway` | Reactive HTTP / NDJSON / WSS 网关转发引擎 `RsocketRequestFactory`、路由契约预热、SSE/WSS 流式背压与超时控制 |
| `lambda-framework-nacos` | Nacos 实例源适配器、`RsocketRunListener` RSocket 端口及路由契约版本号注册发布 |
| `lambda-framework-httpclient` | 基于 WebClient 与共享 `ConnectionProvider` 的非阻塞外部 HTTP 客户端、腾讯云 TC3 签名器 |
| `lambda-framework-repository` | Reactive MongoDB 自动配置、空值策略、枚举与时间转换、审计更新 `MongoAuditUpdates`、分页 `ReactiveUnifyPagingRepositoryOperation`、Redis `ReactiveRedisOperation`、Qdrant gRPC 封装及 `ReactiveQdrantRepository` |
| `lambda-framework-compliance` | `PrincipalFactory`（会话签发/校验/RESP3 广播追踪缓存）、`JwtSessionTokens`（RS256 JWT + JWK 验签）、`DefaultBasicServiceImpl` / `DefaultTreeServiceImpl` |
| `lambda-framework-security` | Reactive Gateway Spring Security 鉴权过滤器链、精确+通配符白名单、身份帧透传 |
| `lambda-framework-guid` | 无锁 Snowflake ID 生成器 `GuidFactory` |
| `lambda-framework-lock` | Redisson 响应式分布式锁（`ReactiveRedissonLock`）、ZooKeeper Curator 分布式锁 |
| `lambda-framework-mq` | Reactor RabbitMQ（含延迟交换机封装 `RabbitMQReactiveOperate`）与 Reactor Kafka（`KafkaMQReactiveOperate`） |

---

## 3. 快速开始与源码安装

### 3.1 安装框架到本地 Maven 仓库

框架禁用远端制品仓库发布（`maven.deploy.skip=true`），需在本机使用 JDK 27 安装至本地 `~/.m2`：

```bash
git clone git@github.com:MrWangGang/lambda-framework.git
cd lambda-framework
mvn clean install
```

### 3.2 业务服务引用示例

业务微服务父工程 `pom.xml` 直接导入框架 POM 或将各模块纳入 `<dependencyManagement>`，在 `-core` 模块按需引入功能模块即可：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.lambda.framework</groupId>
            <artifactId>lambda-framework</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

典型微服务 `-core` 模块依赖组合：

- **业务微服务（MongoDB + Redis + RSocket + RPC + Compliance）**：
  `lambda-framework-rsocket`、`lambda-framework-rpc`、`lambda-framework-nacos`、`lambda-framework-repository`、`lambda-framework-compliance`、`lambda-framework-guid`
- **Reactive 网关（Gateway + Security + Loadbalance + Nacos + Compliance）**：
  `lambda-framework-gateway`、`lambda-framework-security`、`lambda-framework-loadbalance`、`lambda-framework-nacos`、`lambda-framework-compliance`
- **按需增强组件**：
  `lambda-framework-repository`（含 MongoDB/Redis/Qdrant 向量库）、`lambda-framework-lock`（Redisson/ZK 锁）、`lambda-framework-mq`（RabbitMQ/Kafka）、`lambda-framework-httpclient`（外部 HTTP 调用）

启动类固定使用标准 `@SpringBootApplication`，由 Spring Boot `AutoConfiguration.imports` 按连接配置自动装配与启动自检。

---

## 4. 核心机制与高并发架构

### 4.1 Reactive 网关与 RSocket 零拷贝透传

网关通过 `GatewayFilter` 与 `RsocketRequestFactory` 将公网 HTTP POST 与 WSS 请求桥接为内网 RSocket TCP 调用：

- **Zero-Copy Payload 封装**：网关直接消费请求体 `DataBuffer`（`DataBufferUtils.join` + `byte[]`/`NettyDataBuffer` 零额外序列化），按服务返回的 `GatewayInteraction` 协议模式选择 `request/response`、`request/stream` 或 `request/channel`。
- **路径契约自动同步 (`GatewayRouteContract`)**：后端服务启动后自动汇总所有 `@MessageMapping("/api/...")` 的交互类型（`UNARY`、`STREAM`、`NDJSON`、`CHANNEL`），生成契约指纹版本号并注册到 Nacos 元数据；网关拦截器对比响应头版本号，路由变更时自动触发刷新，同时 `RsocketRouteWarmup` 后台定期核对预热，避免首包未知交互模式引发 404/503。
- **有界帧与超时保护**：内置最大请求体（默认 `1MB`）、最大响应体（默认 `4MB`）、流式最大条数（默认 `512`）与单次/首包超时（默认 `15s`），时间轮驱动快速淘汰超时帧。
- **JSON 严格校验开关**：`lambda.gateway.rsocket.strict-json-validation` 默认 `false`（直接透传字节以追求极限性能），按需开启时会对 JSON 入参做严格语法校验。

### 4.2 P2C 自适应负载均衡与多通道隔离

`RSocketLoadbalance` 与 `AdaptiveRSocketPool` 负责全部内网 RSocket 连接维护：

- **三通道隔离 (`RSocketClientResources`)**：
  - `unary`（默认 `0` = 自动按 CPU 核心数分配）：专用于短平快 HTTP RPC / 网关 `Mono` 转发；
  - `stream`（默认 `2`）：专用于服务端推送/NDJSON 流式拉取；
  - `channel`（默认 `2`）：专用于长连接全双工聊天与流式对话，防止大流或长连接拖慢普通短请求响应。
- **连接池与健康探活**：默认每个后端实例维持 `2` 条 TCP 长连接（`connections-per-instance=2`），启用 TCP Keepalive、20 秒心跳探测与 60 秒空闲断线重连（`max-inbound-payload-bytes` 默认 `16MB`）。
- **单/多实例自适应路径**：
  - **单实例**：`AdaptiveRSocketPool` 自动跳过 `nanoTime()` 与原子计数器 CAS 竞争，走零分配直连快速路径；
  - **多实例**：开启 `adaptive-load-balancing=true` 时采用 P2C（Power of Two Choices）双节点抽样，依据 `(pending + 1) * ewmaLatency` 选择最优实例，并以 `1/256` 概率探测冷节点恢复状态；客户端主动取消（Cancel）仅归还 `pending` 计数而不计入错误延迟惩罚。

### 4.3 原生 RPC 与 Apache Fory 编解码

- **服务提供方**：在接口实现类标注 `@RSocketFacade` 并实现独立 `-facade` 接口。服务启动时依据 `spring.application.name`、接口短名与方法名自动生成点分路由（如 `tazhoo-service-user.userFacade.find`），严禁同服务路由冲突与方法重载。
- **服务调用方**：在 `-core` 引入提供方的 `-facade` 依赖后，注入 `@RSocketRpc("目标服务名")` 字段或 Setter，`RsocketRpcProxyBeanFactoryPostProcessor` 自动生成无反射代理，且仅允许以 Apache Fory MIME Type 发送，服务端 `RpcDataMimeInterceptor` 会拦截非法 JSON/明文直连原生 RPC 路由的请求。
- **契约类型自动注册 (`ForyRpcContracts`)**：启动阶段自动扫描所有 `@RSocketFacade` 与 `@RSocketRpc` 接口入参/出参的 `record`、`PO`、嵌套泛型、`ConverterEnum` 及分页类型注册至 Fory；如有非契约签名的多态子类，通过 `ForyRpcTypes` Bean 显式注册。
- **二进制身份透传 (`IdentityStashEncoder/Decoder`)**：内网 `SecurityStash`（token + principal JSON）采用定长 2 字节长度头 + UTF-8 原生字节写入，无需二次 JSON/Fory 对象包装。

### 4.4 Redis RESP3 服务端追踪缓存与 JWT 会话

`PrincipalFactory` 同时支持标准状态令牌与 **RS256 非对称签发 JWT 会话 (`JwtSessionTokens`)**：

- **JWT 与多端会话并发控制**：登录核验后调用 `PrincipalFactory.setPrincipal(ScopedLoginUser)` 签发 JWT，令牌通过 Redis Lua 脚本按 `<redisPrefix>:{<realm>:<userId>}:<terminal>` 原子更新，支持单端互踢与全端吊销。
- **本地零穿透缓存 (`CLIENT TRACKING ON BCAST`)**：
  - 连接初始化时，开启 Redis RESP3 客户端广播失效追踪（前缀覆写传统 auth-token 与 JWT session 键）；
  - 节点本地使用 Guava Cache（最大 `32768` 条 / `30s` TTL）缓存已验证会话，并用 `invalidationEpoch` 分槽版本号杜绝并发写入脏缓存；
  - 缓存内预存 `tokenBytes` 字节数组，鉴权命中时零编码分配直接 `MessageDigest.isEqual` 比对；
  - 若启动阶段 Redis 尚未就绪，流量触发后按 `5s` 退避冷却自愈建连。
- **`currentUserId()` 热路径优化**：内置 `principalUserIdCache` 缓存，避免每次调用 `currentUserId()` / `currentUserObjectId()` 都重复执行 Jackson 完整反序列化。

---

## 5. 组件使用手册与配置说明

### 5.1 启动引导与 Nacos 配置

每个微服务仅保留唯一的本地配置文件 `<服务名>-core/src/main/resources/application.properties`，引导加载 Nacos 配置与服务发现：

```properties
spring.application.name=tazhoo-service-app
spring.config.import=nacos:${spring.application.name}.properties?group=${NACOS_GROUP}&refreshEnabled=true
spring.cloud.nacos.config.server-addr=${NACOS_SERVER_ADDR}
spring.cloud.nacos.config.namespace=${NACOS_NAMESPACE}
spring.cloud.nacos.discovery.server-addr=${NACOS_SERVER_ADDR}
spring.cloud.nacos.discovery.namespace=${NACOS_NAMESPACE}
spring.cloud.nacos.discovery.group=${NACOS_GROUP}
spring.cloud.nacos.username=${NACOS_USERNAME}
spring.cloud.nacos.password=${NACOS_PASSWORD}
```

> 服务业务配置写在 Nacos Data ID `<spring.application.name>.properties` 中，禁止在本地仓库明文保留生产密码、私钥、商户证书或第三方 API Secret。

### 5.2 RSocket、网关与 WebSocket 双向通信

**1）后端 Controller 暴露网关接口**

网关对外暴露固定动作路径（统一 HTTP POST 或 `wss://`）：

```java
@Controller
@RSocketFacade // 若同一业务同时供其他微服务 RPC 调用，可拆出独立 Facade 实现
public class OrderGatewayController {
    @Resource
    private OrderServiceImpl orderService;

    @MessageMapping("/api/order/summary")
    public Mono<OrderFacade.Summary> summary(@RequestBody OrderFacade.SummaryRequest request) {
        return orderService.summary(request); // request/response
    }

    @MessageMapping("/api/order/list")
    public Flux<OrderFacade.Item> list(@RequestBody OrderFacade.ListRequest request) {
        return orderService.list(request); // request/stream -> 聚合 JSON 数组
    }

    @MessageMapping("/api/order/watch")
    public UiFlux<OrderFacade.Event> watch(@RequestBody OrderFacade.WatchRequest request) {
        return UiFlux.of(orderService.watch(request)); // request/stream -> 单向 NDJSON
    }

    @MessageMapping("/api/order/exchange")
    public BiFlux<OrderFacade.Reply> exchange(Flux<OrderFacade.Command> commands) {
        return BiFlux.of(orderService.exchange(commands)); // request/channel -> WSS 双向文本帧 / NDJSON
    }
}
```

**2）网关 Nacos 路由与调优参数 (`tazhoo-gateway.properties`)**

```properties
spring.main.web-application-type=reactive
server.port=8080

# HTTP RSocket 转发路由（lb/rb 前缀按网关约定）
spring.cloud.gateway.server.webflux.routes[0].id=tazhoo-service-app-http
spring.cloud.gateway.server.webflux.routes[0].uri=rb://tazhoo-service-app
spring.cloud.gateway.server.webflux.routes[0].predicates[0]=Path=/api/app/**

# WebSocket 双向通道路由
spring.cloud.gateway.server.webflux.routes[1].id=tazhoo-service-im-ws
spring.cloud.gateway.server.webflux.routes[1].uri=ws://tazhoo-service-im
spring.cloud.gateway.server.webflux.routes[1].predicates[0]=Path=/ws/api/im/**
spring.cloud.gateway.server.webflux.routes[1].filters[0]=StripPrefix=1

# 网关转发与流控阈值（以下为框架默认值，按需调整）
lambda.gateway.rsocket.max-request-bytes=1048576
lambda.gateway.rsocket.max-response-bytes=4194304
lambda.gateway.rsocket.max-response-items=512
lambda.gateway.rsocket.request-timeout=15s
lambda.gateway.rsocket.strict-json-validation=false

# RSocket 客户端连接池与通道线程（0 = 自动按 CPU 核数）
lambda.rsocket.client.connections-per-instance=2
lambda.rsocket.client.adaptive-load-balancing=true
lambda.rsocket.client.max-inbound-payload-bytes=16777216
lambda.rsocket.client.io-workers.unary=0
lambda.rsocket.client.io-workers.stream=2
lambda.rsocket.client.io-workers.channel=2
```

**3）业务服务端 RSocket TCP 配置**

```properties
spring.rsocket.server.transport=tcp
spring.rsocket.server.port=7001
lambda.rsocket.server.max-inbound-payload-bytes=16777216
```

> 业务微服务的 RSocket 必须使用 `tcp`，框架启动拦截器会主动校验并拒绝暴露 WebSocket 等非预期服务端传输层。

### 5.3 安全认证与请求上下文 (`PrincipalFactory`)

- **网关公开白名单 (`SecurityConfig`)**：无需登录的路径配置于 `lambda.security.permit-paths`，静态精确路径自动走 O(1) `HashSet` 查找，通配路径使用 `PathPattern` 匹配：

```properties
lambda.security.permit-paths[0]=/api/user/auth/login
lambda.security.permit-paths[1]=/api/app/public/**
```

- **JWT 会话配置 (`lambda.security.jwt.*`)**：网关与签发服务开启 JWT 验签/签发；业务服务若仅需从 Reactor Context 读取身份，无需配置签名私钥：

```properties
lambda.security.jwt.enabled=true
lambda.security.jwt.issuer=tazhoo
lambda.security.jwt.app-audience=tazhoo-app
lambda.security.jwt.admin-audience=tazhoo-admin
lambda.security.jwt.redis-prefix=tazhoo:auth:session
lambda.security.jwt.lifetime=15m
lambda.security.jwt.app-lifetime=7d
lambda.security.jwt.signing-key-id=k1
lambda.security.jwt.private-key=file:/app/keys/jwt-private.pem
lambda.security.jwt.public-keys.k1=file:/app/keys/jwt-public.pem
```

- **在 Service 中读取登录上下文**：

```java
@Resource
private PrincipalFactory principalFactory;

public Mono<UserFacade.Detail> me() {
    return principalFactory.currentUserObjectId()
            .flatMap(userId -> userRepository.findById(userId))
            .switchIfEmpty(Mono.error(new BusinessException(UserExceptionEnum.USER_NOT_FOUND)));
}
```

### 5.4 MongoDB 响应式仓储、审计、聚合与分页

**配置项：**

```properties
lambda.repository.mongo.uri=mongodb://user:pass@127.0.0.1:27017/tazhoo?authSource=admin&replicaSet=rs0&retryWrites=true&w=majority
lambda.repository.mongo.mode=single
lambda.repository.mongo.create-indexes=true
lambda.repository.mongo.write-null-fields=true
```

**实体基类与索引声明 (`UnifyPO<ObjectId>`)：**

```java
@Document(collection = "pets")
@CompoundIndex(name = "idx_pet_owner_created", def = "{'ownerId': 1, 'createdAt': -1, '_id': -1}")
public class PetPO extends UnifyPO<ObjectId> {
    private ObjectId ownerId;
    private String name;
    private PetStatusEnum status; // 实现 ConverterEnum
}
```

**审计原子更新 (`MongoAuditUpdates`) 与分页面包 (`ReactiveUnifyPagingRepositoryOperation`)：**

- 使用 `MongoAuditUpdates.modified(update)` 自动维护 `$set updatedAt` 与 `$inc version`；
- 幂等写入使用 `MongoAuditUpdates.insertOnly(update, createdAt, updatedAt)`；
- 列表分页让 Repository 继承 `ReactiveMongoCrudRepositoryOperation<PetPO, ObjectId>`，在 Service 层封装页码（1-based）与页容校验后调用 `queryPage(...)` 返回 `Mono<Paged<T>>`。

### 5.5 Redis 缓存与分布式事务操作

```properties
lambda.repository.redis.mode=single
lambda.repository.redis.nodes[0]=127.0.0.1:6379
lambda.repository.redis.username=root
lambda.repository.redis.password=***
lambda.repository.redis.database=0
lambda.repository.redis.timeout=3s
lambda.repository.redis.tls=false
```

使用 `ReactiveRedisOperation`（已预配 String Key + Jackson JSON Value 序列化与启动 `PING` 健康检查）：

```java
@Resource
private ReactiveRedisOperation redis;

// 写入并带正向 TTL（秒），读取强类型反序列化
return redis.set("tazhoo:sms:code:" + phone, code, 300L)
        .then(redis.get("tazhoo:sms:code:" + phone, String.class));
```

> 集群模式配置 `lambda.repository.redis.mode=cluster`，并提供 2 个以上种子节点、`database=0`。

### 5.6 Qdrant 向量检索

```properties
lambda.repository.qdrant.mode=single
lambda.repository.qdrant.endpoints[0]=http://127.0.0.1:6334
lambda.repository.qdrant.api-key=***
lambda.repository.qdrant.timeout=5s
```

业务向量仓储继承 `ReactiveQdrantRepository<T>` 并通过注入 `ReactiveQdrantOperations` 调用非阻塞 gRPC 查询、写入与跨节点集合就绪检查（`awaitCollection`）。

### 5.7 分布式锁（Redisson / ZooKeeper）

- **Redisson 响应式锁**（复用 `lambda.repository.redis.*` 连接）：

```java
var lock = new ReactiveRedissonLock(redissonReactiveClient, "tazhoo:lock:order:" + orderId);
return lock.withLock(Duration.ofSeconds(2), Duration.ofSeconds(10), () -> processOrder(orderId));
```

- **ZooKeeper 分布式锁**：配置 `lambda.lock.zookeeper.connect-string=127.0.0.1:2181`、`lambda.lock.zookeeper.mode=single`、`lambda.lock.zookeeper.namespace=locks` 即可自动装配 `CuratorFramework`。

### 5.8 消息队列（RabbitMQ 延迟消息 / Kafka）

- **RabbitMQ**（`lambda.mq.rabbitmq.*`：`mode=single|cluster`、`addresses`、`username`、`password`、`virtual-host=/`、`tls=false`）：注入 `RabbitMQReactiveOperate`，支持 `declareDelay`、`sendDelay`（基于 `x-delayed-message` 插件与发布确认）及 `receiverManual`/`receiverAuto`。
- **Kafka**（`lambda.mq.kafka.mode=single|cluster`、`bootstrap-servers`）：注入 `KafkaMQReactiveOperate`，统一经发布方 `-facade` 定义 `KafkaMQDeclare` 常量收发消息。

### 5.9 第三方 HTTP 客户端 (`ReactiveHttpClient`)

构造不可变客户端复用框架全局连接池（最大 256 连接，20s 空闲回收），内置响应大小上限（默认 `256KB`）、非 2xx 错误自动映射为 `EventException(UPSTREAM_RESPONSE_FAILED)` 与 `TimeoutException` 统一收敛；对接腾讯云 API 可直接使用同包下的 `Tc3Authorization` 生成签名头。

### 5.10 无锁雪花 ID (`GuidFactory`) 与启动健康检查

- **雪花 ID**：配置 `lambda.guid.mode=single|cluster` 与 `lambda.guid.worker`（或环境变量 `LAMBDA_INSTANCE_INDEX`，范围 `0..1023`），直接注入 `GuidFactory`，调用 `nextId()` / `nextLong()`。
- **启动自检 (`ComponentCheck`)**：默认开启 `lambda.startup.check=true`（超时 `lambda.startup.timeout=8s`），应用启动时会验证已装配的 MongoDB、Redis、RabbitMQ、Kafka、Qdrant 等组件就绪状态，失败则快速暴露配置故障。

---

## 6. 内置开发规范（强制）

任何基于 Lambda Framework 开发的微服务与网关应用，均须遵守以下工程与编码规范：

### 6.1 仓库、包名与模块化规范

1. **一应用一仓库**：仓库名与根工程目录统一为 `<项目名>-<微服务名>`（全小写连字符，如 `tazhoo-service-user`），与 `spring.application.name`、Nacos Data ID 基名一致。
2. **标准四模块目录结构**（即使暂无跨服务 RPC，网关也保留同级 `-facade`）：
   - `<app>-core`：启动类（位于 `com.<项目>.<服务>.core`）、`controller`、`service`、`repository`、`po`、内部 `contract` 及唯一的 `src/main/resources/application.properties`；打包为 Spring Boot 可执行 JAR，并在 `install` 阶段复制到 `../<app>-docker/app.jar`。
   - `<app>-facade`：对外 `@RSocketFacade` 契约接口、请求/响应 `record`、MQ 主题常量；打包为普通 JAR。
   - `<app>-enum`：仅存放本服务错误码枚举 `*ExceptionEnum`（包名用 `enums`）。
   - `<app>-docker`：仅存放 `Dockerfile`、`compose.yaml`、`.dockerignore` 与构建产出的 `app.jar`（`.gitignore` 排除构建产物）。

### 6.2 分层职责与依赖注入

| 层级 | 强制职责 | 严禁行为 |
|---|---|---|
| **Controller / Facade 实现** | 声明 `@MessageMapping` 固定斜杠路径或实现 `*Facade` 接口，绑定参数后直接转发 Service | **禁止**编写校验、默认值、权限判断或业务分支 |
| **Service (`*ServiceImpl`)** | `@Validated` 参数校验、默认值填充、权限归属验证、事务编排、业务规则与契约映射 | **禁止**编写构造函数注入（统一字段 `@Resource`，可选依赖用 `@Autowired`） |
| **Repository** | 数据读写、索引查询、聚合与原子条件更新 | **禁止**参数校验、业务判断或抛出 HTTP 语义 |

> 业务项目中 **禁止** 自建通用工具类（如 `*Util`）；可复用工具必须先沉淀至 `lambda-framework-common` 并通过单元测试。

### 6.3 接口契约、record 命名与数值精度

- **独立契约 `record`**：HTTP/RPC 入参与出参声明为契约 `record`（放于所属 Facade 接口内部或本地 `contract` 包），按动作/用途命名（如 `FindRequest`、`CreateRequest`、`Detail`、`PageItem`），**不加 `DTO`/`VO`/`PO` 后缀**。
- **HTTP 统一 POST 固定路径**：所有面向前端的业务 HTTP 统一通过网关 `rb://` + `POST` 访问，查询条件放在 JSON Body（空参使用 `EmptyRequest`），禁止把业务筛选条件暴露为任意变体 URL 路径或查询参数。
- **金额禁止 `BigDecimal` 与浮点**：API 契约与数据库持久化中的金额及高精度标量统一使用规范字符串（明确币种与最小单位），业务层以带溢出保护的整型运算处理；库存计数、版本号、页码及日期保留技术原类型。
- **枚举中文键**：持久化业务枚举必须实现 `ConverterEnum`，`getValue()`（前后端及 MongoDB 存储值）与 `getDescription()` 均使用简明中文，禁止存储无意义数字魔法值。

### 6.4 MongoDB 建模、ObjectId、索引与原子并发

1. **统一超类**：所有集合实体必须继承 `UnifyPO<ObjectId>`（提供 `_id`、`createdAt`、`updatedAt`、`@Version Long version`），时间统一用 `Instant`。
2. **原生 `ObjectId` 全链路透传**：接口请求/响应 `record` 直接使用 `org.bson.types.ObjectId`（框架 Jackson 自动完成 24 位 hex 字符串互转），Fory RPC 直接传输 `ObjectId`；Repository 的 `@Query` 与 `@Aggregation` 参数 `?0` 严禁加引号（写 `{ _id: ?0 }` 而非 `'?0'`），跨集合关联字段 BSON 类型严格保持一致。
3. **关系建模与索引**：多对多必须建立独立关系集合并配唯一复合索引；高频增长子项（聊天记录、流水）独立建表并以 `(时间, _id)` 游标翻页；所有高频筛选+排序组合必须在实体声明 `@CompoundIndex` 并核对执行计划。
4. **并发与防超卖**：
   - 编辑操作必须携带并校验客户端 `version`；
   - 库存扣减必须在同一条 MongoDB `updateFirst`/`findAndModify` 中同时匹配 `_id + 可售状态 + available >= quantity` 并联动 `MongoAuditUpdates.modified(...)`；
   - 余额条件更新匹配 `_id + currency + oldAmount + version` 并处于同一 `TransactionalOperator` 事务内。

### 6.5 空值处理与统一异常码契约

| 场景 | 强制写法 |
|---|---|
| 同步可空值、Map/枚举查找 | 返回 `Optional<T>`，取值用 `OptionalUtil.require` 或 `orElseThrow`；**严禁 `Optional.get()` 或吞异常兜底** |
| 单条必需响应式结果 | `ReactiveValues.require(mono, () -> new BusinessException(...))` |
| 缺失具有业务含义（查重/幂等） | `ReactiveValues.optional(mono)` 由 Service 显式分支 |
| 集合/分页/流无数据 | 返回 `List.of()`、`Flux.empty()` 或空 `data: []`，`Mono<Void>` 成功直接完成 |

**8 位 `serviceCode` 契约**：
- `00000000`：成功；
- `S` + 7 位数字：框架及系统级错误（`EventException`）；
- `B` + 7 位数字：业务错误（`BusinessException`，定义在各服务 `-enum` 模块）；
- 面向用户的 `serviceMessage` 须清晰可执行且不超过 **24 个 Unicode 码点**，异常堆栈与底层协议细节仅记录在服务端日志。

### 6.6 第三方接口与 Docker 交付禁令

1. **第三方 HTTP 强约束**：所有服务调用外部 HTTP/HTTPS 必须通过 `lambda-framework-httpclient`（基于 `ReactiveHttpClient`）封装成非阻塞 `Mono`/`Flux`；严禁在业务代码中使用 `block()`、自行 `subscribe()` 或另起 `RestTemplate`/`OkHttp` 客户端。
2. **严禁本机运行业务容器与服务**：允许在开发机上执行 `mvn clean install` 编译、单测与产物检查，**严禁在开发者本机启动任何 Java 业务微服务、网关进程或业务 Docker 容器**；联调与验收必须打包唯一可执行 `app.jar` 上传目标服务器的独立部署目录 `/root/deploy/<应用名>/` 通过 Docker 运行。
3. **仓库清洁**：私有 IP、账号密码、SSH 密钥、支付证书与测试临时配置只存于本机私有文档或服务器环境变量与 Nacos，严禁提交进任何 Git 仓库。
