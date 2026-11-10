---
name: lambda-java-backend
description: 用 Lambda Framework 新建、接入或审查 Java 服务。处理项目命名、环境配置、RSocket 网关与 Facade、MongoDB/Redis/Qdrant、消息与锁、身份传递和交付核验。
---

## 1. 收集项目与环境信息

先读取用户给出的项目名、已有配置和本机私有环境记录。缺失时一次性询问当前功能所需的必填项。项目名决定 Maven 名称、Java 根包、Nacos 服务名和 Data ID。环境配置按下表收集，只启用实际使用的模块。

| 场景 | 必填信息 |
|---|---|
| 项目与交付 | 项目名、业务边界、源码路径或 Git 地址、分支、JDK/Maven、运行环境 |
| 服务器 | 服务器地址、SSH 用户、认证方式、内外网访问范围、可用端口、部署与恢复目标 |
| 网关与服务发现 | 网关域名和端口、服务名与 RSocket 端口、Nacos 地址、namespace ID、group、凭据引用、公开路径 |
| MongoDB | `single` 或 `cluster`、副本集 URI、数据库、凭据引用 |
| Redis | 模式、地址列表、数据库序号、TLS、用户名和凭据引用 |
| Qdrant | 模式、gRPC 地址、API key 引用、collection、向量维度与距离 |
| RabbitMQ / Kafka / ZooKeeper | 模式、地址列表、vhost 或 topic/group 或命名空间、所需认证 |
| 登录与跨服务身份 | 登录校验方式、公开路径、Redis 连接、内网访问范围 |
| 雪花 ID | 单机使用默认 worker，集群提供实例编号分配方式 |
| 外部服务 | 域名、接口和凭据引用、允许的测试范围 |

真实服务器信息只写入**本机已安装技能**的 `references/deployment.local.md`。凭据在本机私有配置或密钥管理中保管，该记录保存取值位置。公开仓库使用变量占位符。用户给出信息后更新本机记录并核对启用组件的配置清单。

## 2. 选择模块与实现业务

- 按项目名建立包根 `com.<项目名>`。提供 RPC 的服务把 `facade/` 契约 JAR 放在服务目录内，与可运行的 `app/` 并列。调用方只依赖提供方的 Facade JAR。
- 业务实现类用 `UserServiceImpl`，持久化实体用 `UserEntity`，仓储用 `UserRepository`。契约使用业务动作命名的 record。只经网关访问的 record 可留在业务模块。
- 启动类使用 `@SpringBootApplication`。按已启用的依赖与连接属性自动装配。普通接口用 `@MessageMapping("/api/...")`，客户端经网关 POST。服务间 RPC 由 `@RSocketFacade` 提供，`@RSocketRpc` 调用，固定 Fory。WSS 使用 `Flux<I> → BiFlux<O>`。
- 使用 MongoDB、Redis 和 Qdrant。Mongo 单机也用副本集。向量数据用继承 `ReactiveQdrantRepository<T>` 的 Repository 操作。Mongo ID 从 JSON 自动转为 `ObjectId`，Repository 直接绑定对象。聚合使用未加引号的占位符，业务金额用字符串，库存用带条件的原子扣减。
- 身份统一从受信任的 Reactor Context 获取。业务登录核验成功后签发令牌。网关验证令牌后自动传递身份。业务通过 `PrincipalFactory.currentUserId()` 或 `currentUser(type)` 获取。内部 RSocket 只开放给可信调用方。
- 添加分页、关系建模、索引、锁、消息或身份功能前，按下方参考查对应规则。

## 3. 核验并交付

构建所有受影响模块，验证正常入口、错误路径和配置失败提示。对照启用清单逐项记录实际验证结果。集群验证使用隔离环境，结束后核对恢复状态、清理测试资源并停止本次启动的应用。公开 README 写运行与使用，验收结果放交付报告。

## 参考

- 项目布局、接口、持久化、用户、并发、分页与网关规则见 [Java 后端开发与架构规范](../../docs/Java后端开发与架构规范.md)。安装版使用 `references/Java后端开发与架构规范.md`。
- 组件启用、配置键和模式见 [组件配置手册](../../docs/MODULES-AND-CONFIGURATION.md)。安装版使用 `references/MODULES-AND-CONFIGURATION.md`。
- 环境实际值按本机 `references/deployment.local.md` 中的来源读取。
