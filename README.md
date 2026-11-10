# Lambda Framework

Lambda Framework 提供 JDK 27、Spring Boot 4.1.2 和 Spring Cloud 2025.1.4 下的响应式 Java 后端基础能力。使用 Mono 和 Flux 编写业务，在同一体系内接入 MongoDB、Redis、RabbitMQ、Nacos、RSocket 与 HTTP/WSS 网关。

## 构建与启动

框架是父 POM 与 13 个模块的源码工程。应用按功能依赖模块，并单独启动 Spring Boot 进程。先把框架安装到本地 Maven 缓存，再构建 [lambda-framework-example](https://github.com/MrWangGang/lambda-framework-example)。

~~~bash
export JAVA_HOME='/实际的/JDK27/Contents/Home'
./scripts/mvn.sh -version
./scripts/mvn.sh clean install
cd ../lambda-framework-example
cp .env.example .env
./scripts/run.sh
~~~

在执行 run.sh 前填写本机环境变量并准备 Nacos 启动配置。mvn.sh 使用已有 IDEA Maven 或显式 MAVEN_BIN。框架远程 deploy 默认跳过。

~~~xml
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
</dependencies>
~~~

## 模块与接入入口

| 模块后缀 | 主要能力 |
|---|---|
| common | UnifyPO、SecurityLoginUser、ConverterEnum、通用异常、JsonUtil 和 RPC 注解 |
| repository | MongoDB、Redis、R2DBC MySQL、Elasticsearch、分页 |
| compliance | 通用 Service、树操作、PrincipalFactory、SecurityPrincipalHolder |
| security | 认证 Redis、令牌、URL 授权和自定义校验 |
| gateway | HTTP POST 和 WebSocket 到 RSocket、JSON 与 NDJSON |
| rsocket | 编解码策略、SocketAcceptor 拦截、Payload 生命周期、异常编码 |
| rpc | Java Facade 调用方代理与连接复用 |
| loadbalance | RSocketRequester 缓存、连接释放、目标发现 |
| nacos | 配置导入、服务发现与 RSocket 端口注册 |
| mq | RabbitMQ、Kafka 与响应式连接 |
| httpclient | WebClient 响应过滤辅助 |
| guid | 雪花 ID |
| lock | Redisson 和 ZooKeeper 锁 |

artifactId 由 `lambda-framework-` 加模块后缀组成。按应用需要引入配置与组件，例如数据库配置继承 DefaultReactiveMongoRepositoryConfig，Redis 配置继承 DefaultReactiveRedisRepositoryConfig。完整参数见 [模块与配置](docs/MODULES-AND-CONFIGURATION.md)。

## RSocket RPC 接入

假设服务名为 `catalog`，Maven 父工程管理 `catalog-facade` 和 `catalog-service`。服务提供方实现 Facade，调用方引入 Facade，网关只配置服务名和路径前缀。

| 调用方 | 到服务的链路 | 新增接口时改哪里 |
|---|---|---|
| Java 微服务 | Nacos 服务发现 → RSocket TCP → Fory | Facade 与服务实现 |
| HTTP 前端 | 网关 POST → `rb://catalog` → RSocket JSON | 业务服务的 `@MessageMapping` |
| WebSocket 前端 | 网关 WSS → `ws://catalog` → RSocket JSON Channel | 业务服务的 `@MessageMapping` 与双向流签名 |

### 1. 声明共享 Facade

`catalog-facade` 是普通 Java JAR，依赖 `org.lambda.framework:lambda-framework-common` 和 `org.springframework:spring-messaging`。接口放 `com.catalog.facade`，请求和响应 record 声明在接口内。Facade JAR 仅发布契约。调用方引用 JAR 后，接口可作为 `@RSocketRpc` 字段类型使用。

~~~java
package com.catalog.facade;

import org.lambda.framework.common.support.BiFlux;
import org.springframework.messaging.handler.annotation.MessageMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CatalogFacade {
    @MessageMapping({"catalog.catalogFacade.find", "/api/catalog/find"})
    Mono<Detail> find(FindRequest request);

    @MessageMapping({"catalog.catalogFacade.exchange", "/api/catalog/exchange"})
    BiFlux<Detail> exchange(Flux<FindRequest> requests);

    record FindRequest(String sku) {}
    record Detail(String sku, String name) {}
}
~~~

- 点分隔路由用于 Java RPC。首段 `catalog` 必须等于 Nacos 服务名。同一个 Facade 的所有远程方法使用同一服务名。
- 以 `/` 开头的固定路径用于网关。只需要 Java RPC 时，保留点分隔路由即可。只需要网关访问时，可在服务端 Controller 直接声明斜杠路径。
- 一次请求用 `Mono<T>`。单向流可返回 `Flux<T>` 或 `UiFlux<T>`。双向流使用带具体泛型的 `Flux<I>` 入参和 `BiFlux<O>` 返回值。每个 Facade 方法最多接收一个请求参数。

### 2. 配置服务提供方

`catalog-service` 依赖 `catalog-facade`、`lambda-framework-rsocket` 和 `lambda-framework-nacos`，以 Spring Boot 可执行 JAR 启动。实现类留在 `catalog-service`，标记 `@RSocketFacade`。启动类放在包根并使用 Spring Boot 默认的包结构即可。实现类沿用接口上的映射。

~~~java
package com.catalog.facade;

import org.lambda.framework.common.annotation.rsocket.RSocketFacade;
import org.lambda.framework.common.support.BiFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RSocketFacade
public class CatalogFacadeImpl implements CatalogFacade {
    @Override
    public Mono<Detail> find(FindRequest request) {
        return Mono.just(new Detail(request.sku(), "示例商品"));
    }

    @Override
    public BiFlux<Detail> exchange(Flux<FindRequest> requests) {
        return BiFlux.of(requests.concatMap(this::find));
    }
}
~~~

启动类放在包根 `com.catalog`，使用普通的 Spring Boot 启动方式。框架通过 Maven 依赖自动装配。

~~~java
package com.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CatalogApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatalogApplication.class, args);
    }
}
~~~

在服务提供方的本地 `application.properties` 中配置 Nacos 引导项。环境变量提供 Nacos 的实际地址、namespace ID、group 和凭据。

~~~properties
spring.application.name=catalog
spring.main.web-application-type=none
spring.main.keep-alive=true
spring.config.import=nacos:${spring.application.name}.properties?group=${NACOS_GROUP}&refreshEnabled=true
spring.cloud.nacos.config.server-addr=${NACOS_SERVER_ADDR}
spring.cloud.nacos.config.namespace=${NACOS_NAMESPACE}
spring.cloud.nacos.discovery.server-addr=${NACOS_SERVER_ADDR}
spring.cloud.nacos.discovery.namespace=${NACOS_NAMESPACE}
spring.cloud.nacos.discovery.group=${NACOS_GROUP}
spring.cloud.nacos.username=${NACOS_USERNAME}
spring.cloud.nacos.password=${NACOS_PASSWORD}
~~~

在同一 namespace 和 group 发布 `catalog.properties`，内容格式选 Properties。监听地址是服务本机地址，注册地址必须能被网关和 Java 调用方访问。

~~~properties
spring.rsocket.server.transport=tcp
spring.rsocket.server.address=0.0.0.0
spring.rsocket.server.port=7001
spring.cloud.nacos.discovery.ip=${CATALOG_REGISTER_IP}
~~~

配置 `spring.rsocket.server.port` 后，框架自动启用 TCP 服务端、协议拦截和网关契约发布。Nacos 服务注册开启时，框架在监听成功后登记实际 TCP 端口。部署多实例时保持应用名一致，每个实例填自己的内网地址和端口。

### 3. 配置 Java 调用方

调用方添加提供方同版 `catalog-facade` 依赖，并引入 `lambda-framework-rpc` 和 `lambda-framework-nacos`。RPC 模块会带上 Fory 编解码和连接池。同一 Maven 工程内直接依赖 Facade 模块。跨工程开发时先把 Facade JAR 安装到本机 Maven 仓库，并让调用方引用相同的 groupId、artifactId 和 version。调用方使用自己的 `spring.application.name` 和 Nacos Data ID，发现配置指向提供方所在的 namespace 和 group。若调用方自己也提供 RSocket 服务，继续保留第 2 步的服务端配置。

调用端启动时自动注册 RPC 代理。默认从 Reactor Context 获取 `SecurityStash` 并随 RPC 传递身份。业务提供自己的 `PrincipalStash` Bean 时，框架自动使用该 Bean。只调用其他服务的应用无需设置 RSocket 服务端端口。

以下代码放在调用方的 Spring Bean 中。`@RSocketRpc` 位于 `org.lambda.framework.common.annotation.rsocket` 包。

~~~java
@RSocketRpc
private CatalogFacade catalog;

public Mono<CatalogFacade.Detail> find(String sku) {
    return catalog.find(new CatalogFacade.FindRequest(sku));
}

public BiFlux<CatalogFacade.Detail> exchange(Flux<CatalogFacade.FindRequest> requests) {
    return catalog.exchange(requests);
}
~~~

框架读取接口上的服务名和路由，选择 Fory、发现实例并复用 TCP 连接。应用直接消费 record 或业务流，错误通过 Reactor 的错误信号传递。双向流的输入可以继续来自上游，收到响应后仍可发送下一项。

## 定义普通 HTTP 接口

在业务服务中写 `@Controller` 和带斜杠路径的 `@MessageMapping`。只给前端使用的接口可以把请求和响应 record 留在 service 模块。网关已有 `/api/catalog/**` 前缀时，客户端直接 POST 到新路径。

~~~java
package com.catalog.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
public class CatalogStatusController {
    @MessageMapping("/api/catalog/status")
    public Mono<Status> status(StatusRequest request) {
        return Mono.just(new Status(request.sku(), "active"));
    }

    public record StatusRequest(String sku) {}
    public record Status(String sku, String state) {}
}
~~~

客户端执行 `POST /api/catalog/status`，请求体为 `{"sku":"A01"}`。网关自动识别 `Mono` 并返回单个 JSON 对象。

~~~json
{"serviceCode":"E00000000","serviceMessage":"操作成功","data":{"sku":"A01","state":"active"}}
~~~

## 网关按服务配置路由

网关依赖 `lambda-framework-gateway` 和 `lambda-framework-nacos`。使用 `@SpringBootApplication`，设为 reactive 后自动启用 HTTP/WSS 转发、异常处理和连接池。网关和业务服务使用同一 Nacos 发现配置。

在网关项目的 Nacos Data ID 中按**服务前缀**配置。下面假设项目叫 `catalog-gateway`，Data ID 就是 `catalog-gateway.properties`。

~~~properties
spring.main.web-application-type=reactive
server.port=8080
spring.cloud.nacos.discovery.register-enabled=false
spring.cloud.gateway.server.webflux.routes[0].id=catalog-http
spring.cloud.gateway.server.webflux.routes[0].uri=rb://catalog
spring.cloud.gateway.server.webflux.routes[0].predicates[0]=Path=/api/catalog/**
spring.cloud.gateway.server.webflux.routes[1].id=catalog-ws
spring.cloud.gateway.server.webflux.routes[1].uri=ws://catalog
spring.cloud.gateway.server.webflux.routes[1].predicates[0]=Path=/ws/api/catalog/**
spring.cloud.gateway.server.webflux.routes[1].filters[0]=StripPrefix=1
~~~

- HTTP 客户端 `POST /api/catalog/find`，网关按 `/api/catalog/**` 转发到 `catalog`，服务端匹配 `@MessageMapping("/api/catalog/find")`。
- WSS 客户端连接 `wss://<网关域名>/ws/api/catalog/exchange`。网关去掉第一个路径段 `/ws`，服务端匹配 `@MessageMapping("/api/catalog/exchange")`。TLS 由网关或前置入口终止，前置代理转发 WebSocket Upgrade。
- 在 `catalog` 新增 `/api/catalog/list` 时，只加业务方法，现有 HTTP 路由直接覆盖。新增另一个 `Flux<I> → BiFlux<O>` 接口时，现有 WSS 前缀同样覆盖。引入新后端服务或新公开前缀时再增加网关路由。纯 Java RPC 接口无需配置网关。
- 路径前缀只选择服务。网关仍用最终路径核对服务端公开的固定映射，未知路径返回 404，契约暂不可用返回 503。成功契约按服务缓存 30 秒，新接口随契约刷新出现。WSS 只接受双向流签名。

~~~bash
curl -fsS --json '{"sku":"A01"}' http://127.0.0.1:8080/api/catalog/find
~~~

~~~json
{"serviceCode":"E00000000","serviceMessage":"操作成功","data":{"sku":"A01","name":"示例商品"}}
~~~

WSS 每条文本消息发送一个 JSON 请求值，例如 `{"sku":"A01"}`。每条回执独立送出，`data` 是数组。

~~~json
{"serviceCode":"E00000000","serviceMessage":"操作成功","data":[{"sku":"A01","name":"示例商品"}]}
~~~

业务错误先以 `serviceCode`、`serviceMessage`、`data:[]` 发送终止消息，然后关闭 WSS 连接。HTTP 和 WSS 外壳由网关生成。

### 自动选择请求与响应类型

以下识别规则用于 `rb://` 路由。业务 HTTP 客户端访问网关地址，业务服务使用 `@MessageMapping` 提供内网映射。

| 已发现的服务端签名 | 网关选择 |
|---|---|
| 单条请求，返回 `Mono<T>` | request/response，返回 JSON |
| 单条请求，返回 `Flux<T>` | 收齐后返回单个 JSON 对象，data 为数组 |
| 单条请求，返回 `UiFlux<T>` | 单向 NDJSON，每行 data 为数组 |
| `Flux<I>` 入参，返回 `BiFlux<O>` | 双向 NDJSON，可发 JSON 数组元素或 NDJSON 行 |
| 单条请求，返回 `BiFlux<T>` | 单向 NDJSON，按 request/stream 执行 |

网关缓存服务端契约。已发现的 JSON 路由以签名决定模式与响应格式，客户端发送请求体即可。普通请求缺少 Content-Type 时按 JSON。Mono 响应的 `data` 保留实际类型，record 是对象，`Mono<String>` 是字符串，`Mono<Integer>` 是数字，`Mono<Void>` 是空数据。普通 Flux 聚合，UiFlux 和 BiFlux 逐行发送。数组接口失败时 `data` 为 `[]`，流式失败作为最后一行返回业务错误码。

| 路由键 | 用途 |
|---|---|
| rsocketConnectionsPerInstance | 1–8，默认 1。每个后端实例复用的连接数 |
| rsocketStreamConnectionsPerInstance | 1–8，单向流专用连接数，配置时优先于通用值 |
| rsocketContentType | 仅在需要固定输入格式时声明，例如二进制路由使用 application/octet-stream |

业务请求错误沿用 serviceCode/serviceMessage，网关参数、方法、内容类型与超限错误使用相应 HTTP 状态。

| 配置 | 默认值与范围 |
|---|---|
| lambda.gateway.rsocket.max-request-bytes | 1048576 字节，普通请求体、每条 NDJSON 或单条 WSS 消息的业务上限 |
| lambda.gateway.rsocket.max-response-bytes | 4194304 字节，聚合响应或单条流响应 |
| lambda.gateway.rsocket.max-response-items | 512，聚合 JSON 数组的元素数 |
| lambda.gateway.rsocket.request-timeout | 15s，普通请求总等待或发送请求后的流首条响应等待 |
| lambda.rsocket.server.max-inbound-payload-bytes | 16777216，服务端 Payload 重组上限 |
| lambda.rsocket.client.max-inbound-payload-bytes | 16777216，框架客户端 Payload 重组上限 |

请求体读取一次。传统数组先累计条目和字节数，再按最终长度分配响应缓冲区。NDJSON 边接收边写出。连接按目标、数据 MIME 和连接数缓存，后台刷新服务实例，关闭应用时释放。身份元数据条目独立使用 JSON。

关键路由可在 Nacos 的路由 metadata 设置 `rsocketConnectionsPerInstance=2`。连接数用于同一个后端实例的池内复用。RSocket 池摘除断链后，框架用最近发现的目标列表补建，独立于 30 秒的服务发现刷新。网关将识别到的上游断链或连接不可用映射为 HTTP 503，已发送请求不自动重放。相同目标、数据 MIME 和连接数的路由共享连接池。

## 用户、分页、配置和维护

- [用户体系](docs/USER-AND-SECURITY.md) 涵盖用户超类、会话、授权 Redis、ACL 缺口与扩展点。
- [数据与分页](docs/DATA-AND-PAGINATION.md) 涵盖 Repository 接口、排序、分页和复合游标。
- [RSocket 与线程](docs/RSOCKET-AND-VIRTUAL-THREADS.md) 涵盖背压、握手、负载均衡和虚拟线程。
- [安装](docs/GETTING-STARTED.md)、[配置](docs/MODULES-AND-CONFIGURATION.md)、[维护](docs/OPERATIONS.md) 和 [依赖](docs/DEPENDENCIES.md) 分别提供完整操作。

## Java 后端开发与架构规范

版本日期为 **2026-11-10**。本规范供开发者和智能体统一 Java 项目的结构、接口、数据模型和运行配置。部署地址与凭据由部署环境提供。SSH 私钥由本机保管。仓库中的示例使用本机地址和环境变量。

### GitHub 仓库

- 框架源码与使用手册。[lambda-framework](https://github.com/MrWangGang/lambda-framework)
- 可运行的示例与接入说明。[lambda-framework-example](https://github.com/MrWangGang/lambda-framework-example)

### 项目名称、包名和 Maven 模块

新建业务微服务前确认项目名称，缺少名称时询问。父工程 artifactId、Spring 应用名、服务发现名和 Nacos Data ID 的文件名使用项目名称。Java 包根使用 `com.<项目名>`，在其下按职责分包。包名用小写 Java 标识符，项目名中的连字符对应包层级，例如 `order-center` 对应 `com.order.center`。

业务微服务使用一个 Maven 父工程和两个子模块。以下用 `user` 举例。

~~~text
user/
├── pom.xml
├── user-service/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/user/
│       │   ├── UserApplication.java
│       │   ├── controller/UserController.java
│       │   ├── service/UserServiceImpl.java
│       │   ├── facade/UserFacadeImpl.java
│       │   ├── entity/UserEntity.java
│       │   └── repository/UserRepository.java
│       └── resources/application.properties
└── user-facade/
    ├── pom.xml
    └── src/main/java/com/user/
        └── facade/UserFacade.java
~~~

- 父工程使用 `pom` 打包，管理模块与版本。`<项目名>-service` 是可运行模块，依赖 `<项目名>-facade`。其他微服务通过 `<项目名>-facade` 引入 RPC 契约。
- `<项目名>-facade` 是契约 JAR，`facade` 包内放对外接口和接口内的入参、出参 `record`。该模块只引入编译和使用契约所需的依赖。
- 服务端的 `UserFacadeImpl` 放在 service 模块的 `facade` 包。业务实现直接命名为 `UserServiceImpl`，放在 `service` 包根，省略 `UserService` 接口和 `service.impl` 子包。
- 每个数据库表或集合对应一个持久化实体。`UserEntity` 映射到 `user`，由 `UserRepository` 访问。枚举按业务归属放在实体或 `enums` 包，连接配置放在 `config` 包。

### Facade 契约与 record 命名

- 跨服务共享的请求和响应声明为对外 Facade 接口内的 `record`。仅由网关访问的接口可以在 service 模块定义本地请求、响应 record。入参按动作命名，例如 `FindRequest`、`ChangePasswordRequest`、`PageRequest`。返回按用途命名，例如 `Detail`、`Summary`、`PageItem`。外层 Facade 已表达业务归属，内层名称不重复加表名或实体名，省略 DTO、VO、PO 后缀。
- Controller 与 Facade 实现直接注入 `UserServiceImpl`，复用 Facade 的 record。外部类只导入 `UserFacade`，签名写 `UserFacade.FindRequest` 和 `UserFacade.Detail`。公开契约集中在接口，实体和实现类不声明公开入参、出参。
- 实体留在 service 模块。Controller、Facade 接口和 Service 的公开业务方法使用契约 record，Service 负责从持久化实体映射公开字段。密码哈希和内部权限留在服务端。公开契约按业务需要显式选择可见的审计字段或并发控制字段。
- 对外业务 HTTP 统一经网关访问，使用 POST 动作路径，例如 `/api/user/find`。业务服务的路由统一使用 `@MessageMapping`。承接网关转发的路径以 `/` 开头，并使用固定路径。动态业务条件放请求体。请求条件放 JSON 请求体，空请求可用 `EmptyRequest`。RPC 保留 `Mono<UserFacade.Detail>`、`Flux<UserFacade.Detail>` 等完整类型。
- 业务金额和常规业务标量以字符串建模，禁止 BigDecimal。ObjectId、库存计数、版本号、分页计数和日期保留各自的技术类型。CORS 预检、WebSocket 握手和基础设施协议遵循对应协议。

~~~java
package com.user.facade;

import jakarta.validation.constraints.NotNull;
import org.bson.types.ObjectId;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.lambda.framework.common.jackson.JsonObjectId;
import reactor.core.publisher.Mono;

public interface UserFacade {
    @MessageMapping({"user.userFacade.find", "/api/user/find"})
    Mono<Detail> find(FindRequest request);

    record FindRequest(@JsonObjectId @NotNull ObjectId id) {}
    record Detail(@JsonObjectId ObjectId id, String name) {}
}
~~~

`UserFacadeImpl` 标记 `@RSocketFacade` 并实现 `find`，Spring 继承 Facade 接口的 `@MessageMapping`。共享 Facade 接口本身保持为 Java 契约。同一方法处理 Java RPC 的 `user.userFacade.find` 和网关的 `/api/user/find`，内部调用 `UserServiceImpl.find(UserFacade.FindRequest)`。Facade 模块使用 `spring-messaging` 提供的注解。

### 实体注解与主键

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

HTTP 和 RPC 入参的校验注解放在请求 record 组件上。公开 record 只声明契约需要的字段，持久化映射注解集中在实体或基类。

### 关系建模与读写取舍

设计前记录关键接口的读取频率、更新频率、常见筛选和排序、子项数量上限、独立更新或分页需求，以及一致性要求。评估一次读取需要访问多少文档，以及一次更新是否会触发多份副本更新。表结构按这些访问模式设计，索引跟随实际查询。

| 关系 | 结构要求 | 读多时 | 更新多或持续增长时 |
|---|---|---|---|
| 多对多 | 必须建中间表，MongoDB 建独立关系集合。两端 ID 均用 ObjectId，关系实体也有自己的 `@Id` | 收藏列表从 `favorites` 按 `userId` 分页，再按当前页关联商品。商品反查用户时，按反查条件增加索引 | 新增或取消一条收藏只写一条关系记录。用 `(userId, productId)` 唯一约束，租户场景把租户键纳入唯一范围 |
| 一对多 | 直接内嵌或在子表保存父 ID，无需中间表 | 用户与少量收货地址可内嵌。前提是经常一起读取、地址有业务上限且随用户管理 | 会话与聊天消息拆集合，消息保存 `conversationId`，按时间和 `_id` 游标翻页。高频写消息不改同一份会话文档 |
| 多对一 | 多端持有一端 ID，无需中间表 | 商品列表常读店铺名称时，可保留少量展示快照，写清可接受的延迟和刷新方式。需要实时店铺信息时批量查询或用 `$lookup` | 商品持有 `merchantId`，商家资料保存在商家集合。商家频繁变更时集中更新，避免改名触发所有商品重写 |

内嵌是存储决策，`$lookup` 是查询决策。内嵌适用于有界、一起读写且生命周期一致的数据。子项独立分页、授权、更新或增长无上限时用引用。使用引用时，先判断页面是否需要关联数据，再选择批量查询或聚合。尽早按索引过滤。如果关联结果参与过滤、排序或计数，应在对应步骤完成后再分页，保持结果语义。避免逐条查询形成 N+1。上线前确认常用访问路径、索引、关联基数及删除后的孤儿处理。历史订单里的成交快照保留成交时值。[MongoDB 内嵌模型](https://www.mongodb.com/docs/manual/data-modeling/embedding/) · [MongoDB 引用模型](https://www.mongodb.com/docs/manual/data-modeling/referencing/)

### 按业务设计索引

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

### MongoDB 类型、枚举与查询

MongoDB 主键和关联字段用 BSON `ObjectId`，Repository 泛型和查询参数也使用 `ObjectId`。网关 HTTP JSON 中的 ID 使用 24 位十六进制字符串。原生 Java RPC 用 Fory 直接传输 ObjectId。公开 record 的 Mongo ID 组件声明为 `@JsonObjectId ObjectId`，Jackson 2 和 3 在读入时校验并转换，写出时输出小写十六进制字符串。Service 直接把原生 ID 交给 Repository。

~~~java
import org.lambda.framework.common.jackson.JsonObjectId;

// 定义在所属 Facade 接口内
record FindRequest(@JsonObjectId @NotNull ObjectId id) {}
~~~

`@JsonObjectId` 只作用于标注的单值字段。必填 ID 配合 `@NotNull` 和入口 Bean Validation，游标等可选 ID 允许为空。字符串业务键继续用 `String`。既有字符串入参若暂不迁移，在调用 Repository 前校验并转换一次。

~~~java
public interface ChatMessageRepository extends ReactiveMongoRepository<ChatMessageEntity, ObjectId> {
    @Query("{ '_id': ?0 }")
    Mono<ChatMessageEntity> findWithQuery(ObjectId id);
}
~~~

使用 `findById(ObjectId)` 和带 ObjectId 参数的 `@Query`。使用原生驱动时，`Filters.eq("_id", id)` 的 `id` 传 ObjectId。MongoDB 的 `@Query` 使用 BSON 查询表达式。原生驱动、Document 和聚合匹配应明确 BSON 类型，关联字段也需一致。历史字符串 ID、DECIMAL128 金额和旧索引应经过迁移后再由新实体读取。

数据库实体包含枚举字段时，枚举实现 `org.lambda.framework.common.enums.ConverterEnum`，提供 `getValue()` 与 `getDescription()`。持久化值保持稳定，例如 `text`、`emoji`、`image`、`video`。框架的 Mongo 配置注册枚举读写转换器。

### `@Aggregation` 中的 ObjectId

供网关 JSON 使用的请求 record 用 `@JsonObjectId ObjectId` 自动接收十六进制字符串。Java RPC 的 Fory 编解码直接处理 ObjectId。业务层把原生 ID 直接交给 Repository。Repository 的 `@Aggregation` 用未加引号的 `?0`、`?1` 绑定原生参数，批量条件用 `List<ObjectId>`。已是 ObjectId 的值直接传入。各集合关联字段的 BSON 类型必须一致。顶层字段会按 Repository 实体映射，例如 `userId` 映射为 `user_id`。跨集合阶段按目标集合的实际 BSON 字段名编写。

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

### 乐观锁、SKU 库存和金额原子更新

`@Version` 按业务选用。商品资料等先读取、后提交且可能被多人编辑的数据，使用 `@Version Long version`，更新时检查客户端看到的版本。Spring Data MongoDB 的版本化 `save` 会校验旧版本并递增，冲突抛出 `OptimisticLockingFailureException`。追加写的审计日志、由唯一键保护的关系记录、只走条件原子更新的计数文档通常无需加。使用自定义更新、原生驱动或批量更新时，显式核对更新条件、版本推进和写入结果。版本字段已有框架自动递增时，手写更新也只能递增一次。乐观锁要求服务器确认写入。所有可能覆盖同一业务字段的写入口须遵守同一并发约定。[Spring Data MongoDB 乐观锁](https://docs.spring.io/spring-data/data-mongo/reference/mongodb/template-crud-operations.html#mongo-template.optimistic-locking)

### SKU 防超卖

在写入前校验购买数量为正整数。HTTP/RPC 的购买请求 record 可以接收字符串，Service 校验并转换，库存持久化为整数计数。同一条 MongoDB 更新检查 SKU 主键、可售状态、适用的租户条件和 `available >= quantity`，并用 `$inc` 扣减。根据 `matchedCount` 或 `findAndModify` 是否返回文档判定结果。管理端普通资料编辑只改资料字段，补货和库存调整走单独的原子操作。

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

### 用户与会话

业务用户实体可继承 `UnifyPO<ObjectId>` 并实现 `SecurityLoginUser<ObjectId>`，复用 id、createTime、updateTime。登录快照可用 record 实现 `SecurityLoginUser<String>`，ID 使用十六进制字符串。登录接口对外返回的 record 定义在相应 Facade 接口中。快照保留昵称、状态和必要的权限版本，避免包含密码哈希、验证码和敏感业务字段。

`PrincipalFactory` 是会话抽象，`SecurityPrincipalHolder` 从 Reactor Context 读取当前用户。登录后创建会话，从可信入口验证令牌，并在 Service 按实际身份判断资源权限。认证与授权 Redis 使用独立配置前缀，核对所用 ACL 模式及框架模块能力。应用负责覆盖实际身份与权限规则。

### 分页和索引

`Paging` 对业务使用从 1 开始的页码，默认 1/10。框架转换为从 0 开始的 `PageRequest`。`Paged<T>` 复用 page、size、pages、total、records。页码和大小在入口校验，业务列表限制每页最多 100。RPC 保留 `Mono<Paged<UserFacade.PageItem>>` 的完整泛型。

`ReactiveUnifyPagingRepositoryOperation.sqlPaging` 提供 count 与 query 的协调，也可给 Mongo Repository 使用。两者传相同过滤条件，查询应用 Pageable。排序使用可索引的稳定键，例如 `createdAt DESC, _id DESC`。聊天历史等长期增长列表使用复合游标，游标中的 ID 转为 ObjectId。业务需考虑 count 与查询分开执行时的并发更新。

### 数据库、缓存和运行配置

使用现有 JDK 27 与 IDEA 自带 Maven。框架先执行 `./scripts/mvn.sh install`，应用随后构建。网关运行模式固定 `spring.main.web-application-type=reactive`。业务服务设置 `spring.main.web-application-type=none` 和 `spring.rsocket.server.transport=tcp`。引入 `lambda-framework-rsocket` 后，配置监听端口即启用 RSocket 服务端及框架拦截器。框架在启动时拒绝业务服务的 HTTP 模式和 WebSocket 传输。只向外部开放网关的 HTTPS/WSS 入口，业务 RSocket TCP 端口限网关与获准调用的 Java 服务访问。

| 功能 | 用法 |
|---|---|
| MongoDB | repository 模块继承 DefaultReactiveMongoRepositoryConfig，使用 lambda.repository.mongo.host/user/password/auth-database/database/deploy/replica。副本集事务配置 cluster 与真实 replica 名。 |
| Redis | repository 模块继承 DefaultReactiveRedisRepositoryConfig，使用 lambda.repository.redis.host/username/password/database/deploy。复用连接，以业务前缀与 TTL 管理键。 |
| RabbitMQ | mq 模块继承 DefaultReactiveRabbitMQConfig。延迟交换机需要插件，处理结果落库后再确认消费。 |
| Nacos | 配置 namespace、group、服务名与注册端口。区分 HTTP 端口与 RSocket 端口。 |
| Qdrant | 示例通过 WebClient 访问，按集合定义维度、距离函数、ID 与 payload。 |
| 对象存储与短信 | 通过部署环境注入凭据，接口按云服务文档配置。 |

### Nacos 配置与 Data ID 命名

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

### RSocket、网关与聊天

RSocket 提供端、RPC 调用方和 reactive 网关通过 Maven 依赖与配置自动装配。Spring Boot 启动类使用 `@SpringBootApplication`。使用 Nacos 时配置各项目的应用名、连接信息与发现组。

Java RPC 的 Facade 方法写完整的 `@MessageMapping`，例如 `order.orderFacade.find`。首段 `order` 是服务发现名，同一个 Facade 的方法使用同一服务名。调用方通过 `@RSocketRpc` 注入 Facade。需要同时提供网关入口时，在同一个注解中加上以 `/` 开头的固定路径，由网关的 `rb://<服务名>` 或 `ws://<服务名>` 路由转发。仅经网关访问的方法放在 `@Controller` 中，直接声明斜杠路径。使用默认的 Spring Boot 包结构。Facade JAR 只放接口和契约类型，提供方实现留在 service 模块。

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

服务 A 可以将入站 `Flux<I>` 传给服务 B 的 Facade，由服务 B 的 `BiFlux<O>` 直接返回给 A。B 的 Facade 使用点分隔的 `@MessageMapping`，A 用 `@RSocketRpc` 注入。前端到网关是 WebSocket JSON，网关到 A 是 RSocket JSON，A 到 B 是 RSocket Fory。业务逻辑保留响应式链路，取消随订阅传递。

Java 服务间原生 RPC 固定使用 Apache Fory。提供方依赖 `lambda-framework-rsocket`，调用方依赖 `lambda-framework-rpc`。Spring Boot 自动配置编解码器，框架从已声明的请求、响应类型中自动收集 record、嵌套泛型、枚举和框架分页类型。保持 Facade 契约同版。签名之外的多态子类型用 `ForyRpcTypes` 显式注册。服务端拒绝用 JSON 或二进制连接调用点分隔的原生 RPC 路由。网关 JSON 业务路由使用 `/` 前缀。身份元数据沿用 JSON。

网关默认每个后端实例复用一条 RSocket TCP 连接。需要承受单条连接断开的关键路由可在 Nacos 配置 `rsocketConnectionsPerInstance=2`。连接关闭后按最近一次成功发现的目标列表补建连接。请求已发出时由调用方根据业务幂等规则决定如何处理，网关不自动重放。

保持首条消息、单次订阅、上下文、背压和取消。处理首信号时区分数据、错误与完成。遵循 RSocket 和 Spring 的 Payload 所有权，按流转移或释放缓冲区。经过代理时保持全双工、按条转发和取消语义。

私聊与群聊用会话 ID 区分，消息包含发送人、类型、内容、时间。表情用 Unicode，图片与视频的消息内容用对象存储 key。网关可按字节转发小二进制。媒体文件上传和下载使用独立链路，权限、群成员校验、消息幂等和离线拉取由业务实现。

## 开发规范技能

~~~markdown
---
name: lambda-java-backend
description: 按统一风格开发、修改或审查 Lambda Framework Java 后端项目。适用于项目命名、POST 接口、Facade 契约、RSocket RPC、MongoDB、分页和用户体系。
---

新建项目或微服务前确认项目名称。用户已提供时直接使用。缺少名称时询问，等待期间可以检查现有代码和准备设计。用已确认的名称派生 Maven artifactId、`com.<项目名>` 包根、Spring 应用名、服务发现名和 Nacos Data ID `<项目名称>.properties`。

仓库版规范位于 `../../docs/Java后端开发与架构规范.md`，本机安装版随技能提供 `references/Java后端开发与架构规范.md`。环境值由部署环境或本机环境变量提供。提交前检查真实凭据是否混入 Git。

- 业务微服务拆为 `<项目名>-service` 和 `<项目名>-facade`。service 依赖 facade，RPC 调用方只依赖 facade。
- 对外接口放 facade 模块的 `facade` 包，服务端实现放 service 模块的 `facade` 包。业务类 `UserServiceImpl` 直接放 `service` 包，省略业务接口和 `impl` 子包。
- 对外 HTTP 一律经网关转发，使用 POST 动作路径。WSS 也由网关接入。业务服务只监听内网 RSocket TCP。网关只转发已发布的固定 @MessageMapping 路径。Controller、Facade 实现与 Service 共用 Facade 接口内的请求、响应 record。签名保留 `UserFacade.FindRequest`、`UserFacade.Detail` 等外层归属，内层名称按业务动作或用途命名。实体使用 `UserEntity` 等名称，仅在 service 模块持久化使用。
- 应用运行配置放 Nacos，本地只留项目名、导入声明和 Nacos 引导配置。网关使用 reactive，HTTP 使用 `rb://服务名`，WSS 使用 `ws://服务名`。网关到业务服务使用 RSocket JSON。业务进程用 none 和 TCP。RSocket 提供方、RPC 调用方和网关按 Maven 依赖及应用配置自动装配。共享 Facade 方法只写 @MessageMapping 和签名，`@RSocketFacade` 只标在服务提供方实现类。普通网关接口使用 `@Controller` 和斜杠路径。原生 Java RPC 自动使用 Fory，从同版 Facade 契约收集 record、枚举和框架分页类型。
- Mongo 主键与关联 ID 使用 ObjectId。HTTP JSON 用十六进制字符串，公开 record 的单值 ID 用 @JsonObjectId ObjectId，Fory 直接处理 ObjectId，Repository 直接绑定原生 ID。枚举实现 ConverterEnum。业务金额与标量用字符串，技术字段按规范转换。
- 每个持久化实体明确表或集合名及唯一的 @Id，可复用基类标识。按 Repository 的筛选、排序、唯一约束或生命周期需求加索引，说明用途并核对实际索引和执行计划。
- 按业务读写比例、访问路径和增长设计关系。多对多必须有中间表或关系集合，一对多和多对一直接内嵌或持有引用。内嵌需有业务上限，独立分页和高频更新用引用。聚合用未加引号的占位符绑定 ObjectId，按目标集合 BSON 字段名编写关联条件。
- @Version 按需使用。SKU 以库存足额条件加原子扣减防超卖。字符串金额用旧金额和版本做条件更新，跨文档一致性用事务与幂等键。
- 复用框架分页、异常、Reactor Context 和数据库连接。区分 SecurityLoginUser、UnifyPO、PrincipalFactory。
- RSocket Channel 保持首消息、单次订阅、背压、取消和 Payload 所有权。网关 JSON 聚合有上限，NDJSON 逐条输出。 `rb://` 根据返回类型决定响应。Mono 返回单次 JSON，Flux 聚合为 JSON 数组，UiFlux 单向 NDJSON，BiFlux 配合普通 Flux 入参走双向 NDJSON。流式成功行的 data 是数组，失败行的 data 是空数组。WSS 只开放 `Flux<I> → BiFlux<O>` 路由。每条 WebSocket 文本消息承载一个 JSON 值。聊天 record 描述文字、表情与媒体引用。
- 使用现有 JDK 和 IDEA Maven。README 记录启动方式、接口契约与配置。
~~~
