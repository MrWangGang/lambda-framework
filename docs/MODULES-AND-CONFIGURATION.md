# 组件配置手册

Spring Boot 依据依赖与配置自动装配。业务应用使用标准启动类。默认对已经配置的组件执行并行连通检查，超时由 `lambda.startup.timeout` 控制。业务服务完成启动检查后再向 Nacos 登记实际 TCP 端口。

## 数据组件

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

## 消息、锁与 ID

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

## 网关、服务发现与认证

- 网关依赖 `lambda-framework-gateway`，使用 reactive 模式。HTTP 路由用 `rb://服务名`，WSS 路由用 `ws://服务名`，按服务划分路径前缀。
- 业务提供方配置 RSocket TCP 端口。Nacos 客户端配置各应用名、namespace 和 group，Data ID 为 `<项目名称>.properties`。集群环境使用可达的 Nacos 入口。
- 网关加 `lambda-framework-security` 即启用 Redis 令牌认证。公开路径写在 `lambda.security.permit-paths`，其他路径由 Bearer 令牌认证。服务通过 `PrincipalFactory` 获取身份。RSocket TCP 端口仅允许网关和可信服务访问。
- Spring Bean 以构造函数注入。普通业务 Controller 写斜杠 `@MessageMapping`。Java Facade 依赖契约 JAR，提供方实现用 `@RSocketFacade`，调用方用 `@RSocketRpc`。
