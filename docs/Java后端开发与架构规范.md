# Java 后端开发与架构规范

版本日期为 **2026-11-09**。本规范供开发者和智能体统一 Java 项目的结构、接口、数据模型和验收方式。部署地址、账号、云资源与 GitHub 接入信息集中到本地《第三方配置.md》。仓库中的示例使用本机地址和环境变量。

## 项目名称与命名

新建项目或微服务前收集项目名称。缺少名称时询问。已有项目沿用已确认的名称与包名前缀。Maven artifactId、Spring 应用名和服务发现名使用项目名称。Java 包使用小写标识符，连字符映射为包层级，组织前缀按项目约定。示例中 chat-api 对应 `org.chat.api`，chat-service 对应 `org.chat.service`。

| 包 | 类名示例 | 职责 |
|---|---|---|
| entity | ChatMessageEntity | 集合或表的实体及所属静态 DTO |
| controller | ChatMessageController | HTTP 或 RSocket 参数校验、响应映射 |
| service | ChatMessageService | 业务规则、事务、协作编排 |
| repository | ChatMessageRepository | BSON 查询、更新、索引、分页 |
| rpc | ChatMessageRpc | 跨服务接口 |
| enums 或所属实体 | MessageKind | 值域、持久化值、显示文案 |
| config | MongoConfiguration | 连接与运行配置 |

类名采用业务名与职责后缀。Service 只有一种实现时直接使用业务 Service 类，有多个实现或跨模块契约时再拆接口。Repository 以归属实体命名。请求 DTO 使用 `SendDTO`、`FindDTO`、`PageDTO` 等动作名，多个查询可以共享 `ViewDTO`。

## 实体、DTO 与接口

- 每个数据库表或集合对应一个实体。所属请求、视图和 RPC DTO 放到实体的 `public static` 内部类或 record。Service 复用 Controller 的 DTO。
- DTO 可继承实体来复用合适的字段。涉及密码哈希、权限、审计、版本与内部状态时，明确选择允许读写的字段。缓存操作等缺少对应数据库实体的请求使用 Controller 内部静态 DTO。
- 所有业务 HTTP 接口使用 POST 动作路径，例子为 `/api/message/send`、`/api/product/page`。条件与 ID 放请求体，空请求用空 DTO。RPC 同样声明具体请求 DTO 和 `Mono<T>`、`Flux<T>`。
- 业务金额、普通业务标量以字符串建模，禁止 BigDecimal。技术字段保留原生语义，例如 MongoDB ObjectId、事务库存计数、乐观锁版本、分页计数、日期和向量维度。业务 DTO 的金额、数量等以字符串入参，Repository 或第三方客户端在明确边界转换。
- CORS 预检、WebSocket 握手和基础设施协议遵从各自协议。业务入口仍统一 POST。

~~~java
@Document("chat_messages")
public class ChatMessageEntity extends UnifyPO<ObjectId> {
    private ObjectId conversationId;
    private MessageKind kind;
    private String content;

    public static record FindDTO(String id) {}
    public static record SendDTO(String conversationId, MessageKind kind, String content) {}
    public static record ViewDTO(String id, MessageKind kind, String content) {}
}
~~~

实际实体需提供 Spring Data 可映射的构造器或访问方法。Service 采用上述 DTO 时无需复制一套 Service 参数类。

## MongoDB 类型、枚举与查询

MongoDB 主键和关联字段用 BSON `ObjectId`。HTTP 与 RPC 使用 24 位十六进制字符串，边界用 `ObjectId.isValid` 校验后转换。Repository 泛型和查询参数使用 `ObjectId`，方法名与字段名称保持一致。

~~~java
public interface ChatMessageRepository extends ReactiveMongoRepository<ChatMessageEntity, ObjectId> {
    @Query("{ '_id': ?0 }")
    Mono<ChatMessageEntity> findWithQuery(ObjectId id);
}
~~~

联调实测中，`findById(ObjectId)`、`@Query` 绑定 ObjectId、带实体映射的 `ReactiveMongoTemplate.findById(hex, Entity.class)` 均命中。原生 `Filters.eq("_id", hex)` 查不到，改成绑定 ObjectId 即命中。MongoDB 的 `@Query` 使用 BSON 查询表达式。原生驱动、Document 和聚合匹配应明确 BSON 类型，关联字段也需一致。历史字符串 ID、DECIMAL128 金额和旧索引应经过迁移后再由新实体读取。

数据库实体包含枚举字段时，枚举实现 `org.lambda.framework.common.enums.ConverterEnum`，提供 `getValue()` 与 `getDescription()`。持久化值保持稳定，例如 `text`、`emoji`、`image`、`video`。框架的 Mongo 配置注册枚举读写转换器。本次联调已核对 MongoDB 存储字符串值以及 API 往返。

## 用户与会话

业务用户实体可继承 `UnifyPO<ObjectId>` 并实现 `SecurityLoginUser<ObjectId>`，复用 id、createTime、updateTime。登录快照在用户实体内定义静态 DTO，可用十六进制字符串实现 `SecurityLoginUser<String>`。快照保留昵称、状态和必要的权限版本，避免包含密码哈希、验证码和敏感业务字段。

`PrincipalFactory` 是会话抽象，`SecurityPrincipalHolder` 从 Reactor Context 读取当前用户。登录后创建会话，从可信入口验证令牌，并在 Service 按实际身份判断资源权限。认证与授权 Redis 使用独立配置前缀，核对所用 ACL 模式及框架模块能力。应用负责覆盖实际身份与权限规则。

## 分页和索引

`Paging` 对业务使用从 1 开始的页码，默认 1/10。框架转换为从 0 开始的 `PageRequest`。`Paged<T>` 复用 page、size、pages、total、records。页码和大小在入口校验，业务列表限制每页最多 100。RPC 保留 `Mono<Paged<ViewDTO>>` 的完整泛型。

`ReactiveUnifyPagingRepositoryOperation.sqlPaging` 提供 count 与 query 的协调，也可给 Mongo Repository 使用。两者传相同过滤条件，查询应用 Pageable。排序使用可索引的稳定键，例如 `createdAt DESC, _id DESC`。聊天历史等长期增长列表使用复合游标，游标中的 ID 转为 ObjectId。业务需考虑 count 与查询分开执行时的并发更新。

## 数据库、缓存和运行配置

使用现有 JDK 27 与 IDEA 自带 Maven。框架先执行 `./scripts/mvn.sh install`，应用随后构建。独立 RSocket 服务配置 `spring.main.web-application-type=none`，网关和 HTTP 服务使用 `reactive`。

| 功能 | 用法 |
|---|---|
| MongoDB | repository 模块继承 DefaultReactiveMongoRepositoryConfig，使用 lambda.repository.mongo.host/user/password/auth-database/database/deploy/replica。副本集事务配置 cluster 与真实 replica 名。 |
| Redis | repository 模块继承 DefaultReactiveRedisRepositoryConfig，使用 lambda.repository.redis.host/username/password/database/deploy。复用连接，以业务前缀与 TTL 管理键。 |
| RabbitMQ | mq 模块继承 DefaultReactiveRabbitMQConfig。延迟交换机需要插件，处理结果落库后再确认消费。 |
| Nacos | 配置 namespace、group、服务名与注册端口。区分 HTTP 端口与 RSocket 端口。 |
| Qdrant | 示例通过 WebClient 访问，按集合定义维度、距离函数、ID 与 payload。 |
| 对象存储与短信 | 环境值和对接资料见本地《第三方配置.md》。 |

## RSocket、网关与聊天

RPC 客户端使用 `@RSocketRpcDiscorvery`、`@RSocketRpcType`，由 `@RSocketRpc` 注入。服务端使用与契约一致的 route，例如 `chat-service.chatMessageRpc.find`。普通请求和服务端流在网关以 `rb://服务名` 路由。服务端流可以用 Accept 请求 NDJSON，逐条刷新。双向流通过 POST NDJSON 或原生 RSocket requestChannel 交换 DTO。

保持首条消息、单次订阅、上下文、背压和取消。处理首信号时区分数据、错误与完成。遵循 RSocket 和 Spring 的 Payload 所有权，写测试确认释放行为。部署经过 HTTP 代理时实测全双工与缓冲策略。

私聊与群聊用会话 ID 区分，消息包含发送人、类型、内容、时间。表情用 Unicode，图片与视频的消息内容用对象存储 key。网关可按字节转发小二进制。媒体文件上传和下载使用独立链路，权限、群成员校验、消息幂等和离线拉取由业务实现。

## 验收与交付

先复现旧行为，再在相同条件验证修复。性能记录负载、吞吐、延迟、CPU 与进程内存。测试使用唯一 MongoDB 数据库、Redis 前缀、Qdrant 集合与 RabbitMQ 资源名，结束后先停应用，再按清单清理并复查。临时测试代码和日志放仓库外。README 记录启动、接口、框架用法、验收结果和技能。提交前核对真实环境配置与凭据是否混入 Git。
