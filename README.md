# Lambda Framework

Lambda Framework 提供 JDK 27、Spring Boot 4.1.2 和 Spring Cloud 2025.1.4 下的响应式 Java 后端基础能力。使用 Mono 和 Flux 编写业务，在同一体系内接入 MongoDB、Redis、RabbitMQ、Nacos、RSocket 和 HTTP 网关。

## 构建与启动

框架是父 POM 与 14 个模块的源码工程。应用按功能依赖模块，并单独启动 Spring Boot 进程。先把框架安装到本地 Maven 缓存，再构建 [lambda-framework-example](https://github.com/MrWangGang/lambda-framework-example)。

~~~bash
export JAVA_HOME='/实际的/JDK27/Contents/Home'
./scripts/mvn.sh -version
./scripts/mvn.sh clean install
cd ../lambda-framework-example
cp .env.example .env
./scripts/run.sh
~~~

在执行 run.sh 前按本地《第三方配置.md》填入 .env 并准备 Nacos 启动配置。mvn.sh 使用已有 IDEA Maven 或显式 MAVEN_BIN。框架远程 deploy 默认跳过。

~~~xml
<parent>
  <groupId>org.lambda.framework</groupId>
  <artifactId>lambda-framework</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <relativePath/>
</parent>
<dependencies>
  <dependency>
    <groupId>org.lambda.framework</groupId>
    <artifactId>lambda-framework-repository</artifactId>
  </dependency>
</dependencies>
~~~

## 模块与接入入口

| 模块后缀 | 主要能力 |
|---|---|
| common | UnifyPO、SecurityLoginUser、ConverterEnum、通用异常、JsonUtil 和 RPC 注解 |
| repository | MongoDB、Redis、R2DBC MySQL、Elasticsearch、分页 |
| compliance | 通用 Service、树操作、PrincipalFactory、SecurityPrincipalHolder |
| security | 认证 Redis、令牌、URL 授权和自定义校验 |
| web | WebFlux 上下文、响应与异常处理 |
| gateway | HTTP POST 到 RSocket、JSON 与 NDJSON |
| rsocket | 编解码策略、SocketAcceptor 拦截、Payload 生命周期、异常编码 |
| rpc | Java 接口 RPC 代理、服务端路由注册 |
| loadbalance | RSocketRequester 缓存、连接释放、目标发现 |
| nacos | 配置导入、服务发现与 RSocket 端口注册 |
| mq | RabbitMQ、Kafka 与响应式连接 |
| httpclient | WebClient 响应过滤辅助 |
| guid | 雪花 ID |
| lock | Redisson 和 ZooKeeper 锁 |

artifactId 由 `lambda-framework-` 加模块后缀组成。按应用需要引入配置与组件，例如数据库配置继承 DefaultReactiveMongoRepositoryConfig，Redis 配置继承 DefaultReactiveRedisRepositoryConfig。完整参数见 [模块与配置](docs/MODULES-AND-CONFIGURATION.md)。

## RSocket 服务与 HTTP 网关

独立服务设置 `spring.main.web-application-type=none` 和 `spring.rsocket.server.port`。引入 rsocket 模块并导入 RSocketStrategiesConfig、RSocketAcceptorInterceptor、RsocketGlobalExceptionHandler 与 RsocketInterceptorConfig。服务端用 `@MessageMapping` 声明处理器，客户端和服务端共用 Facade 接口内的请求、响应 record。Java 调用方通过 `@RSocketRpc` 注入 Facade。方法上的 `@MessageMapping` 提供路由，点分隔路径的首段对应服务发现名。

~~~java
public interface ChatMessageFacade {
    @MessageMapping({"chat-service.chatMessageFacade.find", "/rpc/message/find"})
    Mono<Detail> find(FindRequest input);

    record FindRequest(@JsonObjectId ObjectId id) {}
    record Detail(@JsonObjectId ObjectId id, String content) {}
}
~~~

服务端用 `@Controller` 实现 Facade，沿用接口方法上的映射。一个 `@MessageMapping` 可以同时声明 Java RPC 和网关的路由。以下网关使用 `rb://chat-service`，将重写后的请求路径交给 RSocket。网关应用配置 reactive，导入 GatewayFilter、RsocketRequestFactory、GatewayGlobalExceptionHandler、RSocketLoadbalance 与 RSocketStrategies 配置，使用对应的服务发现和 PrincipalStash。

网关项目名为 `chat-gateway` 时，把下面内容发布为 Nacos 的 `chat-gateway.properties`。本地只保留 Nacos 导入所需的引导配置。

~~~properties
spring.main.web-application-type=reactive
server.port=8080
spring.cloud.gateway.server.webflux.routes[0].id=chat
spring.cloud.gateway.server.webflux.routes[0].uri=rb://chat-service
spring.cloud.gateway.server.webflux.routes[0].predicates[0]=Path=/rpc/message/**
~~~

### 内部 RPC 使用 Apache Fory

Java 服务间 RPC 固定使用 Fory。两端导入 `RSocketStrategiesConfig`，框架自动收集契约里的 record、枚举与框架分页类型。HTTP 客户端访问 reactive 网关，网关通过 `rb://` 以 JSON 转发到只监听 RSocket TCP 的业务服务。配置示例和发布步骤见 [RSocket 手册](docs/RSOCKET-AND-VIRTUAL-THREADS.md#2-注解与接口约定)。

### 自动选择请求与响应类型

以下识别规则用于 `rb://` 路由。业务 HTTP 客户端访问网关地址，业务服务使用 `@MessageMapping` 提供内网映射。

| 已发现的服务端签名 | 网关选择 |
|---|---|
| 单条请求，返回 `Mono<T>` | request/response，返回 JSON |
| 单条请求，返回 `Flux<T>` | request/stream，逐条返回 NDJSON |
| `TFlux<I>` 入参，返回 `TFlux<O>` | request/channel，发送 JSON 数组元素或 NDJSON 行，逐条返回 NDJSON |

网关缓存服务端契约。已发现的 JSON 路由以签名决定模式与响应格式，客户端发送请求体即可。普通请求缺少 Content-Type 时按 JSON。单次响应的 `data` 保留实际类型，record 是对象，`Mono<String>` 是字符串，`Mono<Integer>` 是数字，`Mono<Void>` 是空数据。

| 路由键 | 用途 |
|---|---|
| rsocketConnectionsPerInstance | 1–8，默认 1。每个后端实例复用的连接数 |
| rsocketStreamConnectionsPerInstance | 1–8，单向流专用连接数，配置时优先于通用值 |
| rsocketContentType | 仅在需要固定输入格式时声明，例如二进制路由使用 application/octet-stream |

业务网关仅转发 `rb://` 路由中由服务端发布的固定 `@MessageMapping` 路径。未公开路径返回 404，契约不可用时返回 503。契约成功结果按服务缓存 30 秒。业务请求错误沿用 serviceCode/serviceMessage，网关参数、方法、内容类型与超限错误使用相应 HTTP 状态。

~~~bash
curl -fsS --json '{"id":"507f1f77bcf86cd799439011"}' \
  http://127.0.0.1:8080/rpc/message/find
~~~

~~~bash
curl -NsS http://127.0.0.1:8080/rpc/message/channel \
  -H 'Content-Type: application/x-ndjson' \
  --data-binary @messages.ndjson
~~~

示例地址假设对应服务已经启动。messages.ndjson 每行对应Facade 中的 SendRequest。客户端可在收到首条响应后发送下一条请求。部署时复核代理、客户端和 HTTP 版本的双向传输行为。已开始输出后的错误会结束流，客户端按其业务协议重试或续读。

| 配置 | 默认值与范围 |
|---|---|
| lambda.gateway.rsocket.max-request-bytes | 1048576 字节，普通请求体或每条 NDJSON |
| lambda.gateway.rsocket.max-response-bytes | 4194304 字节，聚合响应或单条流响应 |
| lambda.gateway.rsocket.max-response-items | 512，旧版 JSON 数组的元素数 |
| lambda.gateway.rsocket.request-timeout | 15s，普通请求总等待或流首条响应等待 |
| lambda.rsocket.server.max-inbound-payload-bytes | 16777216，服务端 Payload 重组上限 |
| lambda.rsocket.client.max-inbound-payload-bytes | 16777216，框架客户端 Payload 重组上限 |

请求体读取一次。传统数组先累计条目和字节数，再按最终长度分配响应缓冲区。NDJSON 边接收边写出。连接按目标、数据 MIME 和连接数缓存，后台刷新服务实例，关闭应用时释放。身份元数据条目独立使用 JSON。

关键路由可在 Nacos 的路由 metadata 设置 `rsocketConnectionsPerInstance=2`。连接数用于同一个后端实例的池内复用。RSocket 池摘除断链后，框架用最近发现的目标列表补建，独立于 30 秒的服务发现刷新。网关将识别到的上游断链或连接不可用映射为 HTTP 503，已发送请求不自动重放。相同目标、数据 MIME 和连接数的路由共享连接池。

## 用户、分页、配置和维护

- [用户体系](docs/USER-AND-SECURITY.md) 涵盖用户超类、会话、授权 Redis、ACL 缺口与扩展点。
- [数据与分页](docs/DATA-AND-PAGINATION.md) 涵盖 Repository 接口、排序、分页和复合游标。
- [RSocket 与线程](docs/RSOCKET-AND-VIRTUAL-THREADS.md) 涵盖背压、握手、负载均衡和虚拟线程。
- [安装](docs/GETTING-STARTED.md)、[配置](docs/MODULES-AND-CONFIGURATION.md)、[维护](docs/OPERATIONS.md) 和 [依赖](docs/DEPENDENCIES.md) 分别提供完整操作。
- 框架构建、Channel、TCP 取消与引用释放、分页，以及 chat-api、chat-service、网关联调已验收。全量生产聊天、生产代理与集群故障恢复需按部署条件另验收。

## 2026-11-09 验收与性能对照

Channel 直接检查 13 项、TCP 检查 8 项、分页回归 12 项、双服务/网关/示例实机 26 项均通过。

同配置对照中，128 条 JSON 数组流从 3,177 提升到 3,930 请求/秒，网关 CPU 从 0.3950 降到 0.2411 ms/请求。逐条 NDJSON 将慢流首字节从 1052.9 ms 降到 242.4 ms。单次请求吞吐略低，峰值 RSS 略高。压测为本机短轮次，报告保留所有数值和样本。

### 自动类型与真实业务复测

16 项自动类型、旧头兼容、双向流、限额与超时检查通过。使用同一个聊天服务和真实 MongoDB，在 8 并发下直连约 194 请求/秒，HTTP 网关约 175 请求/秒。在 32 并发下两条链路都约 166 请求/秒，尾延迟明显增加。网关进程 CPU 中位约一个核的 12%–15%。这组结果适用于本机服务与跨网络 MongoDB 的测试环境，逐轮数据见本地汇总报告。

### 流式吞吐与虚拟线程复测

24 并发的本机 HTTP 到 TCP RSocket 测试中，128 项数组流原版约 3,857 请求/秒，改用 4 条流式连接后约 5,101 请求/秒，P99 从 7.9 ms 升到 14.3 ms。保持 1 条连接时，新数组实现每请求 CPU 下降约 12%，吞吐接近。每次请求订阅切换到虚拟线程使普通请求吞吐下降约 10.5%，网关保留响应式执行路径。[Reactor 线程与调度器](https://projectreactor.io/docs/core/release/reference/coreFeatures/schedulers.html)

真实 MongoDB 混合读负载下，新网关对流式路由配置 2 条连接。8 并发时旧版和新版约为 195 和 190 请求/秒，32 并发时约为 163 和 178，两版均出现超时。普通查询适合 JSON POST。需要逐条下发时用 NDJSON 单向流，连续聊天收发用双向 Channel。HTTP NDJSON 双向联调已验证首条回复返回后客户端可继续发送，吞吐对照表测量的是普通查询和单向流。

## Java 后端开发与架构规范

版本日期为 **2026-11-10**。本规范供开发者和智能体统一 Java 项目的结构、接口、数据模型和验收方式。部署地址、账号、云资源与 GitHub SSH 认证配置集中到本地《第三方配置.md》。仓库中的示例使用本机地址和环境变量。

### GitHub 仓库

- 框架源码与使用手册。[lambda-framework](https://github.com/MrWangGang/lambda-framework)
- 可运行的示例与接入说明。[lambda-framework-example](https://github.com/MrWangGang/lambda-framework-example)

### 项目名称、包名和 Maven 模块

新建业务微服务前确认项目名称，缺少名称时询问。父工程 artifactId、Spring 应用名、服务发现名和 Nacos Data ID 的文件名使用项目名称。Java 包根使用 `com.<项目名>`，在其下按职责分包。包名用小写 Java 标识符，项目名中的连字符对应包层级，例如 `order-center` 对应 `com.order.center`。

业务微服务使用一个 Maven 父工程和两个子模块。以下用 `user` 举例。

~~~text
user/
├── pom.xml
├── user-service/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/user/
│       │   ├── UserApplication.java
│       │   ├── controller/UserController.java
│       │   ├── service/UserServiceImpl.java
│       │   ├── facade/UserFacadeImpl.java
│       │   ├── entity/UserEntity.java
│       │   └── repository/UserRepository.java
│       └── resources/application.properties
└── user-facade/
    ├── pom.xml
    └── src/main/java/com/user/
        └── facade/UserFacade.java
~~~

- 父工程使用 `pom` 打包，管理模块与版本。`<项目名>-service` 是可运行模块，依赖 `<项目名>-facade`。其他微服务通过 `<项目名>-facade` 引入 RPC 契约。
- `<项目名>-facade` 是契约 JAR，`facade` 包内放对外接口和接口内的入参、出参 `record`。该模块只引入编译和使用契约所需的依赖。
- 服务端的 `UserFacadeImpl` 放在 service 模块的 `facade` 包。业务实现直接命名为 `UserServiceImpl`，放在 `service` 包根，省略 `UserService` 接口和 `service.impl` 子包。
- 每个数据库表或集合对应一个持久化实体。`UserEntity` 映射到 `user`，由 `UserRepository` 访问。枚举按业务归属放在实体或 `enums` 包，连接配置放在 `config` 包。

### Facade 契约与 record 命名

- 公开请求和响应统一声明为对外 Facade 接口内的 `record`。入参按动作命名，例如 `FindRequest`、`ChangePasswordRequest`、`PageRequest`。返回按用途命名，例如 `Detail`、`Summary`、`PageItem`。外层 Facade 已表达业务归属，内层名称不重复加表名或实体名，省略 DTO、VO、PO 后缀。
- Controller 与 Facade 实现直接注入 `UserServiceImpl`，复用 Facade 的 record。外部类只导入 `UserFacade`，签名写 `UserFacade.FindRequest` 和 `UserFacade.Detail`。公开契约集中在接口，实体和实现类不声明公开入参、出参。
- 实体留在 service 模块。Controller、Facade 接口和 Service 的公开业务方法使用契约 record，Service 负责从持久化实体映射公开字段。密码哈希和内部权限留在服务端。公开契约按业务需要显式选择可见的审计字段或并发控制字段。
- 对外业务 HTTP 统一经网关访问，使用 POST 动作路径，例如 `/api/user/find`。业务服务的路由统一使用 `@MessageMapping`。承接网关转发的路径以 `/` 开头，并使用固定路径。动态业务条件放请求体。请求条件放 JSON 请求体，空请求可用 `EmptyRequest`。RPC 保留 `Mono<UserFacade.Detail>`、`Flux<UserFacade.Detail>` 等完整类型。底层对字符串与数字的编码兼容性独立测试。
- 业务金额和常规业务标量以字符串建模，禁止 BigDecimal。ObjectId、库存计数、版本号、分页计数和日期保留各自的技术类型。CORS 预检、WebSocket 握手和基础设施协议遵循对应协议。

~~~java
package com.user.facade;

import jakarta.validation.constraints.NotNull;
import org.bson.types.ObjectId;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.lambda.framework.common.jackson.JsonObjectId;
import reactor.core.publisher.Mono;

public interface UserFacade {
    @MessageMapping({"user.userFacade.find", "/api/user/find"})
    Mono<Detail> find(FindRequest request);

    record FindRequest(@JsonObjectId @NotNull ObjectId id) {}
    record Detail(@JsonObjectId ObjectId id, String name) {}
}
~~~

`UserFacadeImpl` 作为 `@Controller` 实现 `find`，Spring 继承 Facade 的 `@MessageMapping`。同一方法处理 Java RPC 的 `user.userFacade.find` 和网关的 `/api/user/find`，内部调用 `UserServiceImpl.find(UserFacade.FindRequest)`。Facade 模块使用 `spring-messaging` 提供的注解。

### 实体注解与主键

每个持久化实体都要明确表或集合名，以及唯一的主键映射。MongoDB 使用 Spring Data 的 `@Document(collection = "...")` 和 `org.springframework.data.annotation.Id`。实体独立声明主键时写成 `@Id ObjectId id`，继承 `UnifyPO<ObjectId>` 时复用基类已有的 `@Id`。同一实体层级只保留一个主键属性。Repository 的 ID 泛型、关联字段和查询绑定类型需与主键一致。

| 注解 | 放置位置 | 使用约定 |
|---|---|---|
| `@Document` | MongoDB 实体 | 显式写集合名，让部署、查询和迁移引用同一名称 |
| `@Table` | Spring Data R2DBC 实体 | 显式写表名，表结构和索引用迁移脚本维护 |
| `@Id` | 主键字段或 record 组件 | 明确主键映射。MongoDB 对应 BSON `_id` |
| `@Field` / `@Column` | 字段或 record 组件 | 数据库名与 Java 属性不同时声明映射。变更已有映射时提供数据迁移 |
| `@Version` | 乐观锁字段 | 按业务选用。依赖旧版本的手写更新必须显式匹配版本，并在同一操作中推进 |
| `@CreatedDate` / `@LastModifiedDate` | 审计时间字段 | 启用对应的审计配置后使用，核对插入与更新路径是否填充 |
| `@Indexed` / `@CompoundIndex` | 单字段 / MongoDB 实体 | 有对应查询、排序、唯一或生命周期需求时添加，索引名和用途可追溯 |

HTTP 和 RPC 入参的校验注解放在请求 record 组件上。公开 record 只声明契约需要的字段，持久化映射注解集中在实体或基类。

### 关系建模与读写取舍

设计前记录关键接口的读取频率、更新频率、常见筛选和排序、子项数量上限、独立更新或分页需求，以及一致性要求。评估一次读取需要访问多少文档，以及一次更新是否会触发多份副本更新。表结构按这些访问模式设计，索引跟随实际查询。

| 关系 | 结构要求 | 读多时 | 更新多或持续增长时 |
|---|---|---|---|
| 多对多 | 必须建中间表，MongoDB 建独立关系集合。两端 ID 均用 ObjectId，关系实体也有自己的 `@Id` | 收藏列表从 `favorites` 按 `userId` 分页，再按当前页关联商品。商品反查用户时，按反查条件增加索引 | 新增或取消一条收藏只写一条关系记录。用 `(userId, productId)` 唯一约束，租户场景把租户键纳入唯一范围 |
| 一对多 | 直接内嵌或在子表保存父 ID，无需中间表 | 用户与少量收货地址可内嵌。前提是经常一起读取、地址有业务上限且随用户管理 | 会话与聊天消息拆集合，消息保存 `conversationId`，按时间和 `_id` 游标翻页。高频写消息不改同一份会话文档 |
| 多对一 | 多端持有一端 ID，无需中间表 | 商品列表常读店铺名称时，可保留少量展示快照，写清可接受的延迟和刷新方式。需要实时店铺信息时批量查询或用 `$lookup` | 商品持有 `merchantId`，商家资料保存在商家集合。商家频繁变更时集中更新，避免改名触发所有商品重写 |

内嵌是存储决策，`$lookup` 是查询决策。内嵌适用于有界、一起读写且生命周期一致的数据。子项独立分页、授权、更新或增长无上限时用引用。使用引用时，先判断页面是否需要关联数据，再选择批量查询或聚合。尽早按索引过滤。如果关联结果参与过滤、排序或计数，应在对应步骤完成后再分页，保持结果语义。避免逐条查询形成 N+1。上线前确认常用访问路径、索引、关联基数及删除后的孤儿处理。历史订单里的成交快照保留成交时值。[MongoDB 内嵌模型](https://www.mongodb.com/docs/manual/data-modeling/embedding/) · [MongoDB 引用模型](https://www.mongodb.com/docs/manual/data-modeling/referencing/)

### 按业务设计索引

为每个新增或变更的索引记录关联的 Repository 方法、筛选条件、排序、预计数据规模和上线方式。单字段查询或唯一键使用单字段索引。等值筛选加排序优先按实际查询组合设计复合索引，再用执行计划验证。合并重复索引时检查已有查询是否依赖它。索引有写入与存储成本，按需求取舍。

~~~java
// ChatMessageRepository.findHistory 按 conversationId 查询，按 createdAt、_id 倒序取一页。
@Document(collection = "chat_messages")
@CompoundIndex(name = "idx_chat_conversation_recent",
        def = "{'conversationId': 1, 'createdAt': -1, '_id': -1}")
public record ChatMessageEntity(@Id ObjectId id, ObjectId conversationId,
                                Instant createdAt, String content) {}
~~~

集合的 `_id` 已有主键索引，业务幂等键和关系唯一性单独说明，例如收藏的 `(userId, productId)`。上线前用 `getIndexes()` 核对索引名、字段顺序和选项，用 `explain("executionStats")` 核对常用查询的命中、排序和扫描量。生产集合的索引变更安排在部署迁移中。框架 Mongo 配置在 `ApplicationReadyEvent` 异步提交已发现实体的索引创建，依赖唯一约束的业务要确认创建完成再开放流量。[Spring Data 索引管理](https://docs.spring.io/spring-data/mongodb/reference/mongodb/mapping/mapping-index-management.html) · [MongoDB 复合索引顺序](https://www.mongodb.com/docs/manual/tutorial/equality-sort-range-guideline/)

### MongoDB 类型、枚举与查询

MongoDB 主键和关联字段用 BSON `ObjectId`，Repository 泛型和查询参数也使用 `ObjectId`。网关 HTTP JSON 中的 ID 使用 24 位十六进制字符串。原生 Java RPC 用 Fory 直接传输 ObjectId。公开 record 的 Mongo ID 组件声明为 `@JsonObjectId ObjectId`，Jackson 2 和 3 在读入时校验并转换，写出时输出小写十六进制字符串。Service 直接把原生 ID 交给 Repository。

~~~java
import org.lambda.framework.common.jackson.JsonObjectId;

// 定义在所属 Facade 接口内
record FindRequest(@JsonObjectId @NotNull ObjectId id) {}
~~~

`@JsonObjectId` 只作用于标注的单值字段。必填 ID 配合 `@NotNull` 和入口 Bean Validation，游标等可选 ID 允许为空。字符串业务键继续用 `String`。既有字符串入参若暂不迁移，在调用 Repository 前校验并转换一次。

~~~java
public interface ChatMessageRepository extends ReactiveMongoRepository<ChatMessageEntity, ObjectId> {
    @Query("{ '_id': ?0 }")
    Mono<ChatMessageEntity> findWithQuery(ObjectId id);
}
~~~

联调实测中，`findById(ObjectId)`、`@Query` 绑定 ObjectId、带实体映射的 `ReactiveMongoTemplate.findById(hex, Entity.class)` 均命中。原生 `Filters.eq("_id", hex)` 查不到，改成绑定 ObjectId 即命中。MongoDB 的 `@Query` 使用 BSON 查询表达式。原生驱动、Document 和聚合匹配应明确 BSON 类型，关联字段也需一致。历史字符串 ID、DECIMAL128 金额和旧索引应经过迁移后再由新实体读取。

数据库实体包含枚举字段时，枚举实现 `org.lambda.framework.common.enums.ConverterEnum`，提供 `getValue()` 与 `getDescription()`。持久化值保持稳定，例如 `text`、`emoji`、`image`、`video`。框架的 Mongo 配置注册枚举读写转换器。本次联调已核对 MongoDB 存储字符串值以及 API 往返。

### `@Aggregation` 中的 ObjectId

供网关 JSON 使用的请求 record 用 `@JsonObjectId ObjectId` 自动接收十六进制字符串。Java RPC 的 Fory 编解码直接处理 ObjectId。业务层把原生 ID 直接交给 Repository。Repository 的 `@Aggregation` 用未加引号的 `?0`、`?1` 绑定原生参数，批量条件用 `List<ObjectId>`。已是 ObjectId 的值直接传入。各集合关联字段的 BSON 类型必须一致。顶层字段会按 Repository 实体映射，例如 `userId` 映射为 `user_id`。跨集合阶段按目标集合的实际 BSON 字段名编写并验收。

下面从收藏中间集合按用户过滤，关联商品和商家，再按 ObjectId 类型的评论者 ID 统计评论，最后按商家汇总。聚合结果在 `FavoriteRepository` 内定义查询投影 `record MerchantStats(@Id ObjectId merchantId, int favoriteCount, int reviewCount) {}`。Service 将投影映射到对外 Facade 中的响应 record。

~~~java
@Aggregation({
    "{ $match: { userId: ?0 } }",
    "{ $lookup: { from: 'products', localField: 'product_id', foreignField: '_id', as: 'product' } }",
    "{ $unwind: '$product' }",
    "{ $lookup: { from: 'merchants', localField: 'product.merchant_id', foreignField: '_id', as: 'merchant' } }",
    "{ $unwind: '$merchant' }",
    """
    { $lookup: { from: 'reviews', let: { productKey: '$product._id', reviewerKey: ?1 }, pipeline: [
      { $match: { $expr: { $and: [ { $eq: ['$product_id', '$$productKey'] },
                                   { $eq: ['$reviewer_id', '$$reviewerKey'] } ] } } },
      { $count: 'total' }
    ], as: 'reviewStats' } }
    """,
    """
    { $group: { _id: '$merchant._id', favoriteCount: { $sum: 1 },
      reviewCount: { $sum: { $ifNull: [ { $arrayElemAt: ['$reviewStats.total', 0] }, 0 ] } } } }
    """,
    "{ $sort: { favoriteCount: -1, _id: 1 } }"
})
Flux<FavoriteRepository.MerchantStats> stats(ObjectId userId, ObjectId reviewerId);
~~~

实测环境为 MongoDB 9.0.4 副本集、Spring Data MongoDB 5.1.2。上面管道返回两组预期结果，根集合匹配、`$expr`、`$in` 和嵌套 `$lookup.let` 的实际命令均保留了 ObjectId。直接 `_id` 匹配时 Spring Data 在该实体上把合法的 String 转成 ObjectId。普通关联字段和 `$lookup.let` 的 String 在实测中仍是 BSON String，只命中同文本的历史字符串字段。统一使用强类型 Repository 签名，避免依赖主键的映射特例。

历史数据优先迁移为统一 BSON 类型。兼容期确需处理旧字符串时，`$toObjectId` 遇到非法值会使聚合失败，`$convert` 可定义 `onError` 和 `onNull`。不要在热查询中逐行转换索引字段。实测直接匹配走 `user_product`，扫描 5 个索引键。对字段做 `$convert` 时扫描了 1,207 条文档，索引键扫描为 0。上线前用代表性数据的 `explain("executionStats")` 核对根集合与各 `$lookup` 的执行计划。[Spring Data 聚合方法](https://docs.spring.io/spring-data/mongodb/reference/mongodb/repositories/query-methods.html#mongodb.repositories.queries.aggregation) · [MongoDB 类型转换](https://www.mongodb.com/docs/manual/reference/operator/aggregation/convert/)

### 乐观锁、SKU 库存和金额原子更新

`@Version` 按业务选用。商品资料等先读取、后提交且可能被多人编辑的数据，使用 `@Version Long version`，更新时检查客户端看到的版本。Spring Data MongoDB 的版本化 `save` 会校验旧版本并递增，冲突抛出 `OptimisticLockingFailureException`。追加写的审计日志、由唯一键保护的关系记录、只走条件原子更新的计数文档通常无需加。使用自定义更新、原生驱动或批量更新时，显式核对更新条件、版本推进和写入结果。版本字段已有框架自动递增时，手写更新也只能递增一次。乐观锁要求服务器确认写入。所有可能覆盖同一业务字段的写入口须遵守同一并发约定。[Spring Data MongoDB 乐观锁](https://docs.spring.io/spring-data/data-mongo/reference/mongodb/template-crud-operations.html#mongo-template.optimistic-locking)

### SKU 防超卖

在写入前校验购买数量为正整数。HTTP/RPC 的购买请求 record 可以接收字符串，Service 校验并转换，库存持久化为整数计数。同一条 MongoDB 更新检查 SKU 主键、可售状态、适用的租户条件和 `available >= quantity`，并用 `$inc` 扣减。根据 `matchedCount` 或 `findAndModify` 是否返回文档判定结果。管理端普通资料编辑只改资料字段，补货和库存调整走单独的原子操作。

~~~java
Query filter = Query.query(Criteria.where("_id").is(skuId)
    .and("status").is("active").and("available").gte(quantity));
Update deduction = new Update().inc("available", -quantity);
return mongo.updateFirst(filter, deduction, SkuEntity.class)
    .map(result -> result.getMatchedCount() == 1);
~~~

这里的 `quantity` 已校验，`false` 由业务层处理为库存不足或 SKU 不可售。扣库存和创建订单需要共同成功时，在同一 MongoDB 事务内执行，并用带业务或租户范围的订单请求幂等键防止重复扣减。预占与释放也要带上订单状态或唯一操作键，避免重复补回。单条 SKU 更新的原子性只覆盖本条文档。[MongoDB 单文档原子更新与事务](https://www.mongodb.com/docs/manual/core/write-operations-atomicity/)

### 金额条件更新

金额在 API 和持久化中保持规范字符串，字段明确币种、精度和单位。余额等需要加减的金额，可使用字符串形式的最小货币单位，例如 `"1000"` 表示 CNY 10.00 元。服务端验证规范格式、币种和范围，用带溢出检查的整数运算计算目标金额。金额不使用 `BigDecimal`、浮点运算或字符串大小比较。

- 固定金额的并发编辑携带期望版本，按 `_id + version` 条件执行 `$set` 并推进版本。
- 余额扣减先读取金额与版本并校验足额，再用 `_id + currency + 旧金额 + 旧版本` 条件执行 `$set` 新金额并递增版本。任一条件不匹配时写入无效，根据业务返回冲突，或重新读取、重新校验、限次重算。字符串金额无法直接用 `$inc`。
- 余额变更与资金流水或订单状态需要一致时放在同一数据库事务里，用唯一操作键幂等。成交单价由服务端从商品或 SKU 取得并保存为快照。涉及两种库存或多份文档时，分别定义各自的更新条件和事务边界。

### 用户与会话

业务用户实体可继承 `UnifyPO<ObjectId>` 并实现 `SecurityLoginUser<ObjectId>`，复用 id、createTime、updateTime。登录快照可用 record 实现 `SecurityLoginUser<String>`，ID 使用十六进制字符串。登录接口对外返回的 record 定义在相应 Facade 接口中。快照保留昵称、状态和必要的权限版本，避免包含密码哈希、验证码和敏感业务字段。

`PrincipalFactory` 是会话抽象，`SecurityPrincipalHolder` 从 Reactor Context 读取当前用户。登录后创建会话，从可信入口验证令牌，并在 Service 按实际身份判断资源权限。认证与授权 Redis 使用独立配置前缀，核对所用 ACL 模式及框架模块能力。应用负责覆盖实际身份与权限规则。

### 分页和索引

`Paging` 对业务使用从 1 开始的页码，默认 1/10。框架转换为从 0 开始的 `PageRequest`。`Paged<T>` 复用 page、size、pages、total、records。页码和大小在入口校验，业务列表限制每页最多 100。RPC 保留 `Mono<Paged<UserFacade.PageItem>>` 的完整泛型。

`ReactiveUnifyPagingRepositoryOperation.sqlPaging` 提供 count 与 query 的协调，也可给 Mongo Repository 使用。两者传相同过滤条件，查询应用 Pageable。排序使用可索引的稳定键，例如 `createdAt DESC, _id DESC`。聊天历史等长期增长列表使用复合游标，游标中的 ID 转为 ObjectId。业务需考虑 count 与查询分开执行时的并发更新。

### 数据库、缓存和运行配置

使用现有 JDK 27 与 IDEA 自带 Maven。框架先执行 `./scripts/mvn.sh install`，应用随后构建。网关运行模式固定 `spring.main.web-application-type=reactive`。业务服务设置 `spring.main.web-application-type=none` 和 `spring.rsocket.server.transport=tcp`，导入 `RsocketInterceptorConfig`。框架在启动时拒绝业务服务的 HTTP 模式和 WebSocket 传输。只向外部开放网关的 HTTP 端口，业务 RSocket TCP 端口限网关与获准调用的 Java 服务访问。

| 功能 | 用法 |
|---|---|
| MongoDB | repository 模块继承 DefaultReactiveMongoRepositoryConfig，使用 lambda.repository.mongo.host/user/password/auth-database/database/deploy/replica。副本集事务配置 cluster 与真实 replica 名。 |
| Redis | repository 模块继承 DefaultReactiveRedisRepositoryConfig，使用 lambda.repository.redis.host/username/password/database/deploy。复用连接，以业务前缀与 TTL 管理键。 |
| RabbitMQ | mq 模块继承 DefaultReactiveRabbitMQConfig。延迟交换机需要插件，处理结果落库后再确认消费。 |
| Nacos | 配置 namespace、group、服务名与注册端口。区分 HTTP 端口与 RSocket 端口。 |
| Qdrant | 示例通过 WebClient 访问，按集合定义维度、距离函数、ID 与 payload。 |
| 对象存储与短信 | 环境值和对接资料见本地《第三方配置.md》。 |

### Nacos 配置与 Data ID 命名

业务应用引入 `lambda-framework-nacos`。端口、路由、数据库、缓存、消息队列和业务开关统一放 Nacos。项目本地只保留加载 Nacos 所需的项目名、必需导入声明和引导参数。环境变量提供 Nacos 地址、namespace ID、group 和登录凭据。Nacos 中可通过占位符引用部署环境注入的第三方凭据。

- Data ID 固定为 `<项目名称>.properties`，与 `spring.application.name` 一致。例如项目 `order-gateway` 使用 `order-gateway.properties`，项目 `user-service` 使用 `user-service.properties`。同一项目的开发、测试和生产使用各自 namespace，Data ID 保持一致。
- 配置读取和服务发现使用明确的 namespace ID 和 group。Data ID 内容类型选 Properties，先发布再启动应用。用于发现的服务名和网关 `rb://` 目标一致。RSocket 服务登记实际 TCP 端口。
- 项目本地 `application.properties` 使用如下引导配置，第一行用已确认的项目名。

~~~properties
spring.application.name=order-gateway
spring.config.import=nacos:${spring.application.name}.properties?group=${NACOS_GROUP}&refreshEnabled=true
spring.cloud.nacos.config.server-addr=${NACOS_SERVER_ADDR}
spring.cloud.nacos.config.namespace=${NACOS_NAMESPACE}
spring.cloud.nacos.discovery.server-addr=${NACOS_SERVER_ADDR}
spring.cloud.nacos.discovery.namespace=${NACOS_NAMESPACE}
spring.cloud.nacos.discovery.group=${NACOS_GROUP}
spring.cloud.nacos.username=${NACOS_USERNAME}
spring.cloud.nacos.password=${NACOS_PASSWORD}
~~~

- 网关的 `order-gateway.properties` 在 Nacos 配置 `spring.main.web-application-type=reactive`、监听地址及端口、`spring.cloud.gateway.server.webflux.routes`。业务服务的 Nacos 配置填写内网 RSocket 地址和端口，并登记实际 TCP 端口。连接数、超时等框架参数也由对应项目的配置文件提供。连接池、监听端口与路由的热更新需逐项验证，涉及启动期绑定的值按发布流程重启。

### RSocket、网关与聊天

Java RPC 的 Facade 方法写完整的 `@MessageMapping`，例如 `order.orderFacade.find`。首段 `order` 是服务发现名，同一个 Facade 的方法使用同一服务名。调用方通过 `@RSocketRpc` 注入 Facade。需要同时提供网关入口时，在同一个注解中加上以 `/` 开头的 HTTP 路径，由网关的 `rb://<服务名>` 路由转发。仅经网关访问的方法可以直接在业务 Controller 上声明路径。

### 按返回类型选择交互方式

| 后端签名 | RSocket 交互 | 经 `rb://` 网关的 HTTP 用法 |
|---|---|---|
| 单条入参，返回 `Mono<T>` | `request/response`，一次请求、一次响应 | 发普通 POST JSON，响应为 JSON，`data` 保留实际值类型。 |
| 单条入参，返回 `Flux<T>` | `request/stream`，一次请求、逐条响应 | 发普通 POST JSON，网关返回逐行 NDJSON。 |
| `TFlux<I>` 入参，返回 `TFlux<O>` | `request/channel`，双向流 | 客户端可持续发送 JSON 数组元素或 NDJSON 行，同时接收 NDJSON 响应。 |

前端只向网关发送 HTTP POST。业务网关只转发 `rb://` 路由，并在转发前检查服务端通过 `@MessageMapping` 发布的固定斜杠路径。网关重写路径时核对重写后的路径。未公开的路径返回 HTTP 404，服务端契约暂不可用时返回 HTTP 503。服务端发布的 JSON 接口由方法签名决定交互模式与响应格式。网关项目配置目标服务和路径范围，业务接口只声明 `@MessageMapping` 和签名。

~~~java
@MessageMapping("/order/find")
public Mono<Detail> find(FindRequest request) {
    return service.find(request);
}

@MessageMapping("/order/status/stream")
public Flux<Status> stream(StatusRequest request) {
    return service.streamStatuses(request);
}

@MessageMapping("/order/chat")
public TFlux<Reply> chat(TFlux<Message> messages) {
    return TFlux.of(service.chat(messages));
}
~~~

`TFlux` 位于 `org.lambda.framework.common.support`，由 `TFlux.of(Flux)` 封装业务流，Java RPC 的 Facade 和网关后端都声明 `TFlux<Reply> chat(TFlux<Message> messages)`。Java 调用时用 `chat(TFlux.of(messages))` 传入 Reactor 流。框架在启动时校验双向流签名。普通 `Flux`、单条对象或缺少泛型的 `TFlux` 不能作为双向流入参，返回值也须是带泛型的 `TFlux`。HTTP 双向流要求客户端和代理支持在请求体尚未发完时读取响应。用 `application/json` 时按 JSON 数组逐段发送，用 `application/x-ndjson` 时每行发送一条请求。客户端收到一条回复后可以继续写下一条。

Java 服务间原生 RPC 固定使用 Apache Fory。两端导入 `RSocketStrategiesConfig` 后，框架从已声明的请求、响应类型中自动收集 record、嵌套泛型、枚举和框架分页类型。保持 Facade 契约同版。旧项目中的多态实际类型或非 record DTO 可用 `ForyRpcTypes` 扩展白名单。服务端拒绝用 JSON 或二进制连接调用点分隔的原生 RPC 路由。网关 JSON 业务路由使用 `/` 前缀。身份元数据沿用 JSON。

网关默认每个后端实例复用一条 RSocket TCP 连接。需要承受单条连接断开的关键路由可在 Nacos 配置 `rsocketConnectionsPerInstance=2`。连接关闭后按最近一次成功发现的目标列表补建连接。请求已发出时由调用方根据业务幂等规则决定如何处理，网关不自动重放。

保持首条消息、单次订阅、上下文、背压和取消。处理首信号时区分数据、错误与完成。遵循 RSocket 和 Spring 的 Payload 所有权，写测试确认释放行为。部署经过 HTTP 代理时实测全双工与缓冲策略。

私聊与群聊用会话 ID 区分，消息包含发送人、类型、内容、时间。表情用 Unicode，图片与视频的消息内容用对象存储 key。网关可按字节转发小二进制。媒体文件上传和下载使用独立链路，权限、群成员校验、消息幂等和离线拉取由业务实现。

### 验收与交付

- 核对父工程能一起构建 service 和 facade。业务微服务的依赖方向为 service → facade，RPC 调用方只引入 facade。
- 核对 `com.<项目名>` 下的包位置、service 包根的业务实现类，以及 facade 模块接口与 service 模块实现的对应关系。
- 核对 Controller、Facade 实现和 Service 共用接口内的请求、响应 record。用同一业务输入分别验证经网关的 HTTP POST 与 Fory RPC，确认响应只包含公开字段。验收时检查业务进程无 HTTP 监听，验证原生 RPC 路由会拒绝 JSON。

先复现旧行为，再在相同条件验证修复。性能记录负载、吞吐、延迟、CPU 与进程内存。测试使用唯一 MongoDB 数据库、Redis 前缀、Qdrant 集合与 RabbitMQ 资源名，结束后先停应用，再按清单清理并复查。临时测试代码和日志放仓库外。README 记录启动、接口、框架用法、验收结果和技能。提交前核对真实环境配置与凭据是否混入 Git。

## 开发规范技能

使用 `$lambda-java-backend` 应用以下规则。技能源文件位于框架仓库的 `skills/lambda-java-backend/SKILL.md`。

~~~markdown
---
name: lambda-java-backend
description: 按统一风格开发、修改或审查 Lambda Framework Java 后端项目。适用于项目命名、POST 接口、Facade 契约、RSocket RPC、MongoDB、分页、用户体系及交付验收。
---

新建项目或微服务前确认项目名称。用户已提供时直接使用。缺少名称时询问，等待期间可以检查现有代码和准备设计。用已确认的名称派生 Maven artifactId、`com.<项目名>` 包根、Spring 应用名、服务发现名和 Nacos Data ID `<项目名称>.properties`。

仓库版规范位于 `../../docs/Java后端开发与架构规范.md`，本机安装版随技能提供 `references/Java后端开发与架构规范.md`。环境值从本地《第三方配置.md》或环境变量读取。提交前检查真实凭据是否混入 Git。

- 业务微服务拆为 `<项目名>-service` 和 `<项目名>-facade`。service 依赖 facade，RPC 调用方只依赖 facade。
- 对外接口放 facade 模块的 `facade` 包，服务端实现放 service 模块的 `facade` 包。业务类 `UserServiceImpl` 直接放 `service` 包，省略业务接口和 `impl` 子包。
- 对外 HTTP 一律经网关转发，使用 POST 动作路径。业务服务只监听内网 RSocket TCP。业务网关仅转发 rb:// 下已发布的固定 @MessageMapping 路径。Controller、Facade 实现与 Service 共用 Facade 接口内的请求、响应 record。签名保留 `UserFacade.FindRequest`、`UserFacade.Detail` 等外层归属，内层名称按业务动作或用途命名。实体使用 `UserEntity` 等名称，仅在 service 模块持久化使用。
- 应用运行配置放 Nacos，本地只留项目名、导入声明和 Nacos 引导配置。网关使用 reactive，以 rb:// 转发 JSON 并保留响应类型。业务进程用 none 和 TCP。Facade 方法只写 @MessageMapping 和签名。原生 Java RPC 自动使用 Fory，从同版 Facade 契约收集 record、枚举和框架分页类型。
- Mongo 主键与关联 ID 使用 ObjectId。HTTP JSON 用十六进制字符串，公开 record 的单值 ID 用 @JsonObjectId ObjectId，Fory 直接处理 ObjectId，Repository 直接绑定原生 ID。枚举实现 ConverterEnum。业务金额与标量用字符串，技术字段按规范转换。
- 每个持久化实体明确表或集合名及唯一的 @Id，可复用基类标识。按 Repository 的筛选、排序、唯一约束或生命周期需求加索引，说明用途并核对实际索引和执行计划。
- 按业务读写比例、访问路径和增长设计关系。多对多必须有中间表或关系集合，一对多和多对一直接内嵌或持有引用。内嵌需有业务上限，独立分页和高频更新用引用。聚合用未加引号的占位符绑定 ObjectId，实测复杂关联与执行计划。
- @Version 按需使用。SKU 以库存足额条件加原子扣减防超卖。字符串金额用旧金额和版本做条件更新，跨文档一致性用事务与幂等键。
- 复用框架分页、异常、Reactor Context 和数据库连接。区分 SecurityLoginUser、UnifyPO、PrincipalFactory。
- RSocket Channel 保持首消息、单次订阅、背压、取消和 Payload 所有权。网关 JSON 聚合有上限，NDJSON 逐条输出。 `rb://` 按后端签名自动选择 Mono 单次响应、Flux 单向流和 入参和返回值均为带泛型 TFlux 的双向流。错误签名在启动时拒绝。聊天 record 描述文字、表情与媒体引用。
- 使用现有 JDK 和 IDEA Maven。先复现再修复，在相同条件复测。使用隔离测试资源，验收后按名称清理，核对数据并关闭本次启动的 JVM。
- README 记录项目运行与契约。报告区分实测、设计方案和未测范围。
~~~
