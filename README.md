# Lambda Framework

面向 JDK 27 和 Spring Boot 4 的响应式 Java 后端框架。业务服务用 MongoDB、Redis、Qdrant 与消息队列。前端走 reactive 网关，Java 服务间走 RSocket Fory。

## 开始使用

1. 使用现有 JDK 27 与 IntelliJ IDEA Maven。运行 `./scripts/mvn.sh clean install` 将框架安装到本机 Maven。
2. 新应用继承 `org.lambda.framework:lambda-framework:1.0.0-SNAPSHOT`，按需引入模块，启动类使用 `@SpringBootApplication`。
3. 提供启用组件的连接配置。框架自动绑定属性、管理客户端生命周期并检查连通性。

| 需求 | Maven 模块 |
|---|---|
| MongoDB、Redis、事务、分页 | `lambda-framework-repository` |
| Qdrant gRPC，`ReactiveQdrantRepository<T>` 写入和查询 | `lambda-framework-qdrant` |
| RabbitMQ、Kafka | `lambda-framework-mq` |
| ZooKeeper 和 Redisson 锁 | `lambda-framework-lock` |
| 雪花 ID | `lambda-framework-guid` |
| RSocket 提供方与 Java RPC 调用端 | `lambda-framework-rsocket`、`lambda-framework-rpc` |
| HTTP/WSS 网关与 Redis 令牌认证 | `lambda-framework-gateway`、`lambda-framework-security` |
| Nacos 配置与发现 | `lambda-framework-nacos` |

[组件属性、单机和集群用法](docs/MODULES-AND-CONFIGURATION.md)

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

所有 MongoDB 集合实体放在 `po` 包并以 `PO` 结尾，继承 `UnifyPO<ObjectId>`。Spring Data 审计自动维护 `createdAt`、`updatedAt` 和 `@Version`。手写更新和已有数据迁移见 [数据访问手册](docs/DATA-AND-PAGINATION.md)。

Controller 负责绑定和转发。业务逻辑与校验放在 `*ServiceImpl`，Repository 负责数据读写。

## 获取当前用户

注入 `PrincipalFactory`，在当前响应式链路里调用 `currentUserId()` 或 `currentUser(LoginUser.class)`。网关校验 Redis 会话后自动透传身份。Java RPC 的身份元数据和业务请求都使用 Fory。 [用户体系用法](docs/USER-AND-SECURITY.md)

## 开发规范与技能

<details>
<summary>展开智能体开发技能与使用入口</summary>

- [Lambda Java 后端技能](skills/lambda-java-backend/SKILL.md) 定义信息收集、代码实现、检查和交付顺序。
- [组件配置手册](docs/MODULES-AND-CONFIGURATION.md) 列出需要用户提供的属性、凭据引用与部署模式。

</details>
