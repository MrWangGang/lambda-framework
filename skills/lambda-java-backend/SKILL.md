---
name: lambda-java-backend
description: 按统一风格开发、修改或审查 Lambda Framework Java 后端项目。适用于项目命名、POST 接口、实体内 DTO、RSocket RPC、MongoDB、分页、用户体系及交付验收。
---

新建项目或微服务前确认项目名称。用户已提供时直接使用。缺少名称时询问，等待期间可以检查现有代码和准备设计。用已确认的名称派生 Maven artifactId、Java 基础包、Spring 应用名和服务发现名。

仓库版规范位于 `../../docs/Java后端开发与架构规范.md`，本机安装版随技能提供 `references/Java后端开发与架构规范.md`。环境值从本地《第三方配置.md》或环境变量读取。提交前检查真实凭据是否混入 Git。

- 业务 HTTP 接口采用 POST 动作路径与请求 DTO。RPC 同样使用 DTO 与 Mono 或 Flux。
- 每个数据库表或集合对应一个实体。所属 DTO 放实体的静态内部类型。Service 复用 Controller 的 DTO。缓存等无实体归属的请求放 Controller 静态类型。
- Mongo 主键与关联 ID 使用 ObjectId。HTTP 和 RPC 使用十六进制字符串，在 Repository 绑定 BSON 时转换。枚举实现 ConverterEnum。业务金额与标量用字符串，技术字段按规范转换。
- 每个持久化实体明确表或集合名及唯一的 @Id，可复用基类标识。按 Repository 的筛选、排序、唯一约束或生命周期需求加索引，说明用途并核对实际索引和执行计划。
- 复用框架分页、异常、Reactor Context 和数据库连接。区分 SecurityLoginUser、UnifyPO、PrincipalFactory。
- RSocket Channel 保持首消息、单次订阅、背压、取消和 Payload 所有权。网关 JSON 聚合有上限，NDJSON 逐条输出。聊天 DTO 描述文字、表情与媒体引用。
- 使用现有 JDK 和 IDEA Maven。先复现再修复，在相同条件复测。使用隔离测试资源，验收后按名称清理，核对数据并关闭本次启动的 JVM。
- README 记录项目运行与契约。报告区分实测、设计方案和未测范围。
