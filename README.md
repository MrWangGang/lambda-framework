# Lambda Framework

面向响应式 Java 服务的模块化基础框架。基于 **JDK 27、Spring Boot 4.1 与 Spring Cloud 2025.1**，提供数据访问、消息通信、RSocket RPC、网关和认证授权组件。

按业务需要引入模块，业务对象和服务接口保留在应用工程中。本仓库不自动部署服务，也不向远程 Maven 仓库发布构件。

## 开始使用

需要 JDK 27 和已有的 Maven 3.6.3+。macOS 用户可以直接复用 IntelliJ IDEA 自带 Maven，仓库脚本不会安装 Maven。

```bash
# 在框架仓库根目录执行
java -version
./scripts/mvn.sh clean install
```

其他安装位置可指定：

```bash
MAVEN_BIN="/path/to/idea/bundled/maven/bin/mvn" ./scripts/mvn.sh clean install
```

`install` 只写入本机 Maven 缓存；远程 `deploy` 默认禁用。然后克隆并构建[示例工程](https://github.com/MrWangGang/lambda-framework-example)，两个仓库放在同一父目录即可。

应用的 `pom.xml`：

```xml
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
    <dependency>
        <groupId>org.lambda.framework</groupId>
        <artifactId>lambda-framework-mq</artifactId>
    </dependency>
</dependencies>
```

## 模块导航

| 模块 | 主要职责 |
|---|---|
| `common` | 统一异常、响应模型、JSON 和公共工具 |
| `repository` | 响应式 Redis、MongoDB、R2DBC 与 Elasticsearch 数据访问 |
| `compliance` | 基础 CRUD / 树形服务约定与用户上下文 |
| `web` | WebFlux 请求处理、序列化和异常响应 |
| `rsocket` | RSocket 拦截、异常处理与消息交互 |
| `rpc` | RSocket RPC 接口发现和客户端代理 |
| `loadbalance` | 服务实例选择与负载均衡 |
| `gateway` | 响应式网关及过滤器 |
| `nacos` | Nacos 配置、服务发现和 RSocket 端口注册 |
| `security` | 身份认证、路径授权和 Redis 状态管理 |
| `httpclient` | 响应式 HTTP 调用 |
| `guid` | ID 生成 |
| `lock` | Redisson / ZooKeeper 分布式锁支持 |
| `mq` | RabbitMQ 与 Kafka 响应式消息支持 |

实际 Maven artifactId 均为 `lambda-framework-` 加模块名。Redis 属于 `repository`，不存在单独的 `lambda-framework-redis` 模块。

## Redis 与 MongoDB

继承对应配置类并注册为 Spring 配置，即可复用框架的连接与操作对象：

```java
@Configuration
public class RedisConfig extends DefaultReactiveRedisRepositoryConfig {}

@Configuration
public class MongoConfig extends DefaultReactiveMongoRepositoryConfig {}
```

Redis 配置示例，凭据由运行环境注入：

```properties
lambda.repository.redis.host=${REDIS_HOST:127.0.0.1:6379}
lambda.repository.redis.username=${REDIS_USERNAME:default}
lambda.repository.redis.password=${REDIS_PASSWORD:}
lambda.repository.redis.database=0
lambda.repository.redis.deploy=single
```

Redis 支持 ACL 用户名。MongoDB 单机模式不要求副本集名称；多文档事务仍需要 MongoDB 副本集或分片集群。所有数据库应使用与环境对应的独立账号和配置。

副本集与事务配置：

```properties
lambda.repository.mongo.host=${MONGO_HOST:127.0.0.1:27017}
lambda.repository.mongo.user=${MONGO_USER:root}
lambda.repository.mongo.password=${MONGO_PASSWORD}
lambda.repository.mongo.auth-database=admin
lambda.repository.mongo.database=catalog
lambda.repository.mongo.deploy=cluster
lambda.repository.mongo.replica=${MONGO_REPLICA_SET:rs0}
```

框架会提供 `ReactiveMongoTransactionManager`，业务可通过 `TransactionalOperator` 组合多个 MongoDB 写操作。将同一事务内的调用串联为一条 Reactor 流，不能在业务中另行 `subscribe()` 切断事务上下文。

## RPC 接口

服务接口使用 `@RSocketRpcDiscorvery` 标记目标服务，方法使用 `@RSocketRpcType`；实现类使用 `@RSocketRpcApi`；客户端通过 `@RSocketRpc` 注入。

```java
@RSocketRpcDiscorvery("catalog-service")
public interface CatalogApi {
    @RSocketRpcType
    Mono<String> findName();
}
```

注解名称沿用已有 API（包括 `Discorvery` 拼写），避免破坏使用方。当前示例仓库聚焦商品业务与基础设施接入；RPC API 保留在框架模块中，未将跨服务 RPC 计入本次实机验收。

## 版本与升级

- [依赖版本清单](docs/DEPENDENCIES.md)：Maven Central 核验日期、版本管理策略与关键组件。
- [JDK 27 迁移说明](docs/MIGRATION-JDK27.md)：Boot 4 / Security 7 / Nacos 配置变化。
- [可运行示例与验收记录](https://github.com/MrWangGang/lambda-framework-example)：真实服务连接、延迟消息与测试数据清理。

[本次测试记录](https://github.com/MrWangGang/lambda-framework-example/blob/main/docs/TESTING.md)覆盖全部模块构建和基础设施联调。

Spring 生态的传递依赖由 Boot / Cloud BOM 协同管理；未采用预览版或 SNAPSHOT 外部依赖。框架版本仍为 `1.0.0-SNAPSHOT`，表示当前仓库的本地开发版本。升级包含不兼容变化，现有 Boot 3 应用应按迁移说明调整。
