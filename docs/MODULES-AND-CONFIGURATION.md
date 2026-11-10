# 模块接入与配置字典

[返回总览](../README.md) · [构建](GETTING-STARTED.md) · [用户体系](USER-AND-SECURITY.md)

## 1. 配置类如何生效

Maven 依赖让类进入 classpath，Spring 的扫描 / 导入决定 Bean 是否注册，两者不能混为一谈。以下数据库配置示例放在业务应用可扫描的包中，或用明确的 `@Import` 导入。

推荐先参照示例的 `InfrastructureConfiguration`，只注册当前需要的服务。不要扩大扫描范围到全部 `org.lambda.framework` 后，再用大量空配置绕过不需要的组件。

~~~java
@Configuration(proxyBeanMethods = false)
public class MongoConfiguration extends DefaultReactiveMongoRepositoryConfig {}

@Configuration(proxyBeanMethods = false)
public class RedisConfiguration extends DefaultReactiveRedisRepositoryConfig {}

@Configuration(proxyBeanMethods = false)
public class RabbitConfiguration extends DefaultReactiveRabbitMQConfig {}
~~~

上面三个类分别放入同名 Java 文件。需要导入的类：

- `org.springframework.context.annotation.Configuration`
- `org.lambda.framework.repository.config.mongodb.DefaultReactiveMongoRepositoryConfig`
- `org.lambda.framework.repository.config.redis.DefaultReactiveRedisRepositoryConfig`
- `org.lambda.framework.mq.config.rabbitmq.DefaultReactiveRabbitMQConfig`

## 2. MongoDB

副本集配置：

~~~properties
lambda.repository.mongo.host=DB_HOST:27017
lambda.repository.mongo.user=APP_USER
lambda.repository.mongo.password=APP_PASSWORD
lambda.repository.mongo.auth-database=admin
lambda.repository.mongo.database=catalog
lambda.repository.mongo.deploy=cluster
lambda.repository.mongo.replica=rs0
lambda.repository.mongo.connect-timeout-seconds=60
lambda.repository.mongo.max-idle-time-seconds=30
lambda.repository.mongo.max-size=50
~~~

`DB_HOST / APP_USER / APP_PASSWORD` 是占位符；实际密码通过环境或外部配置注入。这里使用分离的用户名 / 密码属性，不需要把密码手动 URL 编码；只有拼入 URI 时才按 URI 规则编码。

- `host` 使用 `主机:端口`；多个种子节点用逗号分隔。
- `database` 是业务库；`auth-database` 是账号创建所在的认证库。
- `deploy=single` 用于单机连接；`cluster` 用于本项目的副本集配置。
- `replica` 必须和服务端副本集名称一致。
- 种子节点可达还不够，副本集公布的所有成员地址也要能被客户端解析和访问。
- 单节点副本集支持事务，但没有冗余和自动故障转移能力。

配置提供 `ReactiveMongoTemplate` 与 `ReactiveMongoTransactionManager`。业务事务操作器示意：

~~~java
@Bean
TransactionalOperator mongoTransactions(ReactiveMongoTransactionManager manager) {
    return TransactionalOperator.create(manager);
}
~~~

事务内串联同一条响应式链：

~~~java
return firstWrite()
    .then(secondWrite())
    .as(transactions::transactional);
~~~

`firstWrite/secondWrite` 为业务方法占位。不要在事务中调用外部短信、COS、RabbitMQ 并假设它们可以随 MongoDB 一起回滚。

## 3. Redis

~~~properties
lambda.repository.redis.host=REDIS_HOST:6379
lambda.repository.redis.username=APP_ACL_USER
lambda.repository.redis.password=APP_PASSWORD
lambda.repository.redis.database=0
lambda.repository.redis.deploy=single
~~~

`DefaultReactiveRedisRepositoryConfig` 创建 `ReactiveRedisOperation`。默认实现支持 ACL 用户名，使用 String 键和 JSON 值；通过 Redis CLI 看到的字符串可能包含 JSON 引号。

| 方法 | 语义 |
|---|---|
| `set(key, value, seconds)` | 写入并设置秒级 TTL |
| `set(key, value)` | 写入，不设置 TTL |
| `get(key)` | 不存在时返回空 Mono |
| `update(key, value)` | 仅在键存在时更新；不要假定会保留原 TTL |
| `update(key, value, seconds)` | 仅在键存在时更新，并明确设置 TTL |
| `delete(key)` | 返回删除数量 |
| `expire(key, seconds)` | 修改过期时间 |
| `existKey(key)` | 是否存在 |

链式使用示意：

~~~java
return redis.set("catalog:cache:welcome", "hello", 60L)
    .then(redis.<String, String>get("catalog:cache:welcome"));
~~~

业务值需要具体 DTO 时要关注 JSON 反序列化结果。不要把原始 `Map` 强转为业务类。多个 Redis 操作 Bean 同时存在时使用 `@Qualifier` 指定业务缓存、认证或授权操作对象。

示例额外将内部 `LettuceConnectionFactory` 注册为带销毁方法的 Bean，用于应用关闭时释放资源。复制接入时一起检查生命周期；不能认为把连接包装为操作对象后就已经自动托管了所有底层资源。

Redis Cluster 只能使用数据库 0；本次只实测单机 ACL。Sentinel / Cluster / 主从模式存在配置代码，不代表完成故障切换验证。

## 4. RabbitMQ

~~~properties
lambda.mq.rabbitmq.host=MQ_HOST:5672
lambda.mq.rabbitmq.user=APP_USER
lambda.mq.rabbitmq.password=APP_PASSWORD
~~~

地址为 AMQP 5672，不是管理后台 15672。多地址使用逗号分隔。当前默认配置未暴露单独的 virtual host 属性，已有示例使用默认 `/`。

框架提供 `Sender`、`Receiver`、`RabbitMQReactiveOperate`：

1. `declareDelay` 声明 `x-delayed-message` 交换机和队列绑定。
2. `sendDelay` 在消息头写 `x-delay`，单位毫秒，并等待发布确认。
3. `receiverManual` 返回需要手工 ACK 的消费流。
4. `receiverAuto` 自动确认，不适合需要先可靠落库的流程。

注意框架辅助类和示例实现的差别：辅助类中的死信拓扑目前是注释代码，发送属性也没有完整复用示例的持久化配置。示例 `DelayedMessageService` 自己声明了死信交换机/队列、持久消息、落库后 ACK 和幂等回执。不要声称仅调用辅助类就自动具备全部可靠性能力。

延迟消息要求服务端插件版本匹配。发布确认表示 Broker 接受，不代表业务消费者已经处理完成。

## 5. Nacos

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

Nacos 3 的端口用途：8080 控制台、8848 API、9848 Java 客户端 gRPC。本次验收包含配置刷新、注册、注销；standalone 没有集群高可用。RSocket 端口监听器另有待修事项，见 RSocket 手册。

## 6. Gateway、RPC 与响应格式

- 示例使用 reactive 网关统一接入 HTTP 和 WSS。网关通过 Spring WebFlux 处理 HTTP 连接。
- 框架公共 `ResponseTemplete` 定义 `serviceCode / serviceMessage / data`，不代表示例所有 HTTP 响应都有这个外壳。
- Gateway 的请求上下文过滤器可与 Security 模块配合使用，身份信息通过 Reactor Context 传递。
- Gateway 的路由、目标协议和 RSocket 服务名必须明确，不能将 HTTP 端口注册后当作 RSocket TCP 端口使用。
- Facade 方法用 `@MessageMapping` 定义路由。Java 原生 RPC 自动使用 Fory，网关根据已发现的签名自动选择交互。

RPC 使用与已知问题统一见 [RSocket 手册](RSOCKET-AND-VIRTUAL-THREADS.md)。

## 7. 其他模块

| 模块 / 类 | 接入重点 | 验收边界 |
|---|---|---|
| MySQL R2DBC | 继承 `DefaultReactiveMySqlRepositoryConfig`，设置 host/user/password/database | 未部署 MySQL 实测 |
| Elasticsearch | 继承 `DefaultReactiveElasticsearchRepositoryConfig`，设置 host/user/password | 未部署 ES 实测 |
| Kafka | 继承 `DefaultReactiveKafkaMQConfig`，配置 `lambda.mq.kafka.host`，设计 topic/group/offset | 未部署 Kafka 实测 |
| GUID | `GuidFactory.nextId()`；每个实例配置不同 datacenter/machine 组合 | 示例商品使用 UUID |
| Redisson | 选择响应式或普通锁封装；确认 ACL、线程所有权、解锁路径 | 未完成锁竞争 / 故障验收 |
| ZooKeeper | 地址、连接/会话超时与重试；释放锁和客户端 | 未部署 ZooKeeper 实测 |
| HttpClient | `HttpclientResponseHandler` 是响应过滤辅助，调用超时由业务配置 | 非通用 HTTP SDK 自动配置 |

雪花算法机器号和数据中心号按位宽规划在 0..31，不能让多个进程使用相同组合。不要只看类名带 Reactive 就推断所有内部操作都非阻塞。

## 8. 当前源码配置字典

下表从当前源码 `@Value` 提取，覆盖框架自定义 `lambda.*` 属性；不是 Spring Boot 全部属性清单。`-1` 和空值很多是“未配置”的占位，不能原样当作有效连接信息。重复出现的同名属性合并展示。

尤其注意：

- `lambda.lock.redisson.host-1` 是当前源码中的真实拼写，并非文档推荐的新命名。
- 连接池等待属性使用 `max_wait-seconds`，保留当前拼写。
- Security 专用 Redis 配置没有读取 username，见用户手册中的 ACL 缺口。
- 外部组件的自动配置属性请以实际 BOM 版本的官方手册为准。

| 属性 | 源码默认值 | 来源 |
|---|---|---|
| `lambda.guid.datacenter.id` | `-1` | [GuidFactory](../lambda-framework-guid/src/main/java/org/lambda/framework/guid/GuidFactory.java) |
| `lambda.guid.machine.id` | `-1` | [GuidFactory](../lambda-framework-guid/src/main/java/org/lambda/framework/guid/GuidFactory.java) |
| `lambda.lock.redisson.database` | `-1` | [DefaultReactiveRedissonConfig](../lambda-framework-lock/src/main/java/org/lambda/framework/lock/config/DefaultReactiveRedissonConfig.java) |
| `lambda.lock.redisson.deploy` | `single` | [DefaultReactiveRedissonConfig](../lambda-framework-lock/src/main/java/org/lambda/framework/lock/config/DefaultReactiveRedissonConfig.java) |
| `lambda.lock.redisson.host-1` | `空` | [DefaultReactiveRedissonConfig](../lambda-framework-lock/src/main/java/org/lambda/framework/lock/config/DefaultReactiveRedissonConfig.java) |
| `lambda.lock.redisson.master` | `master` | [DefaultReactiveRedissonConfig](../lambda-framework-lock/src/main/java/org/lambda/framework/lock/config/DefaultReactiveRedissonConfig.java) |
| `lambda.lock.redisson.password` | `-1` | [DefaultReactiveRedissonConfig](../lambda-framework-lock/src/main/java/org/lambda/framework/lock/config/DefaultReactiveRedissonConfig.java) |
| `lambda.lock.zookeeper.baseSleepTimeMs` | `5000` | [DefaultReactiveZookeeperConfig](../lambda-framework-lock/src/main/java/org/lambda/framework/lock/config/DefaultReactiveZookeeperConfig.java) |
| `lambda.lock.zookeeper.connectionTimeoutMs` | `10000` | [DefaultReactiveZookeeperConfig](../lambda-framework-lock/src/main/java/org/lambda/framework/lock/config/DefaultReactiveZookeeperConfig.java) |
| `lambda.lock.zookeeper.host` | `-1` | [DefaultReactiveZookeeperConfig](../lambda-framework-lock/src/main/java/org/lambda/framework/lock/config/DefaultReactiveZookeeperConfig.java) |
| `lambda.lock.zookeeper.maxRetries` | `3` | [DefaultReactiveZookeeperConfig](../lambda-framework-lock/src/main/java/org/lambda/framework/lock/config/DefaultReactiveZookeeperConfig.java) |
| `lambda.lock.zookeeper.sessionTimeoutMs` | `10000` | [DefaultReactiveZookeeperConfig](../lambda-framework-lock/src/main/java/org/lambda/framework/lock/config/DefaultReactiveZookeeperConfig.java) |
| `lambda.mq.kafka.host` | `-1` | [DefaultReactiveKafkaMQConfig](../lambda-framework-mq/src/main/java/org/lambda/framework/mq/config/kafka/DefaultReactiveKafkaMQConfig.java) |
| `lambda.mq.rabbitmq.host` | `-1` | [DefaultReactiveRabbitMQConfig](../lambda-framework-mq/src/main/java/org/lambda/framework/mq/config/rabbitmq/DefaultReactiveRabbitMQConfig.java) |
| `lambda.mq.rabbitmq.password` | `-1` | [DefaultReactiveRabbitMQConfig](../lambda-framework-mq/src/main/java/org/lambda/framework/mq/config/rabbitmq/DefaultReactiveRabbitMQConfig.java) |
| `lambda.mq.rabbitmq.user` | `-1` | [DefaultReactiveRabbitMQConfig](../lambda-framework-mq/src/main/java/org/lambda/framework/mq/config/rabbitmq/DefaultReactiveRabbitMQConfig.java) |
| `lambda.repository.elasticsearch.connect-timeout-seconds` | `60` | [DefaultReactiveElasticsearchRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/elasticsearch/DefaultReactiveElasticsearchRepositoryConfig.java) |
| `lambda.repository.elasticsearch.host` | `-1` | [DefaultReactiveElasticsearchRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/elasticsearch/DefaultReactiveElasticsearchRepositoryConfig.java) |
| `lambda.repository.elasticsearch.password` | `-1` | [DefaultReactiveElasticsearchRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/elasticsearch/DefaultReactiveElasticsearchRepositoryConfig.java) |
| `lambda.repository.elasticsearch.socket-timeout-seconds` | `60` | [DefaultReactiveElasticsearchRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/elasticsearch/DefaultReactiveElasticsearchRepositoryConfig.java) |
| `lambda.repository.elasticsearch.user` | `-1` | [DefaultReactiveElasticsearchRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/elasticsearch/DefaultReactiveElasticsearchRepositoryConfig.java) |
| `lambda.repository.mongo.auth-database` | `-1` | [DefaultReactiveMongoRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/mongodb/DefaultReactiveMongoRepositoryConfig.java) |
| `lambda.repository.mongo.connect-timeout-seconds` | `60` | [DefaultReactiveMongoRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/mongodb/DefaultReactiveMongoRepositoryConfig.java) |
| `lambda.repository.mongo.database` | `-1` | [DefaultReactiveMongoRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/mongodb/DefaultReactiveMongoRepositoryConfig.java) |
| `lambda.repository.mongo.deploy` | `-1` | [DefaultReactiveMongoRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/mongodb/DefaultReactiveMongoRepositoryConfig.java) |
| `lambda.repository.mongo.host` | `-1` | [DefaultReactiveMongoRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/mongodb/DefaultReactiveMongoRepositoryConfig.java) |
| `lambda.repository.mongo.max-idle-time-seconds` | `30` | [DefaultReactiveMongoRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/mongodb/DefaultReactiveMongoRepositoryConfig.java) |
| `lambda.repository.mongo.max-size` | `50` | [DefaultReactiveMongoRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/mongodb/DefaultReactiveMongoRepositoryConfig.java) |
| `lambda.repository.mongo.password` | `-1` | [DefaultReactiveMongoRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/mongodb/DefaultReactiveMongoRepositoryConfig.java) |
| `lambda.repository.mongo.replica` | `空` | [DefaultReactiveMongoRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/mongodb/DefaultReactiveMongoRepositoryConfig.java) |
| `lambda.repository.mongo.user` | `-1` | [DefaultReactiveMongoRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/mongodb/DefaultReactiveMongoRepositoryConfig.java) |
| `lambda.repository.mysql.connect-timeout-seconds` | `60` | [DefaultReactiveMySqlRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/mysql/DefaultReactiveMySqlRepositoryConfig.java) |
| `lambda.repository.mysql.database` | `-1` | [DefaultReactiveMySqlRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/mysql/DefaultReactiveMySqlRepositoryConfig.java) |
| `lambda.repository.mysql.host` | `-1` | [DefaultReactiveMySqlRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/mysql/DefaultReactiveMySqlRepositoryConfig.java) |
| `lambda.repository.mysql.max-idle-time-seconds` | `30` | [DefaultReactiveMySqlRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/mysql/DefaultReactiveMySqlRepositoryConfig.java) |
| `lambda.repository.mysql.max-size` | `50` | [DefaultReactiveMySqlRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/mysql/DefaultReactiveMySqlRepositoryConfig.java) |
| `lambda.repository.mysql.password` | `-1` | [DefaultReactiveMySqlRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/mysql/DefaultReactiveMySqlRepositoryConfig.java) |
| `lambda.repository.mysql.port` | `3306` | [DefaultReactiveMySqlRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/mysql/DefaultReactiveMySqlRepositoryConfig.java) |
| `lambda.repository.mysql.user` | `-1` | [DefaultReactiveMySqlRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/mysql/DefaultReactiveMySqlRepositoryConfig.java) |
| `lambda.repository.redis.database` | `空` | [DefaultReactiveRedisRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/redis/DefaultReactiveRedisRepositoryConfig.java) |
| `lambda.repository.redis.deploy` | `single` | [DefaultReactiveRedisRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/redis/DefaultReactiveRedisRepositoryConfig.java) |
| `lambda.repository.redis.host` | `空` | [DefaultReactiveRedisRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/redis/DefaultReactiveRedisRepositoryConfig.java) |
| `lambda.repository.redis.lettuce.pool.max-active` | `8` | [DefaultReactiveRedisRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/redis/DefaultReactiveRedisRepositoryConfig.java) |
| `lambda.repository.redis.lettuce.pool.max-idle` | `8` | [DefaultReactiveRedisRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/redis/DefaultReactiveRedisRepositoryConfig.java) |
| `lambda.repository.redis.lettuce.pool.max_wait-seconds` | `50` | [DefaultReactiveRedisRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/redis/DefaultReactiveRedisRepositoryConfig.java) |
| `lambda.repository.redis.lettuce.pool.min-idle` | `0` | [DefaultReactiveRedisRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/redis/DefaultReactiveRedisRepositoryConfig.java) |
| `lambda.repository.redis.master` | `master` | [DefaultReactiveRedisRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/redis/DefaultReactiveRedisRepositoryConfig.java) |
| `lambda.repository.redis.password` | `空` | [DefaultReactiveRedisRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/redis/DefaultReactiveRedisRepositoryConfig.java) |
| `lambda.repository.redis.username` | `空` | [DefaultReactiveRedisRepositoryConfig](../lambda-framework-repository/src/main/java/org/lambda/framework/repository/config/redis/DefaultReactiveRedisRepositoryConfig.java) |
| `lambda.security.redis.auth.database` | `0` | [SecurityAuthRedisRepositoryConfig](../lambda-framework-compliance/src/main/java/org/lambda/framework/compliance/security/config/SecurityAuthRedisRepositoryConfig.java) |
| `lambda.security.redis.auth.deploy` | `single` | [SecurityAuthRedisRepositoryConfig](../lambda-framework-compliance/src/main/java/org/lambda/framework/compliance/security/config/SecurityAuthRedisRepositoryConfig.java) |
| `lambda.security.redis.auth.host` | `空` | [SecurityAuthRedisRepositoryConfig](../lambda-framework-compliance/src/main/java/org/lambda/framework/compliance/security/config/SecurityAuthRedisRepositoryConfig.java) |
| `lambda.security.redis.auth.lettuce.pool.max-active` | `8` | [SecurityAuthRedisRepositoryConfig](../lambda-framework-compliance/src/main/java/org/lambda/framework/compliance/security/config/SecurityAuthRedisRepositoryConfig.java) |
| `lambda.security.redis.auth.lettuce.pool.max-idle` | `8` | [SecurityAuthRedisRepositoryConfig](../lambda-framework-compliance/src/main/java/org/lambda/framework/compliance/security/config/SecurityAuthRedisRepositoryConfig.java) |
| `lambda.security.redis.auth.lettuce.pool.max_wait-seconds` | `50` | [SecurityAuthRedisRepositoryConfig](../lambda-framework-compliance/src/main/java/org/lambda/framework/compliance/security/config/SecurityAuthRedisRepositoryConfig.java) |
| `lambda.security.redis.auth.lettuce.pool.min-idle` | `0` | [SecurityAuthRedisRepositoryConfig](../lambda-framework-compliance/src/main/java/org/lambda/framework/compliance/security/config/SecurityAuthRedisRepositoryConfig.java) |
| `lambda.security.redis.auth.master` | `master` | [SecurityAuthRedisRepositoryConfig](../lambda-framework-compliance/src/main/java/org/lambda/framework/compliance/security/config/SecurityAuthRedisRepositoryConfig.java) |
| `lambda.security.redis.auth.password` | `空` | [SecurityAuthRedisRepositoryConfig](../lambda-framework-compliance/src/main/java/org/lambda/framework/compliance/security/config/SecurityAuthRedisRepositoryConfig.java) |
| `lambda.security.redis.autz.database` | `1` | [SecurityAutzRedisRepositoryConfig](../lambda-framework-compliance/src/main/java/org/lambda/framework/compliance/security/config/SecurityAutzRedisRepositoryConfig.java) |
| `lambda.security.redis.autz.deploy` | `single` | [SecurityAutzRedisRepositoryConfig](../lambda-framework-compliance/src/main/java/org/lambda/framework/compliance/security/config/SecurityAutzRedisRepositoryConfig.java) |
| `lambda.security.redis.autz.host` | `空` | [SecurityAutzRedisRepositoryConfig](../lambda-framework-compliance/src/main/java/org/lambda/framework/compliance/security/config/SecurityAutzRedisRepositoryConfig.java) |
| `lambda.security.redis.autz.lettuce.pool.max-active` | `8` | [SecurityAutzRedisRepositoryConfig](../lambda-framework-compliance/src/main/java/org/lambda/framework/compliance/security/config/SecurityAutzRedisRepositoryConfig.java) |
| `lambda.security.redis.autz.lettuce.pool.max-idle` | `8` | [SecurityAutzRedisRepositoryConfig](../lambda-framework-compliance/src/main/java/org/lambda/framework/compliance/security/config/SecurityAutzRedisRepositoryConfig.java) |
| `lambda.security.redis.autz.lettuce.pool.max_wait-seconds` | `50` | [SecurityAutzRedisRepositoryConfig](../lambda-framework-compliance/src/main/java/org/lambda/framework/compliance/security/config/SecurityAutzRedisRepositoryConfig.java) |
| `lambda.security.redis.autz.lettuce.pool.min-idle` | `0` | [SecurityAutzRedisRepositoryConfig](../lambda-framework-compliance/src/main/java/org/lambda/framework/compliance/security/config/SecurityAutzRedisRepositoryConfig.java) |
| `lambda.security.redis.autz.master` | `master` | [SecurityAutzRedisRepositoryConfig](../lambda-framework-compliance/src/main/java/org/lambda/framework/compliance/security/config/SecurityAutzRedisRepositoryConfig.java) |
| `lambda.security.redis.autz.password` | `空` | [SecurityAutzRedisRepositoryConfig](../lambda-framework-compliance/src/main/java/org/lambda/framework/compliance/security/config/SecurityAutzRedisRepositoryConfig.java) |
