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

独立服务设置 `spring.main.web-application-type=none` 和 `spring.rsocket.server.port`。引入 rsocket 模块并导入 RSocketStrategiesConfig、RSocketAcceptorInterceptor、RsocketGlobalExceptionHandler 与 RsocketInterceptorConfig。服务端用 `@MessageMapping` 声明处理器，客户端和服务端共用 DTO、VO 契约。Java RPC 接口使用 `@RSocketRpcDiscorvery("chat-service")`，方法使用 `@RSocketRpcType`，由 `@RSocketRpc` 注入。普通接口 route 为服务名、接口首字母小写名、方法名，用点号连接。

~~~java
@RSocketRpcDiscorvery("chat-service")
public interface ChatMessageRpc {
    @RSocketRpcType
    Mono<ChatMessagePO.DetailVO> find(ChatMessagePO.FindDTO input);
}
~~~

服务端可以用 `@MessageMapping({"chat-service.chatMessageRpc.find", "/rpc/message/find"})` 同时提供 Java RPC 和网关的路由。以下网关使用 `rb://chat-service`，将重写后的请求路径交给 RSocket。网关应用配置 reactive，导入 GatewayFilter、RsocketRequestFactory、GatewayGlobalExceptionHandler、RSocketLoadbalance 与 RSocketStrategies 配置，使用对应的服务发现和 PrincipalStash。

网关项目名为 `chat-gateway` 时，把下面内容发布为 Nacos 的 `chat-gateway.properties`。本地只保留 Nacos 导入所需的引导配置。

~~~properties
spring.main.web-application-type=reactive
server.port=8080
spring.cloud.gateway.server.webflux.routes[0].id=chat
spring.cloud.gateway.server.webflux.routes[0].uri=rb://chat-service
spring.cloud.gateway.server.webflux.routes[0].predicates[0]=Path=/rpc/message/**
~~~

### 自动选择请求与响应类型

| HTTP 请求 | 网关选择 |
|---|---|
| 普通 POST JSON | 一次请求、一次 JSON 响应。默认走 RSocket request/response |
| Content-Type 为 application/x-ndjson | 双向 request/channel，客户端每行一个 DTO，服务端每行一个 VO |
| Accept 明确接受 application/x-ndjson 且请求体是单条 JSON | request/stream，逐条返回 NDJSON |
| 路由定义了类型 | 按路由契约执行 |

普通接口只需发送 JSON 请求体。响应外壳是 JSON 对象，`data` 保留实际类型。VO 是对象，`Mono<String>` 是字符串，`Mono<Integer>` 是数字。JSON RSocket 服务使用框架 `RSocketStrategiesConfig` 编码字符串。路由 metadata 中的值优先。未配置时，已有 RSocket-Model 和 RSocket-Echo 头仍可显式选择，再由标准头和默认值补齐。普通请求缺少 Content-Type 时按 JSON，已确定为 Channel 时按 NDJSON。路由显式声明输入类型时，冲突的 Content-Type 返回 415。

| 路由键 | 可配置值 |
|---|---|
| rsocketModel | request/response、request/stream、request/channel |
| rsocketEcho | response/object、response/char、response/void |
| rsocketContentType | application/json、application/x-ndjson、application/octet-stream |
| rsocketStreamFormat | json、ndjson。适用于 request/stream，控制数组或逐条返回 |
| rsocketConnectionsPerInstance | 1–8，默认 1。每个后端实例复用的连接数 |
| rsocketStreamConnectionsPerInstance | 1–8，request/stream 专用兼容键。配置时优先于通用连接数 |

文本与空响应在路由配置 response/char 或 response/void。二进制接口可配置 rsocketContentType 为 application/octet-stream。业务请求错误沿用 serviceCode/serviceMessage，网关参数、方法、内容类型与超限错误使用相应 HTTP 状态。

~~~bash
curl -fsS --json '{"id":"507f1f77bcf86cd799439011"}' \
  http://127.0.0.1:8080/rpc/message/find
~~~

~~~bash
curl -NsS http://127.0.0.1:8080/rpc/message/channel \
  -H 'Content-Type: application/x-ndjson' \
  --data-binary @messages.ndjson
~~~

示例地址假设对应服务已经启动。messages.ndjson 每行对应服务端 SendDTO。客户端可在收到首条响应后发送下一条请求。部署时复核代理、客户端和 HTTP 版本的双向传输行为。已开始输出后的错误会结束流，客户端按其业务协议重试或续读。

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

版本日期为 **2026-11-09**。本规范供开发者和智能体统一 Java 项目的结构、接口、数据模型和验收方式。部署地址、账号、云资源与 GitHub 接入信息集中到本地《第三方配置.md》。仓库中的示例使用本机地址和环境变量。

### 项目名称与命名

新建项目或微服务前收集项目名称。缺少名称时询问。已有项目沿用已确认的名称与包名前缀。Maven artifactId、Spring 应用名、服务发现名与 Nacos Data ID 的文件名使用项目名称。Java 包使用小写标识符，连字符映射为包层级，组织前缀按项目约定。示例中 chat-api 对应 `org.chat.api`，chat-service 对应 `org.chat.service`。

| 包 | 类名示例 | 职责 |
|---|---|---|
| entity | UserPO | 对应 user 表或集合，包含所属的静态 DTO 和 VO |
| controller | ChatMessageController | HTTP 或 RSocket 参数校验、响应映射 |
| service | ChatMessageService | 业务规则、事务、协作编排 |
| repository | ChatMessageRepository | BSON 查询、更新、索引、分页 |
| rpc | ChatMessageRpc | 跨服务接口 |
| enums 或所属实体 | MessageKind | 值域、持久化值、显示文案 |
| config | MongoConfiguration | 连接与运行配置 |

类名采用业务名与职责后缀。Service 只有一种实现时直接使用业务 Service 类，有多个实现或跨模块契约时再拆接口。Repository 以归属实体命名。持久化类用大驼峰业务名加 `PO`，例如 `UserPO` 映射到 `user`。请求类型按动作命名，例如 `SendDTO`、`FindDTO`、`PageDTO`。响应类型按返回用途命名，例如 `DetailVO`、`SummaryVO`、`PageItemVO`。

### PO、DTO、VO 与接口

- 每个数据库表或集合对应一个 PO。请求 DTO 和响应 VO 定义在其所属 PO 中，使用 `public static` 类或隐式静态的 `public record`。无直接落库的接口按业务归属放在对应的 PO 下。
- Controller、Service 与 RPC 契约复用同一套 DTO、VO。接收使用 DTO，返回使用 VO。RPC 客户端通过共享 Maven 契约依赖引用 `UserPO.FindDTO`、`UserPO.DetailVO`，可以引用外部类名，但 RPC 请求与响应只传内层契约。
- 严禁把数据库 PO 用作接口参数或返回值。DTO 和 VO 不继承 PO，即使字段完全一样也单独声明，只暴露业务契约字段。密码哈希、内部权限、审计字段和持久化版本不进入公共契约。
- 业务 HTTP 接口统一使用 POST 动作路径，例如 `/api/user/find`。请求条件放 JSON 请求体，空请求可用 `EmptyDTO`。RPC 使用具体的 DTO 与 `Mono<DetailVO>`、`Flux<DetailVO>` 等明确类型。底层对字符串与数字的编码兼容性独立测试。
- 业务金额和常规业务标量以字符串建模，禁止 BigDecimal。ObjectId、库存计数、版本号、分页计数和日期保留各自的技术类型。CORS 预检、WebSocket 握手和基础设施协议遵循对应协议。

~~~java
@Document(collection = "user")
public class UserPO {
    @Id
    private ObjectId id;
    private String name;

    public record FindDTO(@JsonObjectId @NotNull ObjectId id) {}
    public record DetailVO(@JsonObjectId ObjectId id, String name) {}
}
~~~

实体提供 Spring Data 可映射的构造器或访问方法。Controller 负责入口校验，Service 负责业务处理和 PO 到 VO 的映射。Repository 接收 PO 或明确的查询参数，数据库主键继续使用原生 ObjectId。

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

HTTP 入参的校验注解放在 DTO。DTO 和 VO 只声明接口需要的字段，持久化映射注解集中在 PO 或基类。

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
public record ChatMessagePO(@Id ObjectId id, ObjectId conversationId,
                                Instant createdAt, String content) {}
~~~

集合的 `_id` 已有主键索引，业务幂等键和关系唯一性单独说明，例如收藏的 `(userId, productId)`。上线前用 `getIndexes()` 核对索引名、字段顺序和选项，用 `explain("executionStats")` 核对常用查询的命中、排序和扫描量。生产集合的索引变更安排在部署迁移中。框架 Mongo 配置在 `ApplicationReadyEvent` 异步提交已发现实体的索引创建，依赖唯一约束的业务要确认创建完成再开放流量。[Spring Data 索引管理](https://docs.spring.io/spring-data/mongodb/reference/mongodb/mapping/mapping-index-management.html) · [MongoDB 复合索引顺序](https://www.mongodb.com/docs/manual/tutorial/equality-sort-range-guideline/)

### MongoDB 类型、枚举与查询

MongoDB 主键和关联字段用 BSON `ObjectId`，Repository 泛型和查询参数也使用 `ObjectId`。HTTP 与 RPC 的 JSON ID 使用 24 位十六进制字符串。DTO 和 VO 的 Mongo ID 字段声明为 `@JsonObjectId ObjectId`，Jackson 2 和 3 在读入时校验并转换，写出时输出小写十六进制字符串。Service 直接把原生 ID 交给 Repository。

~~~java
import org.lambda.framework.common.jackson.JsonObjectId;

// 定义在所属数据库实体类中
public static record FindDTO(@JsonObjectId @NotNull ObjectId id) {}
~~~

`@JsonObjectId` 只作用于标注的单值字段。必填 ID 配合 `@NotNull` 和入口 Bean Validation，游标等可选 ID 允许为空。字符串业务键继续用 `String`。既有字符串 DTO 若暂不迁移，在调用 Repository 前校验并转换一次。

~~~java
public interface ChatMessageRepository extends ReactiveMongoRepository<ChatMessagePO, ObjectId> {
    @Query("{ '_id': ?0 }")
    Mono<ChatMessagePO> findWithQuery(ObjectId id);
}
~~~

联调实测中，`findById(ObjectId)`、`@Query` 绑定 ObjectId、带实体映射的 `ReactiveMongoTemplate.findById(hex, Entity.class)` 均命中。原生 `Filters.eq("_id", hex)` 查不到，改成绑定 ObjectId 即命中。MongoDB 的 `@Query` 使用 BSON 查询表达式。原生驱动、Document 和聚合匹配应明确 BSON 类型，关联字段也需一致。历史字符串 ID、DECIMAL128 金额和旧索引应经过迁移后再由新实体读取。

数据库实体包含枚举字段时，枚举实现 `org.lambda.framework.common.enums.ConverterEnum`，提供 `getValue()` 与 `getDescription()`。持久化值保持稳定，例如 `text`、`emoji`、`image`、`video`。框架的 Mongo 配置注册枚举读写转换器。本次联调已核对 MongoDB 存储字符串值以及 API 往返。

### `@Aggregation` 中的 ObjectId

HTTP/RPC DTO 用 `@JsonObjectId ObjectId` 接收十六进制 JSON 字符串。解码后把原生 ID 直接交给 Repository。Repository 的 `@Aggregation` 用未加引号的 `?0`、`?1` 绑定原生参数，批量条件用 `List<ObjectId>`。已是 ObjectId 的值直接传入。各集合关联字段的 BSON 类型必须一致。顶层字段会按 Repository 实体映射，例如 `userId` 映射为 `user_id`。跨集合阶段按目标集合的实际 BSON 字段名编写并验收。

下面从收藏中间集合按用户过滤，关联商品和商家，再按 ObjectId 类型的评论者 ID 统计评论，最后按商家汇总。聚合结果定义为 `FavoritePO` 内的 VO，例如 `public record MerchantStatsVO(@Id ObjectId merchantId, int favoriteCount, int reviewCount) {}`。

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
Flux<FavoriteEntity.MerchantStats> stats(ObjectId userId, ObjectId reviewerId);
~~~

实测环境为 MongoDB 9.0.4 副本集、Spring Data MongoDB 5.1.2。上面管道返回两组预期结果，根集合匹配、`$expr`、`$in` 和嵌套 `$lookup.let` 的实际命令均保留了 ObjectId。直接 `_id` 匹配时 Spring Data 在该实体上把合法的 String 转成 ObjectId。普通关联字段和 `$lookup.let` 的 String 在实测中仍是 BSON String，只命中同文本的历史字符串字段。统一使用强类型 Repository 签名，避免依赖主键的映射特例。

历史数据优先迁移为统一 BSON 类型。兼容期确需处理旧字符串时，`$toObjectId` 遇到非法值会使聚合失败，`$convert` 可定义 `onError` 和 `onNull`。不要在热查询中逐行转换索引字段。实测直接匹配走 `user_product`，扫描 5 个索引键。对字段做 `$convert` 时扫描了 1,207 条文档，索引键扫描为 0。上线前用代表性数据的 `explain("executionStats")` 核对根集合与各 `$lookup` 的执行计划。[Spring Data 聚合方法](https://docs.spring.io/spring-data/mongodb/reference/mongodb/repositories/query-methods.html#mongodb.repositories.queries.aggregation) · [MongoDB 类型转换](https://www.mongodb.com/docs/manual/reference/operator/aggregation/convert/)

### 乐观锁、SKU 库存和金额原子更新

`@Version` 按业务选用。商品资料等先读取、后提交且可能被多人编辑的数据，使用 `@Version Long version`，更新时检查客户端看到的版本。Spring Data MongoDB 的版本化 `save` 会校验旧版本并递增，冲突抛出 `OptimisticLockingFailureException`。追加写的审计日志、由唯一键保护的关系记录、只走条件原子更新的计数文档通常无需加。使用自定义更新、原生驱动或批量更新时，显式核对更新条件、版本推进和写入结果。版本字段已有框架自动递增时，手写更新也只能递增一次。乐观锁要求服务器确认写入。所有可能覆盖同一业务字段的写入口须遵守同一并发约定。[Spring Data MongoDB 乐观锁](https://docs.spring.io/spring-data/data-mongo/reference/mongodb/template-crud-operations.html#mongo-template.optimistic-locking)

### SKU 防超卖

在写入前校验购买数量为正整数。HTTP/RPC 的购买 DTO 可以接收字符串，Service 校验并转换，库存持久化为整数计数。同一条 MongoDB 更新检查 SKU 主键、可售状态、适用的租户条件和 `available >= quantity`，并用 `$inc` 扣减。根据 `matchedCount` 或 `findAndModify` 是否返回文档判定结果。管理端普通资料编辑只改资料字段，补货和库存调整走单独的原子操作。

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

业务用户实体可继承 `UnifyPO<ObjectId>` 并实现 `SecurityLoginUser<ObjectId>`，复用 id、createTime、updateTime。登录快照在用户实体内定义静态 VO，可用十六进制字符串实现 `SecurityLoginUser<String>`。快照保留昵称、状态和必要的权限版本，避免包含密码哈希、验证码和敏感业务字段。

`PrincipalFactory` 是会话抽象，`SecurityPrincipalHolder` 从 Reactor Context 读取当前用户。登录后创建会话，从可信入口验证令牌，并在 Service 按实际身份判断资源权限。认证与授权 Redis 使用独立配置前缀，核对所用 ACL 模式及框架模块能力。应用负责覆盖实际身份与权限规则。

### 分页和索引

`Paging` 对业务使用从 1 开始的页码，默认 1/10。框架转换为从 0 开始的 `PageRequest`。`Paged<T>` 复用 page、size、pages、total、records。页码和大小在入口校验，业务列表限制每页最多 100。RPC 保留 `Mono<Paged<DetailVO>>` 的完整泛型。

`ReactiveUnifyPagingRepositoryOperation.sqlPaging` 提供 count 与 query 的协调，也可给 Mongo Repository 使用。两者传相同过滤条件，查询应用 Pageable。排序使用可索引的稳定键，例如 `createdAt DESC, _id DESC`。聊天历史等长期增长列表使用复合游标，游标中的 ID 转为 ObjectId。业务需考虑 count 与查询分开执行时的并发更新。

### 数据库、缓存和运行配置

使用现有 JDK 27 与 IDEA 自带 Maven。框架先执行 `./scripts/mvn.sh install`，应用随后构建。网关运行模式固定 `spring.main.web-application-type=reactive`。独立 RSocket 服务只有 TCP 监听时可自行配置 `none`。框架模块由应用决定运行模式。

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

- 网关的 `order-gateway.properties` 在 Nacos 配置 `spring.main.web-application-type=reactive`、监听地址及端口、`spring.cloud.gateway.server.webflux.routes`。连接数、超时等框架参数也由该文件配置。连接池、监听端口与路由的热更新需逐项验证，涉及启动期绑定的值按发布流程重启。

### RSocket、网关与聊天

RPC 客户端使用 `@RSocketRpcDiscorvery`、`@RSocketRpcType`，由 `@RSocketRpc` 注入。服务端使用与契约一致的 route，例如 `chat-service.chatMessageRpc.find`。普通请求和服务端流在网关以 `rb://服务名` 路由。服务端流可以用 Accept 请求 NDJSON，逐条刷新。双向流通过 POST NDJSON 或原生 RSocket requestChannel 持续接收 DTO 和发送 VO。

普通 JSON POST 自动采用一次请求一次响应。网关的响应外壳固定为 JSON 对象，`data` 保留服务端 JSON 类型。例如 `Mono<UserPO.DetailVO>` 输出对象，`Mono<String>` 输出字符串，`Mono<Integer>` 输出数字。客户端无需配置自定义类型头。NDJSON 请求体自动识别为双向流，普通请求明确用 `Accept: application/x-ndjson` 请求逐条响应时自动识别为服务端流。路由需要锁定交互方式时在 Nacos 的 metadata 声明，已有自定义头作为兼容输入。

网关默认每个后端实例复用一条 RSocket TCP 连接。需要承受单条连接断开的关键路由可在 Nacos 配置 `rsocketConnectionsPerInstance=2`。连接关闭后按最近一次成功发现的目标列表补建连接。请求已发出时由调用方根据业务幂等规则决定如何处理，网关不自动重放。

保持首条消息、单次订阅、上下文、背压和取消。处理首信号时区分数据、错误与完成。遵循 RSocket 和 Spring 的 Payload 所有权，写测试确认释放行为。部署经过 HTTP 代理时实测全双工与缓冲策略。

私聊与群聊用会话 ID 区分，消息包含发送人、类型、内容、时间。表情用 Unicode，图片与视频的消息内容用对象存储 key。网关可按字节转发小二进制。媒体文件上传和下载使用独立链路，权限、群成员校验、消息幂等和离线拉取由业务实现。

### 验收与交付

先复现旧行为，再在相同条件验证修复。性能记录负载、吞吐、延迟、CPU 与进程内存。测试使用唯一 MongoDB 数据库、Redis 前缀、Qdrant 集合与 RabbitMQ 资源名，结束后先停应用，再按清单清理并复查。临时测试代码和日志放仓库外。README 记录启动、接口、框架用法、验收结果和技能。提交前核对真实环境配置与凭据是否混入 Git。

## 开发规范技能

使用 `$lambda-java-backend` 应用以下规则。技能源文件位于框架仓库的 `skills/lambda-java-backend/SKILL.md`。

~~~markdown
---
name: lambda-java-backend
description: 按统一风格开发、修改或审查 Lambda Framework Java 后端项目。适用于项目命名、POST 接口、实体内 DTO/VO、RSocket RPC、MongoDB、分页、用户体系及交付验收。
---

新建项目或微服务前确认项目名称。用户已提供时直接使用。缺少名称时询问，等待期间可以检查现有代码和准备设计。用已确认的名称派生 Maven artifactId、Java 基础包、Spring 应用名、服务发现名和 Nacos Data ID `<项目名称>.properties`。

仓库版规范位于 `../../docs/Java后端开发与架构规范.md`，本机安装版随技能提供 `references/Java后端开发与架构规范.md`。环境值从本地《第三方配置.md》或环境变量读取。提交前检查真实凭据是否混入 Git。

- 业务 HTTP 接口采用 POST 动作路径，HTTP、Service 与 RPC 共用请求 DTO 和响应 VO。
- 每个数据库表或集合对应一个 PO。DTO 和 VO 是所属 PO 的静态内部类型，按动作或视图命名。数据库 PO 只用于持久化，接口参数、返回值和契约继承都不能使用 PO。
- 应用运行配置放 Nacos，本地只留项目名、导入声明和 Nacos 引导配置。网关使用 reactive，普通 JSON POST 自动转发并保留响应 JSON 类型。
- Mongo 主键与关联 ID 使用 ObjectId。HTTP 和 RPC 的 JSON 用十六进制字符串，DTO/VO 单值字段用 @JsonObjectId ObjectId，Repository 直接绑定原生 ID。枚举实现 ConverterEnum。业务金额与标量用字符串，技术字段按规范转换。
- 每个持久化实体明确表或集合名及唯一的 @Id，可复用基类标识。按 Repository 的筛选、排序、唯一约束或生命周期需求加索引，说明用途并核对实际索引和执行计划。
- 按业务读写比例、访问路径和增长设计关系。多对多必须有中间表或关系集合，一对多和多对一直接内嵌或持有引用。内嵌需有业务上限，独立分页和高频更新用引用。聚合用未加引号的占位符绑定 ObjectId，实测复杂关联与执行计划。
- @Version 按需使用。SKU 以库存足额条件加原子扣减防超卖。字符串金额用旧金额和版本做条件更新，跨文档一致性用事务与幂等键。
- 复用框架分页、异常、Reactor Context 和数据库连接。区分 SecurityLoginUser、UnifyPO、PrincipalFactory。
- RSocket Channel 保持首消息、单次订阅、背压、取消和 Payload 所有权。网关 JSON 聚合有上限，NDJSON 逐条输出。聊天 DTO/VO 描述文字、表情与媒体引用。
- 使用现有 JDK 和 IDEA Maven。先复现再修复，在相同条件复测。使用隔离测试资源，验收后按名称清理，核对数据并关闭本次启动的 JVM。
- README 记录项目运行与契约。报告区分实测、设计方案和未测范围。
~~~
