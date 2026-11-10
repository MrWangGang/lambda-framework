# Java 后端开发与架构规范

版本日期为 **2026-11-10**。本规范供开发者和智能体统一 Java 项目的结构、接口、数据模型和运行配置。部署地址与凭据由部署环境提供。SSH 私钥由本机保管。仓库中的示例使用本机地址和环境变量。

## GitHub 仓库

- 框架源码与使用手册。[lambda-framework](https://github.com/MrWangGang/lambda-framework)
- 可运行的示例与接入说明。[lambda-framework-example](https://github.com/MrWangGang/lambda-framework-example)

## 项目名称、包名和 Maven 模块

新建业务微服务前确认项目名称，缺少名称时询问。父工程 artifactId、Spring 应用名、服务发现名和 Nacos Data ID 的文件名使用项目名称。Java 包根使用 `com.<项目名>`，在其下按职责分包。包名用小写 Java 标识符，项目名中的连字符对应包层级，例如 `order-center` 对应 `com.order.center`。

业务微服务使用一个 Maven 父工程和两个子模块。以下用 `user` 举例。

~~~text
user/
├── pom.xml
├── app/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/user/
│       │   ├── UserApplication.java
│       │   ├── controller/UserController.java
│       │   ├── service/UserServiceImpl.java
│       │   ├── facade/UserFacadeImpl.java
│       │   ├── po/UserPO.java
│       │   └── repository/UserRepository.java
│       └── resources/application.properties
└── facade/
    ├── pom.xml
    └── src/main/java/com/user/
        └── facade/UserFacade.java
~~~

- 父工程使用 `pom` 打包，管理模块与版本。`app/` 是可运行模块，依赖同一服务目录中的 `facade/`。其他微服务通过 `<项目名>-facade` 引入 RPC 契约。
- `facade/` 打包成 `<项目名>-facade` 契约 JAR，`facade` 包内放对外接口和接口内的入参、出参 `record`。该模块只引入编译和使用契约所需的依赖。
- 服务端的 `UserFacadeImpl` 放在 service 模块的 `facade` 包。业务实现直接命名为 `UserServiceImpl`，放在 `service` 包根，省略 `UserService` 接口和 `service.impl` 子包。
- 每个 MongoDB 集合对应一个持久化类，放在 `po` 包并以 `PO` 结尾。`UserPO` 映射到 `user`，由 `UserRepository` 访问。枚举按业务归属放在实体或 `enums` 包，连接配置放在 `config` 包。

## Facade 契约与 record 命名

- 跨服务共享的请求和响应声明为对外 Facade 接口内的 `record`。仅由网关访问的接口可以在 service 模块定义本地请求、响应 record。入参按动作命名，例如 `FindRequest`、`ChangePasswordRequest`、`PageRequest`。返回按用途命名，例如 `Detail`、`Summary`、`PageItem`。外层 Facade 已表达业务归属，内层名称不重复加表名或实体名，省略 DTO、VO、PO 后缀。
- Controller 与 Facade 实现直接注入 `UserServiceImpl`，复用 Facade 的 record。外部类只导入 `UserFacade`，签名写 `UserFacade.FindRequest` 和 `UserFacade.Detail`。公开契约集中在接口，实体和实现类不声明公开入参、出参。
- 实体留在 service 模块。Controller、Facade 接口和 Service 的公开业务方法使用契约 record，Service 负责从持久化实体映射公开字段。密码哈希和内部权限留在服务端。公开契约按业务需要显式选择可见的审计字段或并发控制字段。
- 对外业务 HTTP 统一经网关访问，使用 POST 动作路径，例如 `/api/user/find`。业务 Controller 使用 `@MessageMapping` 声明网关路径。承接网关转发的路径以 `/` 开头，并使用固定路径。动态业务条件放请求体。请求条件放 JSON 请求体，空请求可用 `EmptyRequest`。RPC 保留 `Mono<UserFacade.Detail>`、`Flux<UserFacade.Detail>` 等完整类型。
- 业务金额和常规业务标量以字符串建模，禁止 BigDecimal。ObjectId、库存计数、版本号、分页计数和日期保留各自的技术类型。CORS 预检、WebSocket 握手和基础设施协议遵循对应协议。

~~~java
package com.user.facade;

import jakarta.validation.constraints.NotNull;
import org.bson.types.ObjectId;
import reactor.core.publisher.Mono;

public interface UserFacade {
    Mono<Detail> find(FindRequest request);

    record FindRequest(@NotNull ObjectId id) {}
    record Detail(ObjectId id, String name) {}
}
~~~

`UserFacadeImpl` 标记 `@RSocketFacade` 并实现 `UserFacade`。RPC 方法路由自动生成。若需向前端开放同一业务，由独立 Controller 声明 `@MessageMapping("/api/user/find")`。两者调用同一个 `UserServiceImpl.find(UserFacade.FindRequest)`。

## 实体注解与主键

所有 MongoDB 集合实体都使用普通 Java 类，可以用 Lombok 生成构造方法和访问器。PO 只声明持久化内容，接口 record 放在独立契约中。所有 PO 继承 `UnifyPO<ObjectId>`，并用 `@Document(collection = "...")` 显式声明集合名。包括关系集合、审计记录和消息回执。框架启动时检查已注册的 `@Document` 实体，四个公共属性由超类统一声明。

| Java 字段 | MongoDB 字段与类型 | 使用约定 |
|---|---|---|
| `id` | `_id`，BSON ObjectId | 基类 `@Id`。Repository 的 ID 泛型和关联字段使用 ObjectId |
| `createdAt` | `createdAt`，BSON Date | `@CreatedDate`，首次写入填充，后续保留 |
| `updatedAt` | `updatedAt`，BSON Date | `@LastModifiedDate`，插入和成功修改时填充 |
| `version` | `version`，BSON Int64 | 必须有 `@Version Long`。初始为 0，成功更新递增一次 |

时间字段在 Java 使用 `Instant`。MongoDB 自动装配会启用响应式审计。普通实体 `insert`、`save` 和 Repository 的保存入口由框架填写时间与版本。`@Field` 用于业务属性需要特殊存储名的场景。索引根据业务查询、排序、唯一约束或生命周期需求添加。

HTTP 和 RPC 入参的校验注解放在请求 record 组件上。公开 record 只声明契约需要的字段，持久化映射注解集中在实体或基类。

## 关系建模与读写取舍

设计前记录关键接口的读取频率、更新频率、常见筛选和排序、子项数量上限、独立更新或分页需求，以及一致性要求。评估一次读取需要访问多少文档，以及一次更新是否会触发多份副本更新。表结构按这些访问模式设计，索引跟随实际查询。

| 关系 | 结构要求 | 读多时 | 更新多或持续增长时 |
|---|---|---|---|
| 多对多 | 必须建中间表，MongoDB 建独立关系集合。两端 ID 均用 ObjectId，关系实体同样继承 `UnifyPO<ObjectId>` | 收藏列表从 `favorites` 按 `userId` 分页，再按当前页关联商品。商品反查用户时，按反查条件增加索引 | 新增或取消一条收藏只写一条关系记录。用 `(userId, productId)` 唯一约束，租户场景把租户键纳入唯一范围 |
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
public class ChatMessagePO extends UnifyPO<ObjectId> {
    private ObjectId conversationId;
    private String content;
}
~~~

集合的 `_id` 已有主键索引，业务幂等键和关系唯一性单独说明，例如收藏的 `(userId, productId)`。上线前用 `getIndexes()` 核对索引名、字段顺序和选项，用 `explain("executionStats")` 核对常用查询的命中、排序和扫描量。生产集合的索引变更安排在部署迁移中。框架在启动检查时创建已注册 PO 注解声明的索引，检查完成后开放流量。[Spring Data 索引管理](https://docs.spring.io/spring-data/mongodb/reference/mongodb/mapping/mapping-index-management.html) · [MongoDB 复合索引顺序](https://www.mongodb.com/docs/manual/tutorial/equality-sort-range-guideline/)

## MongoDB 类型、枚举与查询

MongoDB 主键和关联字段用 BSON `ObjectId`，Repository 泛型和查询参数也使用 `ObjectId`。网关 HTTP JSON 和消息 JSON 中的 ID 使用 24 位十六进制字符串。原生 Java RPC 用 Fory 直接传输 ObjectId。公开 record 的 Mongo ID 组件声明为 `ObjectId`，Jackson 2 和 3 在读入时校验并转换，写出时输出小写十六进制字符串。Service 直接把原生 ID 交给 Repository。

~~~java

// 定义在所属 Facade 接口内
record FindRequest(@NotNull ObjectId id) {}
~~~

框架的 Jackson 2 与 Spring Boot Jackson 3 自动处理 `ObjectId` 和 `List<ObjectId>`。必填 ID 配合 `@NotNull` 与入口校验，可选游标允许为空。字符串业务键使用 `String`。

~~~java
public interface ChatMessageRepository extends ReactiveMongoRepository<ChatMessagePO, ObjectId> {
    @Query("{ '_id': ?0 }")
    Mono<ChatMessagePO> findWithQuery(ObjectId id);
}
~~~

使用 `findById(ObjectId)` 和带 ObjectId 参数的 `@Query`。使用原生驱动时，`Filters.eq("_id", id)` 的 `id` 传 ObjectId。MongoDB 的 `@Query` 使用 BSON 查询表达式。原生驱动、Document 和聚合匹配应明确 BSON 类型，关联字段也需一致。历史字符串 ID、DECIMAL128 金额和旧索引应经过迁移后再由新实体读取。

数据库实体包含枚举字段时，枚举实现 `org.lambda.framework.common.enums.ConverterEnum`，提供 `getValue()` 与 `getDescription()`。持久化值保持稳定，例如 `text`、`emoji`、`image`、`video`。框架的 Mongo 配置注册枚举读写转换器。

## `@Aggregation` 中的 ObjectId

供网关 JSON 使用的请求 record 用 `ObjectId` 自动接收十六进制字符串。Java RPC 的 Fory 编解码直接处理 ObjectId。业务层把原生 ID 直接交给 Repository。Repository 的 `@Aggregation` 用未加引号的 `?0`、`?1` 绑定原生参数，批量条件用 `List<ObjectId>`。已是 ObjectId 的值直接传入。各集合关联字段的 BSON 类型必须一致。顶层字段会按 Repository 实体映射，例如 `userId` 映射为 `user_id`。跨集合阶段按目标集合的实际 BSON 字段名编写。

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

统一使用强类型 Repository 签名。聚合根集合匹配、`$expr`、`$in` 和 `$lookup.let` 都绑定原生 ObjectId。Spring Data 的主键映射可能转换合法的 String。普通关联字段和跨集合条件一律显式保持 BSON 类型一致。

历史字符串 ID 在数据迁移时统一转为 BSON ObjectId。迁移脚本可用 `$convert` 的 `onError` 和 `onNull` 显式处理异常值。不要在热查询中逐行转换索引字段。优先对参数完成转换，再直接匹配索引字段。上线前用代表性数据的 `explain("executionStats")` 核对根集合与各 `$lookup` 的执行计划。[Spring Data 聚合方法](https://docs.spring.io/spring-data/mongodb/reference/mongodb/repositories/query-methods.html#mongodb.repositories.queries.aggregation) · [MongoDB 类型转换](https://www.mongodb.com/docs/manual/reference/operator/aggregation/convert/)

## 乐观锁、SKU 库存和金额原子更新

所有集合都使用 `@Version`。通过 `save` 保存已读取的实体时，Spring Data MongoDB 用旧版本匹配并在成功后递增。版本过期时抛出 `OptimisticLockingFailureException`。使用有写确认的连接。编辑接口接收客户端看到的版本，并在查询条件中匹配 `_id + version`。[Spring Data MongoDB 乐观锁](https://docs.spring.io/spring-data/mongodb/reference/mongodb/template-crud-operations.html#mongo-template.optimistic-locking)

`Update`、`findAndModify`、原生驱动和批量写入需要核对审计字段。使用 `MongoAuditUpdates.modified(update)` 同步维护 `updatedAt` 与版本。库存扣减以可用库存作为原子条件，在同一条语句中扣库存、更新日期和递增版本。收藏关系、消息回执等只插入一次的幂等写入，使用 `MongoAuditUpdates.insertOnly(update, createdAt, updatedAt)`，重复命中时保留初始版本和审计时间。

升级已有项目时，先在部署迁移中回填缺失或为空的公共字段，已有值保留。旧 `createTime` 和 `updateTime` 迁到 `createdAt` 和 `updatedAt`，缺失的版本初始化为 BSON Int64 `0`。在所有旧写入方停写后执行迁移，核对缺失数为零，再启动启用版本的应用。见 [数据访问手册](DATA-AND-PAGINATION.md#旧集合迁移)。

### SKU 防超卖

在写入前校验购买数量为正整数。HTTP/RPC 的购买请求 record 可以接收字符串，Service 校验并转换，库存持久化为整数计数。同一条 MongoDB 更新检查 SKU 主键、可售状态、适用的租户条件和 `available >= quantity`，并用 `$inc` 扣减。根据 `matchedCount` 或 `findAndModify` 是否返回文档判定结果。管理端普通资料编辑只改资料字段，补货和库存调整走单独的原子操作。

~~~java
Query filter = Query.query(Criteria.where("_id").is(skuId)
    .and("status").is("active").and("available").gte(quantity));
Update deduction = new Update().inc("available", -quantity);
return mongo.updateFirst(filter, deduction, SkuPO.class)
    .map(result -> result.getMatchedCount() == 1);
~~~

这里的 `quantity` 已校验，`false` 由业务层处理为库存不足或 SKU 不可售。扣库存和创建订单需要共同成功时，在同一 MongoDB 事务内执行，并用带业务或租户范围的订单请求幂等键防止重复扣减。预占与释放也要带上订单状态或唯一操作键，避免重复补回。单条 SKU 更新的原子性只覆盖本条文档。[MongoDB 单文档原子更新与事务](https://www.mongodb.com/docs/manual/core/write-operations-atomicity/)

### 金额条件更新

金额在 API 和持久化中保持规范字符串，字段明确币种、精度和单位。余额等需要加减的金额，可使用字符串形式的最小货币单位，例如 `"1000"` 表示 CNY 10.00 元。服务端验证规范格式、币种和范围，用带溢出检查的整数运算计算目标金额。金额不使用 `BigDecimal`、浮点运算或字符串大小比较。

- 固定金额的并发编辑携带期望版本，按 `_id + version` 条件执行 `$set` 并推进版本。
- 余额扣减先读取金额与版本并校验足额，再用 `_id + currency + 旧金额 + 旧版本` 条件执行 `$set` 新金额并递增版本。任一条件不匹配时写入无效，根据业务返回冲突，或重新读取、重新校验、限次重算。字符串金额无法直接用 `$inc`。
- 余额变更与资金流水或订单状态需要一致时放在同一数据库事务里，用唯一操作键幂等。成交单价由服务端从商品或 SKU 取得并保存为快照。涉及两种库存或多份文档时，分别定义各自的更新条件和事务边界。

## 用户与会话

业务用户实体可继承 `UnifyPO<ObjectId>` 并实现 `SecurityLoginUser<ObjectId>`，复用 `id`、`createdAt`、`updatedAt` 和 `version`。登录快照使用独立类型实现 `SecurityLoginUser<String>`，ID 使用十六进制字符串。登录接口对外返回的 record 定义在相应 Facade 接口中。快照保留昵称、状态和必要的权限版本，避免包含密码哈希、验证码和敏感业务字段。

在 Controller、Service、Facade 注入 `PrincipalFactory`。只需用户 ID 时用 `currentUserId()`，MongoDB 主键用 `currentUserObjectId()`，读取完整快照用 `currentUser(LoginUser.class)`。框架从当前请求的 Reactor Context 读取身份。应用在业务层校验资源归属和权限。

## 分页和索引

`Paging` 对业务使用从 1 开始的页码，默认 1/10。框架转换为从 0 开始的 `PageRequest`。`Paged<T>` 复用 page、size、pages、total、records。页码和大小在入口校验，业务列表限制每页最多 100。RPC 保留 `Mono<Paged<UserFacade.PageItem>>` 的完整泛型。

`ReactiveUnifyPagingRepositoryOperation.queryPage` 提供 count 与 query 的协调，也可给 Mongo Repository 使用。两者传相同过滤条件，查询应用 Pageable。排序使用可索引的稳定键，例如 `createdAt DESC, _id DESC`。聊天历史等长期增长列表使用复合游标，游标中的 ID 转为 ObjectId。业务需考虑 count 与查询分开执行时的并发更新。

## 数据和运行配置

应用选择 Maven 模块，启动类使用 `@SpringBootApplication`。连接配置出现时自动创建客户端和检查，生命周期由 Spring 管理。完整属性与示例见 [组件配置手册](MODULES-AND-CONFIGURATION.md)。

| 场景 | 模块和使用方式 |
|---|---|
| 文档数据 | `lambda-framework-repository` 提供 reactive MongoDB、分页、枚举转换、索引和 `TransactionalOperator` |
| 缓存 | `lambda-framework-repository` 提供 `ReactiveRedisOperation`，用 TTL 和业务键前缀 |
| 向量 | `lambda-framework-qdrant` 提供 `ReactiveQdrantRepository<T>`，按 collection 封装异步写入和向量查询 |
| 消息 | `lambda-framework-mq` 提供 RabbitMQ 和 Kafka 的响应式客户端 |
| 分布式锁 | `lambda-framework-lock` 提供 ZooKeeper 和 Redisson，优先用作用域锁 |
| 雪花 ID | `lambda-framework-guid` 自动提供 `GuidFactory` |

MongoDB 单机使用单节点副本集，集群使用多节点副本集。两种模式的 URI 都声明数据库与 `replicaSet`。Redis 集群使用数据库 0。Redis 的业务键与认证键分开命名。Qdrant 集群先等待 collection 在全部配置端点可见，再开放写入。业务 Repository 继承 `ReactiveQdrantRepository<T>`，负责绑定集合、定义索引和转换查询结果。Service 调用 Repository 并处理业务校验。

认证网关引入 `lambda-framework-security`，配置 Redis 并声明公开路径。业务登录核验用户后用 `PrincipalFactory.setPrincipal` 签发会话令牌。网关校验 Bearer 令牌，框架沿 Reactor Context 和 RSocket 元数据传递身份。内部 RSocket 端口绑定回环或可信内网，网络规则只允许网关和可信服务接入。

## Nacos 配置与 Data ID 命名

业务应用引入 `lambda-framework-nacos`。端口、路由、数据库、缓存、消息队列和业务开关统一放 Nacos。项目本地只保留加载 Nacos 所需的项目名、必需导入声明和引导参数。环境变量提供 Nacos 地址、namespace ID、group 和登录凭据。Nacos 中可通过占位符引用部署环境注入的第三方凭据。

- Data ID 固定为 `<项目名称>.properties`，与 `spring.application.name` 一致。例如项目 `order-gateway` 使用 `order-gateway.properties`，项目 `user-service` 使用 `user-service.properties`。同一项目的各运行环境使用各自 namespace，Data ID 保持一致。
- 配置读取和服务发现使用明确的 namespace ID 和 group。Data ID 内容类型选 Properties，先发布再启动应用。用于发现的服务名与网关 `rb://` 和 `ws://` 的目标一致。RSocket 服务登记实际 TCP 端口。
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

本地配置只保留 Nacos 引导属性，业务、数据库、MQ、锁和网关配置写入 Nacos。Data ID 使用 `<项目名>.properties`。

## RSocket、网关与聊天

RSocket 提供端、RPC 调用方和 reactive 网关通过 Maven 依赖与配置自动装配。Spring Boot 启动类使用 `@SpringBootApplication`。使用 Nacos 时配置各项目的应用名、连接信息与发现组。

Java RPC 的提供类使用 `@RSocketFacade`，每个实现类关联一个最具体的 `*Facade` 接口。调用方通过 `@RSocketRpc("order")` 指定目标 Nacos 服务。提供方读取 `spring.application.name`，两端按相同规则生成 `order.orderFacade.find`。公开的是接口抽象方法，默认方法留在客户端本地执行。同一服务禁止路由重名和 Facade 方法重载，冲突时启动失败。

网关的普通 HTTP 和 WSS 在独立 `@Controller` 上声明固定的斜杠 `@MessageMapping` 路径，按服务路由转发。框架自动接入应用的 Spring `Validator`。入参需要校验时在方法参数和 record 上声明 `@Valid` 与约束注解。Facade JAR 放接口与契约类型，提供方实现放 service 模块。

### HTTP 按返回类型选择交互方式

| 后端签名 | RSocket 交互 | 网关响应 |
|---|---|---|
| 普通入参，返回 `Mono<T>` | `request/response` | 一个 JSON 对象，`data` 保留原类型。 |
| 普通入参，返回 `Flux<T>` | `request/stream` | 网关等结果收齐后返回一个 JSON 对象，`data` 是数组。 |
| 普通入参，返回 `UiFlux<T>` | `request/stream` | 单向 NDJSON，每条响应一行，`data` 是数组。 |
| `Flux<I>` 等普通响应式入参，返回 `BiFlux<O>` | `request/channel` | 双向 NDJSON，客户端可发 JSON 数组或 NDJSON 行，响应逐行返回。 |
| 普通单条入参，返回 `BiFlux<O>` | `request/stream` | 处理一条输入，响应逐行返回 NDJSON。 |

HTTP 业务请求经网关的 `rb://` 路由发送 POST。WebSocket 经网关的 `ws://` 路由访问输入为 `Flux<I>`、返回为 `BiFlux<O>` 的接口。两种入口都核对服务端通过 `@MessageMapping` 发布的固定斜杠路径。按服务划分网关路径前缀。新增方法落在已有前缀内时，业务方法声明映射和签名即可。网关使用重写后的路径查契约。未公开路径返回 404，契约暂不可用返回 503。

~~~java
@MessageMapping("/api/order/summary")
public Mono<Summary> summary(Form request) {
    return service.summary(request);
}

@MessageMapping("/api/order/check")
public Flux<Check> check(Form request) {
    return service.check(request);
}

@MessageMapping("/api/order/watch")
public UiFlux<Check> watch(Form request) {
    return UiFlux.of(service.check(request));
}

@MessageMapping("/api/order/exchange")
public BiFlux<Check> exchange(Flux<Item> items) {
    return BiFlux.of(service.checkItems(items));
}
~~~

`UiFlux` 与 `BiFlux` 位于 `org.lambda.framework.common.support`，用于表达接口返回方式。表单内嵌数组按普通 `List` 字段接收。双向调用的 Java 入参可直接声明 `Flux<I>`，调用方直接传业务流。HTTP 客户端要边发边收，客户端和代理都需支持同一请求同时读写。Java 原生 RPC 直接消费业务类型，HTTP 响应外壳由网关生成。

普通 `Flux` 聚合完成后返回 `{"serviceCode":"E00000000","serviceMessage":"操作成功","data":[...]}`。NDJSON 每条业务元素输出一行，放在该行的 `data` 数组中。零条结果的 `data` 为 `[]`。聚合接口与流式接口共用异常转换，失败外壳的 `data` 为 `[]`。流中途失败时追加一行错误，保留已发出的成功行，并结束响应。

### WSS 路由与服务间双向 RPC

客户端连接 `wss://<网关域名>/ws/api/order/exchange`。在网关的 Nacos 配置内按服务加一条路由，`StripPrefix=1` 去掉 `/ws`。业务服务仍使用 RSocket TCP，并通过 `@MessageMapping("/api/order/exchange")` 公开处理方法。TLS 由网关或其前置入口终止。前置代理应转发 WebSocket Upgrade，认证与授权在网关握手时生效。

~~~properties
spring.cloud.gateway.server.webflux.routes[1].id=order-websocket
spring.cloud.gateway.server.webflux.routes[1].uri=ws://order
spring.cloud.gateway.server.webflux.routes[1].predicates[0]=Path=/ws/api/order/**
spring.cloud.gateway.server.webflux.routes[1].filters[0]=StripPrefix=1
~~~

每条 WebSocket 文本消息包含一个 JSON 请求值。每个响应元素独立发送一条 JSON 文本消息，例如 `{"serviceCode":"E00000000","serviceMessage":"操作成功","data":[{"sku":"A1"}]}`。业务失败发送错误外壳，`data` 为 `[]`，随后关闭连接。客户端可在同一个连接上先收到回复再发送下一条请求。

服务 A 可以将入站 `Flux<I>` 传给服务 B 的 Facade，由服务 B 的 `BiFlux<O>` 直接返回给 A。B 的实现用 `@RSocketFacade`，A 用 `@RSocketRpc("service-b")` 注入。前端到网关是 WebSocket JSON，网关到 A 是 RSocket JSON，A 到 B 是 RSocket Fory。业务逻辑保留响应式链路，取消随订阅传递。

Java 服务间原生 RPC 固定使用 Apache Fory。提供方依赖 `lambda-framework-rsocket`，调用方依赖 `lambda-framework-rpc`。Spring Boot 自动配置编解码器，框架从已声明的请求、响应类型中自动收集 record、嵌套泛型、枚举和框架分页类型。保持 Facade 契约同版。签名之外的多态子类型用 `ForyRpcTypes` 显式注册。服务端拒绝用 JSON 或二进制连接调用点分隔的原生 RPC 路由。网关 JSON 业务路由使用 `/` 前缀。Java RPC 身份元数据使用 Fory，网关到服务的身份元数据使用 JSON。

网关默认每个后端实例复用一条 RSocket TCP 连接。需要承受单条连接断开的关键路由可在 Nacos 配置 `rsocketConnectionsPerInstance=2`。连接关闭后按最近一次成功发现的目标列表补建连接。请求已发出时由调用方根据业务幂等规则决定如何处理，网关不自动重放。

保持首条消息、单次订阅、上下文、背压和取消。处理首信号时区分数据、错误与完成。遵循 RSocket 和 Spring 的 Payload 所有权，按流转移或释放缓冲区。经过代理时保持全双工、按条转发和取消语义。

私聊与群聊用会话 ID 区分，消息包含发送人、类型、内容、时间。表情用 Unicode，图片与视频的消息内容用对象存储 key。网关可按字节转发小二进制。媒体文件上传和下载使用独立链路，权限、群成员校验、消息幂等和离线拉取由业务实现。
