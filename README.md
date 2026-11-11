# Lambda Framework

面向 JDK 27 和 Spring Boot 4 的响应式 Java 后端框架。业务服务用 MongoDB、Redis、Qdrant 与消息队列。前端走 reactive 网关，Java 服务间走 RSocket Fory。

## 开始使用

1. 使用现有 JDK 27 与 IntelliJ IDEA Maven。用下方命令将框架安装到本机 Maven。
2. 新应用继承 `org.lambda.framework:lambda-framework:1.0.0-SNAPSHOT`，按需引入模块，启动类使用 `@SpringBootApplication`。
3. 提供启用组件的连接配置。框架自动绑定属性、管理客户端生命周期并检查连通性。

~~~bash
export JAVA_HOME="$(/usr/libexec/java_home -v 27)"
MAVEN_BIN="$HOME/Applications/IntelliJ IDEA.app/Contents/plugins/maven-plugin/lib/maven3/bin/mvn"
"$MAVEN_BIN" -version
"$MAVEN_BIN" -B clean install
~~~

路径以 IDEA 的实际安装位置为准。其他环境设置 JDK 27 后，在 IDEA Maven 工具窗口或用本机 Maven 执行相同目标。

| 需求 | Maven 模块 |
|---|---|
| 框架异常码与业务枚举契约 | `lambda-framework-enum` |
| MongoDB、Redis、事务、分页 | `lambda-framework-repository` |
| Qdrant gRPC，`ReactiveQdrantRepository<T>` 写入和查询 | `lambda-framework-qdrant` |
| RabbitMQ、Kafka | `lambda-framework-mq` |
| ZooKeeper 和 Redisson 锁 | `lambda-framework-lock` |
| 雪花 ID | `lambda-framework-guid` |
| RSocket 提供方与 Java RPC 调用端 | `lambda-framework-rsocket`、`lambda-framework-rpc` |
| HTTP/WSS 网关与 Redis 令牌认证 | `lambda-framework-gateway`、`lambda-framework-security` |
| Nacos 配置与发现 | `lambda-framework-nacos` |

[组件配置](#组件配置) · [数据访问与分页](#数据访问与分页) · [会话与身份](#会话与身份) · [GitHub SSH](#github-ssh)

## 普通接口与 Java RPC

普通前端接口在服务端的 `@Controller` 中声明。网关按服务配置 `rb://` 路由前缀，前端发送 POST JSON。Mongo ID 字段声明为 `ObjectId`，框架在 JSON 入口完成类型绑定。

~~~java
@Controller
class UserController {
    @MessageMapping("/api/user/find")
    Mono<Detail> find(@Payload FindRequest request) {
        return users.find(request);
    }

    record FindRequest(@NotNull ObjectId id) {}
    record Detail(ObjectId id, String name) {}
}
~~~

Java RPC 的契约 JAR 放在提供方服务目录内。实现类用 `@RSocketFacade`，调用方字段用 `@RSocketRpc("user")` 指定 Nacos 服务。框架自动生成 `user.userFacade.find`，同一路由注册两次会在启动时失败。

~~~java
public interface UserFacade {
    Mono<Detail> find(FindRequest request);

    record FindRequest(ObjectId id) {}
    record Detail(ObjectId id, String name) {}
}
~~~

返回 `Mono` 生成一次响应，`Flux` 可聚合成 JSON 数组，`UiFlux` 用于单向 NDJSON，`Flux<I> → BiFlux<O>` 用于双向流。前端 WSS 路由使用 `ws://服务名`，网关用 RSocket Channel 转发。示例仓库展示完整链路和认证配置。 [示例仓库](https://github.com/MrWangGang/lambda-framework-example)

RSocket 出站连接按普通请求、单向流和双向流使用独立的 Netty I/O 线程组和连接池。每个应用的 Nacos Data ID 可覆盖以下默认值：

```properties
lambda.rsocket.client.io-workers.unary=2
lambda.rsocket.client.io-workers.stream=2
lambda.rsocket.client.io-workers.channel=2
lambda.rsocket.client.connections-per-instance=2
```

线程数是每种交互在一个 JVM 内的 I/O 工作线程数。连接数是每个下游实例、每种交互和编码各自的 TCP 连接数。网关可用路由元数据 `rsocketConnectionsPerInstance`、`rsocketStreamConnectionsPerInstance` 或 `rsocketChannelConnectionsPerInstance` 覆盖连接数。单条连接支持多个并行请求或流；连接数不限制业务并发数。修改线程数后重启对应应用。


所有 MongoDB 集合实体放在 `po` 包并以 `PO` 结尾，继承 `UnifyPO<ObjectId>`。Spring Data 审计自动维护 `createdAt`、`updatedAt` 和 `@Version`。手写更新和已有数据迁移见 [数据访问与分页](#数据访问与分页)。

Controller 负责绑定和转发。业务逻辑与校验放在 `*ServiceImpl`，Repository 负责数据读写。

## 获取当前用户

注入 `PrincipalFactory`，在当前响应式链路里调用 `currentUserId()` 或 `currentUser(LoginUser.class)`。网关校验 Redis 会话后自动透传身份。Java RPC 的身份元数据和业务请求都使用 Fory。 [会话与身份](#会话与身份)

## 异常码与微服务结构

框架各组件的错误枚举位于 [lambda-framework-enum](lambda-framework-enum/src/main/java/org/lambda/framework/enums)，例如 `GatewayExceptionEnum.UPSTREAM_UNAVAILABLE`。框架通过 `EventException` 抛出枚举，业务 Service 通过 `BusinessException` 抛出本服务的异常枚举。

每个 Java 应用在独立仓库中维护 `-core` 和 `-facade` 模块。业务微服务另设 `-enum` 维护自己的 `B` 码。源码中的应用配置集中在 `-core/src/main/resources/application.properties`。模块与部署目录规范见 [Lambda Java 后端技能](skills/lambda-java-backend/SKILL.md#项目名称包名和-maven-模块)。

`serviceCode` 固定八位。成功为 `00000000`。框架使用 `S` 加七位数字，业务使用 `B` 加七位数字。框架号段按组件分配。

| 框架号段 | 组件 |
|---|---|
| `S00` 至 `S04` | Common、网关、RSocket、RPC、负载均衡 |
| `S05` 至 `S09` | Repository、Security、Compliance、MQ、分布式锁 |
| `S10` 至 `S13` | HTTP 客户端、雪花 ID、Qdrant、Nacos |

## 开发规范与技能

<details>
<summary>展开智能体开发技能与使用入口</summary>

- [Lambda Java 后端技能](skills/lambda-java-backend/SKILL.md) 定义信息收集、代码实现、检查和交付顺序。

</details>

## 组件配置

Spring Boot 依据依赖与配置自动装配。业务应用使用标准启动类。默认对已经配置的组件执行并行连通检查，超时由 `lambda.startup.timeout` 控制。业务服务完成启动检查后再向 Nacos 登记实际 TCP 端口。

### 数据组件

| 组件 | Maven 模块 | 激活属性 | 单机 `single` | 集群 `cluster` |
|---|---|---|---|---|
| MongoDB | `lambda-framework-repository` | `lambda.repository.mongo.uri` | 一个地址，单节点副本集 | 多个种子地址或 SRV，多节点副本集 |
| Redis | `lambda-framework-repository` | `lambda.repository.redis.nodes` | 一个地址，可配置数据库序号 | 至少两个种子地址，数据库 0 |
| Qdrant | `lambda-framework-qdrant` | `lambda.repository.qdrant.endpoints` | 一个 gRPC 地址 | 至少两个 gRPC 地址 |

~~~properties
lambda.repository.mongo.mode=single
lambda.repository.mongo.uri=${MONGODB_URI}
lambda.repository.redis.mode=single
lambda.repository.redis.nodes=${REDIS_NODES}
lambda.repository.redis.username=${REDIS_USERNAME}
lambda.repository.redis.password=${REDIS_PASSWORD}
lambda.repository.qdrant.mode=single
lambda.repository.qdrant.endpoints=${QDRANT_ENDPOINTS}
lambda.repository.qdrant.api-key=${QDRANT_API_KEY}
~~~

Mongo URI 形如 `mongodb://host:27017/app?replicaSet=rs0`。带凭据时对 URI 中的用户名和密码做百分号编码。单机环境使用 `rs.initiate()` 初始化副本集。框架检测实际副本集成员数与声明模式，并自动注册响应式事务操作符。Mongo 自动创建实体声明的索引，可通过 `lambda.repository.mongo.create-indexes=false` 交给部署迁移管理。

Redis 和 Qdrant 的地址列表可写为逗号分隔值。Redis 地址是 `host:port`，默认数据库 0、命令超时 3 秒，可启用 `lambda.repository.redis.tls=true`。Qdrant 地址是 `grpc://host:6334` 或 `grpcs://host:6334`，默认请求超时 5 秒。配置多个 Qdrant 端点时请求轮询，写入重试由业务幂等约束决定。应用的 `@Repository` 继承 `ReactiveQdrantRepository<T>`，构造时绑定集合与结果映射，调用受保护的 `ensureCollection`、`upsert` 和 `query`。集合分片和副本数由创建参数指定。

~~~java
Mono<UpdateResult> saved = qdrant.upsert(upsertPoints);
Flux<ScoredPoint> results = qdrant.query(queryPoints);
~~~

### 消息、锁与 ID

| 组件 | 模块 | 激活属性和模式 |
|---|---|---|
| RabbitMQ | `lambda-framework-mq` | `lambda.mq.rabbitmq.addresses`、`mode`、`username`、`password`、`virtual-host`、`tls` |
| Kafka | `lambda-framework-mq` | `lambda.mq.kafka.bootstrap-servers` 和 `mode` |
| ZooKeeper | `lambda-framework-lock` | `lambda.lock.zookeeper.connect-string`、`mode`、`namespace`、`timeout` |
| Redisson | `lambda-framework-lock` | 随该模块及 Redis 连接配置启用，共用 Redis 两种部署模式 |
| 雪花 ID | `lambda-framework-guid` | `lambda.guid.mode` 和 `lambda.guid.worker` |

模式的默认值是 `single`。集群配置提供多个地址，使用 Docker 时客户端能访问集群公布的地址。RabbitMQ 延迟交换机需要在承载它的 RabbitMQ 节点启用延迟消息插件。Kafka 的 topic 分区、副本和消费者组按业务设置。ZooKeeper 连接串可包含 chroot，锁空间通过 `namespace` 分开。

~~~java
return locks.getLock("order:" + id)
        .withLock(Duration.ofSeconds(2), () -> repository.update(id));
~~~

单实例雪花默认 worker 0。集群设置 `LAMBDA_INSTANCE_INDEX` 或 `lambda.guid.worker`，范围 0–1023，同一个 ID 命名空间中同时运行的实例分配不同编号。生产主机同步时钟。每个 worker 每毫秒最多生成 4096 个 ID，时间回拨或耗尽时应用收到异常。对外建议使用 `GuidFactory.nextId()` 返回字符串。

### 网关、服务发现与认证

- 网关依赖 `lambda-framework-gateway`，使用 reactive 模式。HTTP 路由用 `rb://服务名`，WSS 路由用 `ws://服务名`，按服务划分路径前缀。
- 业务提供方配置 RSocket TCP 端口。Nacos 客户端配置各应用名、namespace 和 group，Data ID 为 `<项目名称>.properties`。集群环境使用可达的 Nacos 入口。
- 网关加 `lambda-framework-security` 即启用 Redis 令牌认证。公开路径写在 `lambda.security.permit-paths`，其他路径由 Bearer 令牌认证。服务通过 `PrincipalFactory` 获取身份。RSocket TCP 端口仅允许网关和可信服务访问。
- 应用 Service 直接以 `Impl` 结尾，用 `@Resource` 注入依赖。普通业务 Controller 写斜杠 `@MessageMapping`。Java Facade 依赖契约 JAR，提供方实现用 `@RSocketFacade`，调用方用 `@RSocketRpc("服务名")`，方法路由自动生成。

## 数据访问与分页

### 1. 数据访问的三层

| 层 | 入口 | 适用范围 |
|---|---|---|
| 公共模型 | `UnifyPO<ID>`、`Paging`、`Paged<T>` | 实体字段、分页请求和结果约定 |
| Repository | MongoDB Repository、`ReactiveRedisOperation`、`ReactiveQdrantRepository<T>` | 数据库读写 |
| Service | `DefaultBasicServiceImpl`、`DefaultTreeServiceImpl` | 通用业务操作组合 |

需要复杂原子条件更新、多集合事务或精确控制查询计划时，可直接使用 `ReactiveMongoTemplate`；示例的商品和订单采用这一路径。不要为了套公共 CRUD 把库存更新拆成“先读、在 Java 计算、再无条件保存”。

### 2. 公共实体

所有 MongoDB 集合实体继承 `UnifyPO<ObjectId>`，统一持久化 `_id`、`createdAt`、`updatedAt` 和 `version`。Spring Data 审计负责实体写入，原子更新使用 `MongoAuditUpdates`。业务需要的租户、软删除等字段由实体单独定义。 [统一字段规范](skills/lambda-java-backend/SKILL.md#实体注解与主键)

#### 旧集合迁移

先在目标项目中确定要迁移的集合和历史时间来源，并暂停旧版本写入。下面以 MongoDB 的 `products` 集合为例。脚本保留已有值，只匹配至少一个公共字段缺失或为 null 的记录，可重跑。示例最后用 ObjectId 的生成时间补齐缺失的创建时间，已有业务审计时间时优先用业务数据。

~~~javascript
const needsAudit = {
  $or: [{createdAt: null}, {updatedAt: null}, {version: null}]
};

db.products.updateMany(needsAudit, [
  {$set: {createdAt: {$ifNull: ["$createdAt", "$createTime", {$toDate: "$_id"}]}}},
  {$set: {
    updatedAt: {$ifNull: ["$updatedAt", "$updateTime", "$createdAt"]},
    version: {$ifNull: ["$version", NumberLong("0")]}
  }}
]);

db.products.countDocuments(needsAudit);
~~~

对其他已确认的集合分别执行，计数为零后启动新版本。旧字段的移除按项目的数据保留策略单独安排。

### 3. 通用 CRUD

`IDefaultBasicService` 包含：

| 方法组 | 行为入口 |
|---|---|
| `insert / update / sync` | 单个和 Publisher 批量保存 |
| `get / find / fuzzy / count` | ID、对象条件、模糊与数量查询 |
| `delete / deleteBy` | 按 ID 或条件删除 |
| `deleteAll` | 删除全部数据，业务接口不应直接暴露 |
| `find(Paging, ...)` | 使用分页适配接口查询 |

服务需要返回匹配的 Repository，其约束包括 ReactiveCrudRepository、ReactiveSortingRepository、ReactiveQueryByExampleExecutor 和框架分页接口。

分页查询由回调实现。Mongo 示例使用 ReactiveMongoTemplate。

批量 Publisher 也需要限制输入规模和并发；`collectList()` 会把结果收集到内存，不能对无限流直接调用。

### 4. Paging 和 Paged 的字段约定

请求：

~~~json
{"page":1,"size":20}
~~~

设计约定是 page 从 1 开始，size 大于 0。框架目前没有统一的 size 上限，HTTP 边界应另设 1..100 等业务限制。

结果设计：

~~~json
{"page":1,"size":20,"total":45,"pages":3,"records":[]}
~~~

示例 POST /api/product/page 使用此响应结构，POST /api/product/cursor 使用独立的复合游标结构。

~~~java
Paging paging = Paging.builder().page(1).size(20).build();
Paging defaults = Paging.builder().build();
~~~

Builder 默认 page=1、size=10。内部 PageRequest 使用从 0 开始的页码。

### 5. 分页适配入口

| 入口 | 回调 | 主要结果 |
|---|---|---|
| `pageFrom` | `MappedPageQuery` | 回调返回 Spring Data Page |
| 条件 `queryPage` | `CountedPageQuery` | count(condition) + query(condition, pageable) |
| 无条件 `queryPage` | `CountedPageSource` | count() + query(pageable) |

业务实现 Mongo 查询和 count 时，遵守以下规则。

1. 两个查询使用完全相同的过滤条件。
2. count 不携带 skip/limit。
3. 排序稳定且有索引。
4. 返回空结果时 total、pages、records 的约定一致。
5. 多次查询之间并发写入可能使 count 与 records 不处于同一快照。

框架在事务内顺序执行 count 和 records。在事务外可并行等待两个查询。

### 6. 页码查询示例

~~~bash
curl -fsS -X POST http://127.0.0.1:8081/api/product/page \
  -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' -d '{"category":"book","page":"1","size":"20"}'
~~~

### 7. 示例已经实现的游标分页

~~~bash
curl -fsS -X POST http://127.0.0.1:8081/api/product/cursor \
  -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' -d '{"category":"book","limit":"20"}'
~~~

空结果结构：

~~~json
{"items":[],"nextBeforeTime":null,"nextBeforeId":null}
~~~

非末页会返回两个 next 字段。下一页必须同时传入：

~~~bash
curl -fsS http://127.0.0.1:8081/api/product/cursor \
  -H "Authorization: Bearer ${TOKEN}" \
  --json '{"category":"book","limit":"20","beforeTime":"上次的 nextBeforeTime","beforeId":"上次的 nextBeforeId"}'
~~~

实际排序为 `createdAt DESC, _id DESC`，索引为 `category ASC, createdAt DESC, _id DESC`。查询取 limit+1 条判断是否有下一页；只返回前 limit 条。

下一页条件是“时间更早，或时间相等而 ID 更小”。ID 用于稳定排序，不用来推断创建时间。分页期间插入或修改数据仍可能改变结果集合，游标不提供跨请求一致快照。

### 8. 页码分页和游标分页如何选

| 场景 | 建议 |
|---|---|
| 后台少量数据，需要跳到第 N 页 | 页码分页，限制深度并核验 count 成本 |
| 信息流、收藏、评论、订单时间线 | 游标分页 |
| 大规模导出 | 范围批次 / 流式处理，避免整表 collectList |
| 搜索引擎深翻页 | 按引擎能力设计 search_after 等方案，单独验收 |
| 必须看到严格一致的历史快照 | 专门的快照 / 导出任务 |

大偏移 skip 随偏移增长需要扫描更多结果。范围条件配合稳定索引更适合连续翻页。[MongoDB skip 与范围查询说明](https://www.mongodb.com/docs/manual/reference/method/cursor.skip/)

### 9. 查询与更新规范

- 索引先覆盖等值过滤，再考虑排序和范围条件；用实际 explain 验证。
- 高频接口限制返回字段，避免每次返回大文档。
- 关系数据增长不设上限时拆集合，避免不断追加收藏 ID。
- 库存使用 stock >= quantity 的条件更新，再原子递减。
- 更新带 version 条件；冲突交给业务重试 / 提示，不做静默覆盖。
- count 和跨集合聚合需要独立观察成本。
- 缓存和 Qdrant 是派生数据；MongoDB 是示例业务事实来源。
- 事务范围内不进行远程副作用；可靠通知采用待实现的 Outbox 流程。

完整关系设计见[示例数据建模手册](https://github.com/MrWangGang/lambda-framework-example#mongodb-建模)。

## 会话与身份

网关引入 `lambda-framework-security`，应用引入 `lambda-framework-compliance`。网关和处理登录的服务使用同一个 Redis 会话库。用 `lambda.security.permit-paths` 声明登录等公开路径，其他请求由网关从 `Authorization: Bearer <token>` 获取令牌并校验会话。

### 业务实现

用户实体可继承 `UnifyPO<ObjectId>`。登录快照使用实现 `SecurityLoginUser<ID>` 的独立类型，保留 ID 和业务需要的字段。实际应用在核验密码或其他凭据后调用 `principalFactory.setPrincipal(loginUser)`。返回的令牌通过专门的响应 record 交给客户端。Redis 按用户维护一个有效会话，重新登录替换旧令牌。

在 Controller、Service 或 Facade 注入 `PrincipalFactory`。在当前请求的 Reactor 链路内取身份。未登录时返回空 Mono，受保护的业务将空值转成自己的未认证错误。

~~~java
return principals.currentUserId()
        .switchIfEmpty(Mono.error(new EventException(SecurityExceptionEnum.AUTHENTICATION_REQUIRED)))
        .flatMapMany(orderRepository::findByCustomerId);
~~~

| 方法 | 用途 |
|---|---|
| `currentUserId()` | 从当前会话令牌取用户 ID，直接读取 Reactor Context |
| `currentUserObjectId()` | 取得 BSON ObjectId，直接传给 MongoDB Repository |
| `currentUser(LoginUser.class)` | 取完整用户快照，按指定类型解码 |
| `currentToken()` | 获取当前会话令牌，供会话维护使用 |
| `updatePrincipal(user)` | 更新当前令牌对应的快照 |
| `revisePrincipal(user)` | 从受保护的管理入口更新指定用户当前会话的快照 |
| `deletePrincipal()` | 注销当前令牌 |

身份读取无需重新访问 Redis。只需要 ID 时使用 `currentUserId()`。MongoDB Repository 用 `ObjectId` 时，直接使用 `currentUserObjectId()`。断开的新订阅、独立定时任务和消息消费者需要自己的身份来源。业务仍负责校验租户、资源归属和操作权限。

### 透传与部署

客户端请求到网关后，网关校验 Redis 会话。框架把身份放入独立的 RSocket 元数据，下游放回 Reactor Context。网关到服务用 JSON，Java RPC 的身份元数据与业务数据都用 Fory。双向流只从首帧恢复该流的身份。

内部 TCP 使用受信任网络模型。把业务 RSocket 端口绑定到回环或专用内网地址，防火墙和网络策略只放行网关与可信服务。获准直接访问端口的进程有权提供身份元数据。公开 HTTP 与 WSS 由边缘入口统一开放。

## GitHub SSH

使用本机已有的 GitHub SSH 密钥。通过 `ssh -T git@github.com` 核对登录账号，通过 `git remote -v` 核对仓库地址。

~~~bash
git remote set-url --push origin git@github.com:MrWangGang/lambda-framework.git
git ls-remote origin refs/heads/main
git push origin main
~~~

示例仓库的 SSH 地址为 `git@github.com:MrWangGang/lambda-framework-example.git`。将公钥添加到 GitHub，私钥保留在本机。先构建并安装框架，再构建示例。若 Maven 报 `release version 27 not supported`，核对 `mvn -version` 中的 JDK。运行时类冲突用 `mvn dependency:tree` 定位。
