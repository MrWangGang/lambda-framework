---
name: lambda-java-backend
description: 创建、开发和审查 Lambda Framework Java 后端。规范分层、通用工具、空值、异常码、Nacos、MongoDB、网关、Fory RPC、消息、向量、锁与验收。
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

真实服务器信息和凭据以本机私有的 `第三方配置.md` 为准，可结合本机安装版的 `references/deployment.local.md` 查阅。公开仓库只放占位值，禁止提交或推送私有配置。

## 2. 配置与模块

- 先按[项目结构](#项目名称包名和-maven-模块)建立独立仓库和 Maven 模块，再按 [Nacos 配置规范](#nacos-配置与-data-id-命名)写入唯一的应用配置文件。
- 示例配置模板默认单机，集群属性给出注释版本。MongoDB 单机也用副本集，Redis 集群使用 DB 0，RabbitMQ 延迟插件在接入节点启用，集群队列用 quorum。ZooKeeper、Kafka 等按连接属性自动启用。未使用时省略连接属性。雪花集群为每个实例分配不同 worker。
- 启动类用 `@SpringBootApplication`，由框架自动装配和检查连接。应用只声明依赖、Nacos 配置和业务组件。部署共享服务时，限制 RSocket 和中间件的网络访问范围。

## 数据库命名

项目启动前确认项目名。MongoDB 的业务 database 统一使用项目名，例如项目 `tazhoo` 使用 `tazhoo`。同一项目下的微服务使用该项目名，MongoDB 系统库由数据库自身维护。项目名应符合目标数据库的命名限制，确认大小写和长度；已有数据库变更名称需要先制定数据迁移方案。

Redis 的逻辑库使用数字编号，单机统一选择 DB 0，集群使用 DB 0。业务 key 使用 `<项目名>:<业务>:<标识>`，例如 `tazhoo:user:123`。Qdrant 的 collection 以项目名开头并追加业务含义，例如 `tazhoo_product_vectors`。只为项目实际用到的数据库配置名称和连接。

## 3. 代码与持久化

- 在 `-core` 组件包下按 `controller`、`service`、`repository`、`po` 和业务需要的 `contract` 细分。持久化类用 `UserPO`，放 `po` 包，使用普通 Java 类和 Lombok。仓储用 `UserRepository`，Service 类直接以 `Impl` 结尾。依赖 Bean 在字段上用 `@Resource`，可选提供者用 `@Autowired`。Service 不使用构造函数注入。
- Repository 只执行数据读写与持久化映射，禁止业务逻辑和校验。Service 负责请求校验、默认值、类型转换、业务规则、权限、事务和响应映射。
- 每个 MongoDB 集合的 PO 继承 `UnifyPO<ObjectId>`。公共字段为 `id`、`createdAt`、`updatedAt` 和 `@Version Long version`。在 PO 声明 `@Document` 和按业务设计的索引。PO 只放持久化字段、映射和业务枚举，接口请求和响应使用独立契约 record。
- 普通保存由框架审计。用 `MongoAuditUpdates.modified` 维护原子更新的时间和版本，用 `insertOnly` 维护幂等插入。编辑匹配客户端版本，库存扣减匹配足额条件。部署新版本前补齐旧集合公共字段。
- MongoDB 请求 ID 自动绑定 ObjectId，Repository 直接传对象，聚合占位符不加引号。多对多用独立关系集合，一对多和多对一按访问模式选择嵌入或引用。金额用字符串。索引跟随实际过滤、排序和唯一约束，深分页用时间加 ObjectId 游标。
- Qdrant Repository 继承 `ReactiveQdrantRepository<T>`，服务通过 Repository 执行响应式检索。前端向量用数值数组。涉及持久化枚举时实现框架 `ConverterEnum`。

## 4. 接口、身份与 RPC

- 前端经 reactive 网关访问。Controller 只用 `@MessageMapping("/api/...")` 定义固定路径，普通 HTTP 统一 POST，使用独立 record 作为参数与结果。Controller 只绑定请求并调用 Service，禁止业务逻辑和校验。使用 Bean Validation 时在 Service 类加 `@Validated`，Service 参数加 `@Valid`，请求 record 声明约束。
- 提供者把 `-facade` 契约模块放在该微服务的代码目录内。实现类加 `@RSocketFacade`，每个实现类对应一个 `*Facade` 接口。调用者只依赖契约 JAR，并用 `@RSocketRpc("服务名")`。框架生成方法路由，拒绝重复路由和重载。RPC 业务与身份元数据固定 Fory。网关内部转发用 JSON。
- `Mono` 单次响应，`Flux` 聚合数组，`UiFlux` 单向 NDJSON，`Flux<I> → BiFlux<O>` 用于 WSS 或 Java 双向 RPC。网关路由按服务前缀配置。
- 登录核验后用 `PrincipalFactory.setPrincipal` 签发会话。网关用 Redis 校验令牌并透传身份。用 `currentUserId()`、`currentUserObjectId()` 或 `currentUser(type)` 从 Reactor Context 获取，业务检查资源权限。内部 RSocket 只开放给可信调用方。

## 5. 交付

**强制部署与验收规则**。禁止在用户的电脑上启动 Java 微服务、网关或其他业务应用。允许在本机编译、打包和检查产物。应用的启动、联调和运行验收必须在用户指定的服务器上通过 Docker 进行。

服务器部署目录固定为 `/root/depoly/<项目名>-<微服务名>/`，例如 `/root/depoly/tazhoo-gateway/` 和 `/root/depoly/tazhoo-order/`；每个微服务使用独立目录。构建后仅向对应目录上传编译好的 `app.jar` 和运行所需的 Docker 部署文件，例如 `Dockerfile`、`compose.yaml` 和 `.dockerignore`。源码、Maven 工程、测试代码和本机私有配置不作为部署产物上传。服务器的环境参数按部署环境提供，测试使用隔离资源，结束后清理测试数据和临时容器。通用用法写入本技能，验收结果记入交付报告。

每个交付项目的根目录 `README.md` 只记录以下清单。

- 微服务只列实际接入的中间件，可注明每项用途。以代码和生效配置为准。
- App 只列实际使用的 UI 组件，可按项目组件、平台内置组件和第三方组件分组。以页面与组件引用为准。

## 6. 空值与异常

| 场景 | 处理 |
|---|---|
| 同步可空值、Map 键、枚举键、JSON 反序列化 | 返回 `Optional<T>`。需要取值时用 `orElseThrow` 或框架的 `OptionalUtil.require` |
| MongoDB、Redis、网关或 Java RPC 的单条必需结果 | 用 `ReactiveValues.require(mono)` 取值，或传入明确的业务异常供应器 |
| 缺失有业务含义 | 用 `ReactiveValues.optional(mono)` 表达已存在或未找到。Service 决定是否创建，例如幂等订单按请求键检查 |
| 列表、分页记录和流 | 无记录用 `List.of()`、`Flux.empty()` 或 `OptionalUtil.listOrEmpty`，HTTP 的集合结果为 `[]` |
| 执行成功且无业务结果 | 方法显式声明 `Mono<Void>`，保留成功完成 |

对可空字段先包装再处理。取必需值使用 `orElseThrow`，禁止直接调用 `Optional.get()` 或还原为 `orElse(null)`。缺少必需值时，默认 `RequiredValueException` 对应 `S0000036`，提示「未找到所需信息」。不吞掉查询、超时和反序列化错误来伪造空结果。Optional 用于 Java 内部读取决策，JSON、Mongo PO 和 Fory 契约使用原有字段类型。契约明确可选的单字段可以缺省。

网关遇到非 Void 的空单次响应时返回 502 与 `S0100005`。Java RPC 调用方遇到非 Void 空响应时抛 `S0300017`。集合和 `Mono<Void>` 保持各自成功语义。

`serviceCode` 固定八位。`00000000` 表示成功，框架错误使用 `S` 加七位数字，业务手动错误使用 `B` 加七位数字。异常码统一使用 `org.lambda.framework.enums.ExceptionEnumFunction` 的 `getCode()` 与 `getMessage()`。

- 框架组件的异常枚举集中在 `lambda-framework-enum` 的 `org.lambda.framework.enums` 包。按组件定义 `*ExceptionEnum`，常量名描述错误原因，例如 `GatewayExceptionEnum.UPSTREAM_UNAVAILABLE`。组件通过 `EventException(枚举值)` 引用错误码。
- 每个业务微服务在自己的代码目录内创建 `<项目名>-<微服务名>-enum/`。Maven artifactId 与目录名一致。模块只放本服务的 `*ExceptionEnum`，依赖 `lambda-framework-enum` 取得错误契约。由本服务的 `-core` 模块引用，业务异常在 Service 使用 `BusinessException(枚举值)` 抛出。实体状态、配置和业务实现仍按各自职责归档。
- 新增前核对已分配码段与重复码。已发布的数字和语义是接口契约，退役编号保留历史含义。提交前核对所有枚举常量的码格式、唯一性和提示长度。

面向用户的提示要具体、自然、简短，最多 24 个 Unicode 码点。说明发生了什么，能给出下一步时给出可执行操作，例如「商品已变更，请刷新后重试」。数据库、协议、类名和堆栈等细节留在服务端日志。

## 7. 框架工具与包职责

写可复用代码前先查表和 JDK。项目特有的规则属于 `*ServiceImpl`。可复用工具先在框架相应模块实现和验证，微服务禁止定义通用工具类或通用工具方法。

| 入口 | 用途 |
|---|---|
| `common.util.OptionalUtil` | 可空值、Map 查找、列表空值 |
| `common.util.ReactiveValues` | 把单值 Mono 转成 Optional 或取必需结果 |
| `common.util.JsonUtil` | 共用 Jackson 编解码，`read` 可空、`readRequired` 强制有值 |
| `common.util.EnumUtil` | 按持久化 key 查 `ConverterEnum`，拒绝重复 key |
| `common.util.IdUtil` | 生成无连字符 UUID |
| `repository.operation.MongoAuditUpdates` | 原子更新审计和幂等插入 |
| `repository.operation.MongoTransactions` | 识别 MongoDB 可重试事务错误 |
| `GuidFactory`、`PrincipalFactory` | 雪花 ID、请求身份与会话 |
| `ReactiveQdrantRepository`、`ReactiveRedisOperation` | 向量集合与 Redis 操作 |

历史工具入口仍在 `common.util.sample`，包括 BeanUtil、JsonUtil、LocalDateUtil、MD5Util、MapGetUtil、NanoIdUtil、ObjectUtil、StringUtil、URLEncoderUtil、UUIDUtil。Base64AlgorithmUtil 与 RSACoderAlgorithmUtil 在 `common.util.algorithm`，树结构工具在 `common.util.tree`。新增调用优先使用上表与 JDK 标准库。历史序列化与摘要工具仅用于已有格式对接。

网关的 `forward` 处理协议转发和路由契约，`error` 负责对外错误外壳。`rpc.proxy` 负责生成 Facade 调用代理。`rsocket.codec` 负责序列化和 Fory 类型发现，`rsocket.error` 负责服务端异常编码。通用工具进入 `common.util`，数据库专用能力保留在数据模块，业务组件依赖公开注解和契约。

## 8. 实体枚举的中文 key

数据库和前端使用同一个中文 key。描述也写简明中文，客户端按 key 读取和提交。Java 常量名只供代码引用。例如商品状态的 key 是「在售」或「下架」，说明是「商品可购买」或「商品已下架」。

升级已有示例库时，暂停旧应用写入并核对 `products` 的历史值，再将 `active` 更新为「在售」，将 `inactive` 更新为「下架」。确认历史值已迁移后再发布服务和客户端。

~~~javascript
db.products.updateMany({status: "active"}, {$set: {status: "在售"}});
db.products.updateMany({status: "inactive"}, {$set: {status: "下架"}});
~~~

# 架构与使用细节

## GitHub 仓库

- 框架源码与使用手册。[lambda-framework](https://github.com/MrWangGang/lambda-framework)
- 可运行的示例与接入说明。[lambda-framework-example](https://github.com/MrWangGang/lambda-framework-example)

## 项目名称、包名和 Maven 模块

新建项目时确认项目名和服务名。一个应用一个 Git 仓库，App、网关、业务服务和 IM 等独立应用各有仓库。仓库名与根目录采用 `<项目名>-<微服务名>`，例如 `tazhoo-server`。统一使用小写连字符命名。Maven 父工程 artifactId、Spring 应用名、服务发现名和 Nacos Data ID 的基名与仓库名一致。每个 Java 仓库根目录各有 Maven 父 POM，直接继承已安装的 Lambda Framework。

Java 源码按 `com.<项目名>.<微服务名>.<组件名>` 分包，源码目录与 `package` 声明一致。启动类位于 `core` 组件包根，运行时 Bean 位于它的子包。RPC 契约放在 `facade` 组件包。异常码模块使用 `enums` 包，`enum` 是 Java 关键字。

子模块使用 `<仓库名>-<模块职责>` 命名，目录名与 Maven artifactId 一致。部署目录沿用此前缀加 `-docker`。以下是 `mall-order` 独立仓库的结构。

~~~text
mall-order/
├── .gitignore
├── pom.xml                             mall-order
├── mall-order-core/                  启动类和服务端实现
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/mall/order/core/
│       └── resources/application.properties
├── mall-order-facade/                  Java RPC 契约
│   ├── pom.xml
│   └── src/main/java/com/mall/order/facade/
├── mall-order-enum/                    本服务异常码枚举
│   ├── pom.xml
│   └── src/main/java/com/mall/order/enums/
└── mall-order-docker/                  容器部署目录
    ├── Dockerfile
    ├── compose.yaml
    └── .dockerignore
~~~

- 每个 Java 应用必建同级的 `-core` 与 `-facade`，包括网关。当前没有 Java RPC 接口时，保留普通 JAR 模块和契约包。业务微服务另建 `-enum`，基础设施网关使用框架网关异常码。新建时提供同级 `-docker` 部署目录。
- 父工程使用 `pom` 打包。它声明 Java 子模块和共用的插件配置。POM 统一 XML 声明、Maven 命名空间和四空格缩进，版本优先由父工程管理。
- `-core` 打包为 Spring Boot 可执行 JAR，按需依赖本服务的 `-enum` 和 `-facade`。契约和异常码模块打包为普通 JAR。跨服务调用方只依赖提供方的 `-facade`。每个仓库维护自己的 `.gitignore`，排除 `target/`、`*.class`、打包产物和本地私有配置。
- 服务端 Facade 实现、业务 `*ServiceImpl` 和实体 `*PO` 放在 `-core`，按职责分包。新建项目时先核对目录、POM 坐标和依赖方向，再写业务代码。

## Docker 部署编译产物

`-docker` 与 `-core` 同级，独立作为镜像构建上下文。提交 Dockerfile、Compose 文件和 `.dockerignore`。构建生成的 `app.jar` 与部署环境文件通过 `.gitignore` 排除。

1. 用项目约定的 JDK 和 Maven 执行 `clean install`。先构建并重打包 `-core` 的 Spring Boot 可执行 JAR，随后在 Maven 的 `install` 阶段把主 JAR 复制到同级 `-docker/app.jar`。父 POM 集中维护复制插件，只有可运行模块启用该执行并指定自己的目标目录。Maven `clean` 清除生成的 `app.jar`。
2. 检查部署目录中的 `app.jar` 与此次构建的可执行 JAR 相同。部署目录只上传编译 JAR 和 Docker 运行文件，禁止上传源码与测试代码。Docker 构建上下文就是该目录，`Dockerfile` 复制目录内的 `app.jar`，使用部署环境指定的 Java 运行时镜像启动。
3. 部署机从私有环境文件提供 `JAVA_RUNTIME_IMAGE`、Nacos 地址和凭据引用。组件、网关和业务配置存放在 Nacos。使用项目目标 JDK 匹配的 Java 运行时镜像，并在部署前核对镜像来源与架构。内部服务的监听和注册地址必须对调用方可达。
4. 进入已上传的 Docker 目录，运行 `docker compose --env-file /实际部署路径/runtime.env up -d --build`。用 `docker compose --env-file /实际部署路径/runtime.env ps` 核对容器状态，查看启动日志并核对 Nacos 服务注册。再通过网关验证业务请求。示例使用 Linux host 网络，按配置中的原生端口监听。
5. 发布时把完整部署目录作为一个版本保存。应用回滚使用上一版部署目录及与之对应的 Nacos 配置。变更数据库结构或共享组件时单独维护迁移顺序。

[示例的 Maven 打包与 Docker 命令](https://github.com/MrWangGang/lambda-framework-example#docker-部署)

## Controller、Service 与 Repository 分层

| 层 | 职责 |
|---|---|
| Controller 与 Facade 实现 | 声明路由、接收已绑定的请求契约，直接调用 Service 并返回结果。Controller 禁止写业务逻辑或校验，方法参数只使用绑定所需的注解 |
| Service | 参数校验、默认值、类型转换、权限、业务规则、事务编排、异常语义和响应映射都在这一层 |
| Repository | 执行数据读写，构造查询、排序、分页及原子更新，返回持久化结果。禁止执行参数校验或业务判断，也不负责返回 HTTP 状态 |

Service 类直接以 `Impl` 结尾，放在 `service` 包。用字段上的 `@Resource` 注入 Bean，需要 `ObjectProvider` 等特殊注入时可用 `@Autowired`。Service 禁止编写注入构造函数，也不用 Lombok 生成注入构造函数。使用 Jakarta Bean Validation 时，在 Service 类声明 `@Validated`，Service 方法参数使用 `@Valid`，契约 record 组件声明约束。领域规则在 Service 中显式校验。

分页由 Service 解析页码、应用默认值并校验页大小，再将已处理的 `Paging` 传给 Repository。库存扣减由 Service 校验数量和解释失败原因，数据库查询仍应保留足额条件，确保扣减原子性。

## MQ 主题契约

发布者在自己的 `-facade` 模块公开消息主题常量和事件契约。Kafka 的 topic 名称由该模块统一定义，发布者和消费者都引用同一常量，业务应用不重复硬编码。事件消息字段和版本也由对外契约维护。

RabbitMQ 的 exchange 与 routing key 分别定义在发布者 Facade。消费队列、消费者组和重试策略由消费服务按职责配置，不与发布主题混为一项。主题名变更按对外契约管理，先核对已有发布者与消费者。

## Facade 契约与 record 命名

- 跨服务共享的请求和响应声明为对外 Facade 接口内的 `record`。仅由网关访问的接口可以在 service 模块定义本地请求、响应 record。入参按动作命名，例如 `FindRequest`、`ChangePasswordRequest`、`PageRequest`。返回按用途命名，例如 `Detail`、`Summary`、`PageItem`。外层 Facade 已表达业务归属，内层名称不重复加表名或实体名，省略 DTO、VO、PO 后缀。
- Controller 与 Facade 实现直接注入 `UserServiceImpl`，复用 Facade 的 record。外部类只导入 `UserFacade`，签名写 `UserFacade.FindRequest` 和 `UserFacade.Detail`。公开契约集中在接口，实体和实现类不声明公开入参、出参。
- 实体留在 service 模块。Controller、Facade 接口和 Service 的公开业务方法使用契约 record，Service 负责从持久化实体映射公开字段。密码哈希和内部权限留在服务端。公开契约按业务需要显式选择可见的审计字段或并发控制字段。
- 对外业务 HTTP 统一经网关访问，使用 POST 动作路径，例如 `/api/user/find`。业务 Controller 使用 `@MessageMapping` 声明网关路径。承接网关转发的路径以 `/` 开头，并使用固定路径。动态业务条件放请求体。请求条件放 JSON 请求体，空请求可用 `EmptyRequest`。RPC 保留 `Mono<UserFacade.Detail>`、`Flux<UserFacade.Detail>` 等完整类型。
- 业务金额和常规业务标量以字符串建模，禁止 BigDecimal。ObjectId、库存计数、版本号、分页计数和日期保留各自的技术类型。CORS 预检、WebSocket 握手和基础设施协议遵循对应协议。

~~~java
package com.mall.user.facade;

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

公开 record 只声明契约需要的字段和约束，由 Service 执行校验。实体枚举实现 `ConverterEnum`，`getValue()` 和 `getDescription()` 用中文。持久化映射注解集中在实体或基类。

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

升级已有项目时，先在部署迁移中回填缺失或为空的公共字段，已有值保留。旧 `createTime` 和 `updateTime` 迁到 `createdAt` 和 `updatedAt`，缺失的版本初始化为 BSON Int64 `0`。在所有旧写入方停写后执行迁移，核对缺失数为零，再启动启用版本的应用。见 [数据访问手册](../../README.md#旧集合迁移)。

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

`Paging` 对业务使用从 1 开始的页码，默认 1/10。框架转换为从 0 开始的 `PageRequest`。`Paged<T>` 复用 page、size、pages、total、records。页码和大小在 Service 校验，业务列表限制每页最多 100。RPC 保留 `Mono<Paged<UserFacade.PageItem>>` 的完整泛型。

`ReactiveUnifyPagingRepositoryOperation.queryPage` 提供 count 与 query 的协调，也可给 Mongo Repository 使用。两者传相同过滤条件，查询应用 Pageable。排序使用可索引的稳定键，例如 `createdAt DESC, _id DESC`。聊天历史等长期增长列表使用复合游标，游标中的 ID 转为 ObjectId。业务需考虑 count 与查询分开执行时的并发更新。

## 第三方 HTTP 请求（强制）

所有微服务调用第三方 HTTP/HTTPS 接口时，必须引入 `lambda-framework-httpclient`，通过该组件封装第三方客户端，并以 Reactor 的 `Mono<T>` 或 `Flux<T>` 向 Service 提供接口。第三方地址、超时和凭据引用放在对应环境的 Nacos 配置中；密钥不写入源码或公开仓库。

当前组件基于 Spring WebFlux，提供 `HttpclientResponseHandler.create(ReponseVerify)` 响应过滤器，并非开箱即用的完整通用客户端。应根据接口响应格式在组件内封装 WebClient 请求、响应映射及异常处理；遇到空响应、流式响应、鉴权或其他现有组件不支持的场景，先在框架的 `lambda-framework-httpclient` 中补齐并验证，再供微服务调用，不在微服务内重复建设通用 HTTP 客户端。

调用链保持非阻塞，不在响应式链路使用 `block()` 或自行 `subscribe()`。按接口特点设置连接及响应超时、连接池和必要的并发限制；结合 HTTP 状态与第三方业务错误映射框架或业务异常。仅对满足幂等条件的调用按明确策略重试；流式数据逐项处理，避免无界聚合。

## 数据和运行配置

应用选择 Maven 模块，启动类使用 `@SpringBootApplication`。连接配置出现时自动创建客户端和检查，生命周期由 Spring 管理。完整属性与示例见 [组件配置手册](../../README.md#组件配置)。

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

应用运行配置在源码中只保留 `-core/src/main/resources/application.properties`。各环境如需分段，在同一个文件内使用 `#---` 和 `spring.config.activate.on-profile`。Nacos 模式只保留引导属性，端口、路由、中间件和业务参数发布到 Nacos。由环境变量提供 Nacos 地址、namespace ID、group 和登录凭据。

- Data ID 固定为 `<项目名称>.properties`，与 `spring.application.name` 一致。例如项目 `order-gateway` 使用 `order-gateway.properties`，项目 `user-service` 使用 `user-service.properties`。同一项目的各运行环境使用各自 namespace，Data ID 保持一致。
- 配置读取和服务发现使用明确的 namespace ID 和 group。Data ID 内容类型选 Properties，先发布再启动应用。用于发现的服务名与网关 `rb://` 和 `ws://` 的目标一致。RSocket 服务登记实际 TCP 端口。
- 在 `-core` 的 `application.properties` 写入以下引导配置，并将第一行替换为已确认的应用名。

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

Nacos 的 Data ID 在配置中心维护。交付时检查仓库内唯一的运行配置位于 `-core/src/main/resources/application.properties`，仓库根目录和契约模块保持无运行配置。

## RSocket、网关与聊天

RSocket 提供端、RPC 调用方和 reactive 网关通过 Maven 依赖与配置自动装配。Spring Boot 启动类使用 `@SpringBootApplication`。使用 Nacos 时配置各项目的应用名、连接信息与发现组。

Java RPC 的提供类使用 `@RSocketFacade`，每个实现类关联一个最具体的 `*Facade` 接口。调用方通过 `@RSocketRpc("order")` 指定目标 Nacos 服务。提供方读取 `spring.application.name`，两端按相同规则生成 `order.orderFacade.find`。公开的是接口抽象方法，默认方法留在客户端本地执行。同一服务禁止路由重名和 Facade 方法重载，冲突时启动失败。

网关的普通 HTTP 和 WSS 在独立 `@Controller` 上声明固定的斜杠 `@MessageMapping` 路径，按服务路由转发。Controller 将绑定后的契约交给 Service。Service 方法执行请求校验。Facade JAR 放接口与契约类型，提供方实现放 service 模块。

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

普通 `Flux` 聚合完成后返回 `{"serviceCode":"00000000","serviceMessage":"操作成功","data":[...]}`。NDJSON 每条业务元素输出一行，放在该行的 `data` 数组中。零条结果的 `data` 为 `[]`。聚合接口与流式接口共用异常转换，失败外壳的 `data` 为 `[]`。流中途失败时追加一行错误，保留已发出的成功行，并结束响应。

### WSS 路由与服务间双向 RPC

客户端连接 `wss://<网关域名>/ws/api/order/exchange`。在网关的 Nacos 配置内按服务加一条路由，`StripPrefix=1` 去掉 `/ws`。业务服务仍使用 RSocket TCP，并通过 `@MessageMapping("/api/order/exchange")` 公开处理方法。TLS 由网关或其前置入口终止。前置代理应转发 WebSocket Upgrade，认证与授权在网关握手时生效。

~~~properties
spring.cloud.gateway.server.webflux.routes[1].id=order-websocket
spring.cloud.gateway.server.webflux.routes[1].uri=ws://order
spring.cloud.gateway.server.webflux.routes[1].predicates[0]=Path=/ws/api/order/**
spring.cloud.gateway.server.webflux.routes[1].filters[0]=StripPrefix=1
~~~

每条 WebSocket 文本消息包含一个 JSON 请求值。每个响应元素独立发送一条 JSON 文本消息，例如 `{"serviceCode":"00000000","serviceMessage":"操作成功","data":[{"sku":"A1"}]}`。业务失败发送错误外壳，`data` 为 `[]`，随后关闭连接。客户端可在同一个连接上先收到回复再发送下一条请求。

服务 A 可以将入站 `Flux<I>` 传给服务 B 的 Facade，由服务 B 的 `BiFlux<O>` 直接返回给 A。B 的实现用 `@RSocketFacade`，A 用 `@RSocketRpc("service-b")` 注入。前端到网关是 WebSocket JSON，网关到 A 是 RSocket JSON，A 到 B 是 RSocket Fory。业务逻辑保留响应式链路，取消随订阅传递。

Java 服务间原生 RPC 固定使用 Apache Fory。提供方依赖 `lambda-framework-rsocket`，调用方依赖 `lambda-framework-rpc`。Spring Boot 自动配置编解码器，框架从已声明的请求、响应类型中自动收集 record、嵌套泛型、枚举和框架分页类型。保持 Facade 契约同版。签名之外的多态子类型用 `ForyRpcTypes` 显式注册。服务端拒绝用 JSON 或二进制连接调用点分隔的原生 RPC 路由。网关 JSON 业务路由使用 `/` 前缀。Java RPC 身份元数据使用 Fory，网关到服务的身份元数据使用 JSON。

网关默认每个后端实例复用一条 RSocket TCP 连接。需要承受单条连接断开的关键路由可在 Nacos 配置 `rsocketConnectionsPerInstance=2`。连接关闭后按最近一次成功发现的目标列表补建连接。请求已发出时由调用方根据业务幂等规则决定如何处理，网关不自动重放。

保持首条消息、单次订阅、上下文、背压和取消。处理首信号时区分数据、错误与完成。遵循 RSocket 和 Spring 的 Payload 所有权，按流转移或释放缓冲区。经过代理时保持全双工、按条转发和取消语义。

私聊与群聊用会话 ID 区分，消息包含发送人、类型、内容、时间。表情用 Unicode，图片与视频的消息内容用对象存储 key。当前 WSS 入口只接收 JSON 文本帧，不支持 WebSocket 二进制消息。媒体文件上传和下载使用独立链路，权限、群成员校验、消息幂等和离线拉取由业务实现。

### uni-app 三端聊天与可靠性

面向微信小程序、Android 和 iOS 的 uni-app 或 uni-app x 聊天，客户端统一通过 `uni.connectSocket` 建立 `wss://` 连接，使用返回的 `SocketTask` 监听连接、消息、错误和关闭事件；不要以全局 Socket 事件管理多个连接。按实际项目区分 uni-app JavaScript/TypeScript 与 uni-app x UTS，核对目标 HBuilderX、微信基础库和插件版本的兼容性。客户端通过网关访问服务，不直连内部 RSocket，也不要求前端实现 RSocket 或 Fory。

客户端与网关之间使用 WSS 和逐条 JSON 文本消息；网关将双向消息转换为 RSocket request/channel，业务 Controller 的 `Flux<I> → BiFlux<O>` 负责接入，Service 负责业务处理。Java 服务之间的 RPC 按框架约定使用 Fory。WebSocket 消息边界由帧表达，不按 NDJSON 换行拆包；应用正常响应和业务异常遵循前述 `serviceCode`、`serviceMessage`、`data` 外壳。

部署时先确认域名解析、有效 TLS 证书、反向代理对 WebSocket Upgrade 的支持、网关及后端路由、握手认证和会话授权。微信小程序需要在公众平台配置对应的 `wss://` socket 合法域名；图片上传、下载所用域名按实际调用方式分别配置。不要在客户端内置第三方密钥、商户私钥或业务服务内部地址。域名和账号的实际值保存在私有《第三方配置.md》，不能因为写入规范就认定已经部署或可用。

现有网关在收到第一条客户端消息后才建立后端 Channel，并默认对首次响应设置 15 秒超时。正式聊天连接应发送定义明确的首条会话建立消息，由服务端及时返回确认；认证信息、会话绑定和后续消息格式应形成前后端共同的接口契约，不能把现有 echo 示例当作完整聊天功能。网关参数按部署环境配置，服务端推送不得要求用户先发送聊天正文。

当前项目的 `/ws/api/im/echo` 是本地 profile 下的回显示例。以下为普通 uni-app JavaScript 用法，域名和认证方式必须替换为实际部署配置；uni-app x 使用 UTS 时按所用版本调整类型。该例用于理解消息格式，正式聊天还须实现前述会话建立和下述可靠性规则。

~~~javascript
const socket = uni.connectSocket({
  url: 'wss://<网关域名>/ws/api/im/echo',
  header: { Authorization: `Bearer ${token}` }
})

socket.onOpen(() => {
  socket.send({ data: JSON.stringify({ text: '你好' }) })
})

socket.onMessage(({ data }) => {
  const result = JSON.parse(data)
  if (result.serviceCode !== '00000000') {
    console.error(result.serviceMessage)
    return
  }
  for (const item of result.data || []) {
    console.log(item.text)
  }
})

socket.onError((error) => console.error('连接错误', error))
socket.onClose(() => console.log('连接关闭'))
~~~

发送一条 `{"text":"你好"}` 时，当前 echo 接口预期以独立 WebSocket 文本消息返回 `{"serviceCode":"00000000","serviceMessage":"操作成功","data":[{"text":"你好"}]}`。是否允许访问和如何携带 token 由实际认证配置决定；示例不包含重连、补拉或离线投递实现。

聊天协议至少定义消息类型、客户端消息 ID、会话 ID、发送人、服务端时间、消息内容及可恢复的会话游标或序号。单聊与群聊均校验会话成员；群聊投递需明确成员变动时点。消息可靠性按以下规则实现：

- 服务端可靠持久化后才确认接收；客户端对发送中、成功和失败提供明确状态，同一客户端消息 ID 重试时服务端幂等去重。
- 断网、切网和前后台切换后按有上限的指数退避重连；重连后根据游标从持久化数据补拉漏消息，并处理重复、乱序和权限变更。
- 单会话明确服务端排序规则，客户端不以本机时间作为唯一顺序依据。跨网关、跨实例按连接位置路由或有可靠性的消息投递方案送达；Redis 在线状态不能替代消息持久化。
- 实现心跳、空闲连接清理、连接状态管理、限流、待发送队列和单消息大小上限；对慢消费者提供有界缓冲和断线补拉。WebSocket 不会自动把 RSocket 背压端到端传给前端，必须定义应用层流控。
- 不假设后台或锁屏期间连接持续在线；结合小程序、Android、iOS 的平台限制和用户授权，配置各自可用的通知能力，恢复前台后仍以服务端消息为准。

图片、语音和视频先通过独立上传链路上传 COS，完成授权、大小与类型校验后，通过聊天消息发送对象 key、类型、尺寸或时长等元数据；接收方按权限获取下载或播放地址。不要把媒体文件整体塞进聊天长连接，也不要把临时签名 URL 当成长期有效的资源标识。

完成接入后，在服务器部署网关与服务，用微信小程序、Android 和 iOS 真机分别验证双向互发、错误处理、断网重连、后台恢复、离线补拉、重复发送、单聊、群聊和媒体上传下载；覆盖消息丢失、重复及顺序等断言。按用户要求清理隔离测试数据，结束后停止本次运行的业务测试应用。以上是开发和验收要求，不代表当前项目已实现完整聊天。
