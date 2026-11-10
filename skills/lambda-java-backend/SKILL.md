---
name: lambda-java-backend
description: 创建、开发和审查 Lambda Framework Java 后端。涵盖 Nacos 配置、PO 与 MongoDB、身份透传、网关、Fory RPC、流式接口、MQ、向量检索、锁和部署验收。
---

# Lambda Java 后端开发

## 1. 收集项目与部署信息

先找项目名、现有源码与部署记录。项目名决定 Maven 模块、Java 包根、Nacos 服务名及 Data ID。缺少项目名时询问用户。配置只收集业务启用的组件，缺失项一次问齐。

| 范围 | 需要的信息 |
|---|---|
| 项目 | 项目名、业务边界、仓库与分支、JDK 和 Maven、运行目标 |
| 服务器 | 地址、SSH 用户、认证方式、网络与端口范围、部署恢复目标 |
| Nacos 与网关 | Nacos 地址、namespace ID、group、凭据引用、服务名、网关地址、RSocket 端口、公开路径 |
| MongoDB | 单机或集群、副本集 URI、数据库、凭据引用 |
| Redis | 模式、节点、数据库序号、ACL 用户、凭据引用 |
| Qdrant | 模式、gRPC 地址、API key 引用、集合、维度与距离 |
| RabbitMQ、Kafka、ZooKeeper | 使用的组件、单机或集群、节点、RabbitMQ vhost、Kafka topic 与 group、ZooKeeper 锁命名空间、认证 |
| 业务 | 登录校验、可信网络范围、雪花 worker 分配、外部接口与凭据引用 |

真实服务器信息保存在本机安装版的 `references/deployment.local.md`，凭据从本机私有配置或密钥管理读取。公开仓库只放占位值。

## 2. 配置与模块

- 本地只留 Nacos 引导属性。用 `nacos:<项目名>.properties` 导入配置。MongoDB、Redis、Qdrant、MQ、锁、路由、业务参数全部按应用写入 Nacos，Data ID 与项目名一致。同环境的网关和服务使用同一 namespace 和 group。
- 示例配置模板默认单机，集群属性给出注释版本。MongoDB 单机也用副本集，Redis 集群使用 DB 0，RabbitMQ 延迟插件在接入节点启用，集群队列用 quorum。ZooKeeper、Kafka 等按连接属性自动启用。未使用时省略连接属性。雪花集群为每个实例分配不同 worker。
- 启动类用 `@SpringBootApplication`，由框架自动装配和检查连接。应用只声明依赖、Nacos 配置和业务组件。部署共享服务时，限制 RSocket 和中间件的网络访问范围。

## 3. 代码与持久化

- 包根使用 `com.<项目名>`，按 `controller`、`service`、`repository`、`po` 和业务需要的 `contract` 细分。持久化类用 `UserPO`，放 `po` 包，使用普通 Java 类和 Lombok。仓储用 `UserRepository`，服务实现用 `UserServiceImpl`。
- 每个 MongoDB 集合的 PO 继承 `UnifyPO<ObjectId>`。公共字段为 `id`、`createdAt`、`updatedAt` 和 `@Version Long version`。在 PO 声明 `@Document` 和按业务设计的索引。PO 只放持久化字段、映射和业务枚举，接口请求和响应使用独立契约 record。
- 普通保存由框架审计。用 `MongoAuditUpdates.modified` 维护原子更新的时间和版本，用 `insertOnly` 维护幂等插入。编辑匹配客户端版本，库存扣减匹配足额条件。部署新版本前补齐旧集合公共字段。
- MongoDB 请求 ID 自动绑定 ObjectId，Repository 直接传对象，聚合占位符不加引号。多对多用独立关系集合，一对多和多对一按访问模式选择嵌入或引用。金额用字符串。索引跟随实际过滤、排序和唯一约束，深分页用时间加 ObjectId 游标。
- Qdrant Repository 继承 `ReactiveQdrantRepository<T>`，服务通过 Repository 执行响应式检索。前端向量用数值数组。涉及持久化枚举时实现框架 `ConverterEnum`。

## 4. 接口、身份与 RPC

- 前端经 reactive 网关访问。Controller 只用 `@MessageMapping("/api/...")` 定义固定路径，普通 HTTP 统一 POST，使用独立 record 作为参数与结果。校验由框架自动接入 Spring Validator，业务声明 `@Valid` 和约束注解。
- 提供者把 `facade/` 契约 JAR 放在服务目录内。实现类加 `@RSocketFacade`，每个实现类对应一个 `*Facade` 接口。调用者只依赖契约 JAR，并用 `@RSocketRpc("服务名")`。框架生成方法路由，拒绝重复路由和重载。RPC 业务与身份元数据固定 Fory。网关内部转发用 JSON。
- `Mono` 单次响应，`Flux` 聚合数组，`UiFlux` 单向 NDJSON，`Flux<I> → BiFlux<O>` 用于 WSS 或 Java 双向 RPC。网关路由按服务前缀配置。
- 登录核验后用 `PrincipalFactory.setPrincipal` 签发会话。网关用 Redis 校验令牌并透传身份。用 `currentUserId()`、`currentUserObjectId()` 或 `currentUser(type)` 从 Reactor Context 获取，业务检查资源权限。内部 RSocket 只开放给可信调用方。

## 5. 交付

示例覆盖组件的实际调用和可启用的配置。构建并验收正常入口、错误路径及启用的部署模式。使用隔离数据和配置，验收后清理临时资源并停止本次启动的应用。公开 README 和本技能写用法，结果记入交付报告。

细节见 [Java 后端开发与架构规范](../../docs/Java后端开发与架构规范.md) 和 [组件配置](../../docs/MODULES-AND-CONFIGURATION.md)。
