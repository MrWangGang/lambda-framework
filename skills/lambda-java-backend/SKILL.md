---
name: lambda-java-backend
description: 按统一风格开发、修改或审查 Lambda Framework Java 后端项目。适用于项目命名、POST 接口、Facade 契约、RSocket RPC、MongoDB、分页和用户体系。
---

新建项目或微服务前确认项目名称。用户已提供时直接使用。缺少名称时询问，等待期间可以检查现有代码和准备设计。用已确认的名称派生 Maven artifactId、`com.<项目名>` 包根、Spring 应用名、服务发现名和 Nacos Data ID `<项目名称>.properties`。

仓库版规范位于 `../../docs/Java后端开发与架构规范.md`，本机安装版随技能提供 `references/Java后端开发与架构规范.md`。环境值从本地《第三方配置.md》或环境变量读取。提交前检查真实凭据是否混入 Git。

- 业务微服务拆为 `<项目名>-service` 和 `<项目名>-facade`。service 依赖 facade，RPC 调用方只依赖 facade。
- 对外接口放 facade 模块的 `facade` 包，服务端实现放 service 模块的 `facade` 包。业务类 `UserServiceImpl` 直接放 `service` 包，省略业务接口和 `impl` 子包。
- 对外 HTTP 一律经网关转发，使用 POST 动作路径。WSS 也由网关接入。业务服务只监听内网 RSocket TCP。网关只转发已发布的固定 @MessageMapping 路径。Controller、Facade 实现与 Service 共用 Facade 接口内的请求、响应 record。签名保留 `UserFacade.FindRequest`、`UserFacade.Detail` 等外层归属，内层名称按业务动作或用途命名。实体使用 `UserEntity` 等名称，仅在 service 模块持久化使用。
- 应用运行配置放 Nacos，本地只留项目名、导入声明和 Nacos 引导配置。网关使用 reactive，HTTP 使用 `rb://服务名`，WSS 使用 `ws://服务名`。网关到业务服务使用 RSocket JSON。业务进程用 none 和 TCP。Facade 方法只写 @MessageMapping 和签名。原生 Java RPC 自动使用 Fory，从同版 Facade 契约收集 record、枚举和框架分页类型。
- Mongo 主键与关联 ID 使用 ObjectId。HTTP JSON 用十六进制字符串，公开 record 的单值 ID 用 @JsonObjectId ObjectId，Fory 直接处理 ObjectId，Repository 直接绑定原生 ID。枚举实现 ConverterEnum。业务金额与标量用字符串，技术字段按规范转换。
- 每个持久化实体明确表或集合名及唯一的 @Id，可复用基类标识。按 Repository 的筛选、排序、唯一约束或生命周期需求加索引，说明用途并核对实际索引和执行计划。
- 按业务读写比例、访问路径和增长设计关系。多对多必须有中间表或关系集合，一对多和多对一直接内嵌或持有引用。内嵌需有业务上限，独立分页和高频更新用引用。聚合用未加引号的占位符绑定 ObjectId，按目标集合 BSON 字段名编写关联条件。
- @Version 按需使用。SKU 以库存足额条件加原子扣减防超卖。字符串金额用旧金额和版本做条件更新，跨文档一致性用事务与幂等键。
- 复用框架分页、异常、Reactor Context 和数据库连接。区分 SecurityLoginUser、UnifyPO、PrincipalFactory。
- RSocket Channel 保持首消息、单次订阅、背压、取消和 Payload 所有权。网关 JSON 聚合有上限，NDJSON 逐条输出。 `rb://` 根据返回类型决定响应。Mono 返回单次 JSON，Flux 聚合为 JSON 数组，UiFlux 单向 NDJSON，BiFlux 配合普通 Flux 入参走双向 NDJSON。流式成功行的 data 是数组，失败行的 data 是空数组。WSS 只开放 `Flux<I> → BiFlux<O>` 路由。每条 WebSocket 文本消息承载一个 JSON 值。聊天 record 描述文字、表情与媒体引用。
- 使用现有 JDK 和 IDEA Maven。README 记录启动方式、接口契约与配置。
