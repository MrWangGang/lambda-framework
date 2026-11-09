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

## 实体注解与主键

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

HTTP 入参的校验注解放在 DTO。静态 DTO 沿用业务字段约定，数据库映射注解集中在持久化实体或基类。

## 关系建模与读写取舍

设计前记录关键接口的读取频率、更新频率、常见筛选和排序、子项数量上限、独立更新或分页需求，以及一致性要求。评估一次读取需要访问多少文档，以及一次更新是否会触发多份副本更新。表结构按这些访问模式设计，索引跟随实际查询。

| 关系 | 结构要求 | 读多时 | 更新多或持续增长时 |
|---|---|---|---|
| 多对多 | 必须建中间表，MongoDB 建独立关系集合。两端 ID 均用 ObjectId，关系实体也有自己的 `@Id` | 收藏列表从 `favorites` 按 `userId` 分页，再按当前页关联商品。商品反查用户时，按反查条件增加索引 | 新增或取消一条收藏只写一条关系记录。用 `(userId, productId)` 唯一约束，租户场景把租户键纳入唯一范围 |
| 一对多 | 直接内嵌或在子表保存父 ID，无需中间表 | 用户与少量收货地址可内嵌。前提是经常一起读取、地址有业务上限且随用户管理 | 会话与聊天消息拆集合，消息保存 `conversationId`，按时间和 `_id` 游标翻页。高频写消息不改同一份会话文档 |
| 多对一 | 多端持有一端 ID，无需中间表 | 商品列表常读店铺名称时，可保留少量展示快照，写清可接受的延迟和刷新方式。需要实时店铺信息时批量查询或用 `$lookup` | 商品持有 `merchantId`，商家资料保存在商家集合。商家频繁变更时集中更新，避免改名触发所有商品重写 |

内嵌是存储决策，`$lookup` 是查询决策。内嵌适用于有界、一起读写且生命周期一致的数据。子项独立分页、授权、更新或增长无上限时用引用。使用引用时，先判断页面是否需要关联数据，再选择批量查询或聚合。尽早按索引过滤。如果关联结果参与过滤、排序或计数，应在对应步骤完成后再分页，保持结果语义。避免逐条查询形成 N+1。上线前确认常用访问路径、索引、关联基数及删除后的孤儿处理。历史订单里的成交快照保留成交时值。[MongoDB 内嵌模型](https://www.mongodb.com/docs/manual/data-modeling/embedding/) · [MongoDB 引用模型](https://www.mongodb.com/docs/manual/data-modeling/referencing/)

## 按业务设计索引

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

## `@Aggregation` 中的 ObjectId

HTTP/RPC DTO 接收十六进制字符串时，在调用 Repository 前校验并转成 `ObjectId` 一次。Repository 的 `@Aggregation` 用未加引号的 `?0`、`?1` 绑定原生参数，批量条件用 `List<ObjectId>`。已是 ObjectId 的值直接传入。各集合关联字段的 BSON 类型必须一致。顶层字段会按 Repository 实体映射，例如 `userId` 映射为 `user_id`。跨集合阶段按目标集合的实际 BSON 字段名编写并验收。

下面从收藏中间集合按用户过滤，关联商品和商家，再按 ObjectId 类型的评论者 ID 统计评论，最后按商家汇总。结果 DTO 定义在 `FavoriteEntity` 内，例如 `public record MerchantStats(@Id ObjectId merchantId, int favoriteCount, int reviewCount) {}`。

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

## 乐观锁、SKU 库存和金额原子更新

`@Version` 按业务选用。商品资料等先读取、后提交且可能被多人编辑的数据，使用 `@Version Long version`，更新时检查客户端看到的版本。Spring Data MongoDB 的版本化 `save` 会校验旧版本并递增，冲突抛出 `OptimisticLockingFailureException`。追加写的审计日志、由唯一键保护的关系记录、只走条件原子更新的计数文档通常无需加。使用自定义更新、原生驱动或批量更新时，显式核对更新条件、版本推进和写入结果。版本字段已有框架自动递增时，手写更新也只能递增一次。乐观锁要求服务器确认写入。所有可能覆盖同一业务字段的写入口须遵守同一并发约定。[Spring Data MongoDB 乐观锁](https://docs.spring.io/spring-data/data-mongo/reference/mongodb/template-crud-operations.html#mongo-template.optimistic-locking)

### SKU 防超卖

在写入前校验购买数量为正整数。HTTP/RPC DTO 可以接收字符串，库存持久化为整数计数。同一条 MongoDB 更新检查 SKU 主键、可售状态、适用的租户条件和 `available >= quantity`，并用 `$inc` 扣减。根据 `matchedCount` 或 `findAndModify` 是否返回文档判定结果。管理端普通资料编辑只改资料字段，补货和库存调整走单独的原子操作。

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

普通 JSON POST 默认选择 request/response 和 response/object。服务端流、双向流、文本、空响应与二进制入口可由网关路由的 metadata 一次定义。未配置模式时，以标准 NDJSON Content-Type 识别双向流，或以显式 NDJSON Accept 识别服务端流。新客户端按接口契约发送请求，已有 RSocket-Model 和 RSocket-Echo 头继续作为无路由配置时的兼容输入。

保持首条消息、单次订阅、上下文、背压和取消。处理首信号时区分数据、错误与完成。遵循 RSocket 和 Spring 的 Payload 所有权，写测试确认释放行为。部署经过 HTTP 代理时实测全双工与缓冲策略。

私聊与群聊用会话 ID 区分，消息包含发送人、类型、内容、时间。表情用 Unicode，图片与视频的消息内容用对象存储 key。网关可按字节转发小二进制。媒体文件上传和下载使用独立链路，权限、群成员校验、消息幂等和离线拉取由业务实现。

## 验收与交付

先复现旧行为，再在相同条件验证修复。性能记录负载、吞吐、延迟、CPU 与进程内存。测试使用唯一 MongoDB 数据库、Redis 前缀、Qdrant 集合与 RabbitMQ 资源名，结束后先停应用，再按清单清理并复查。临时测试代码和日志放仓库外。README 记录启动、接口、框架用法、验收结果和技能。提交前核对真实环境配置与凭据是否混入 Git。
