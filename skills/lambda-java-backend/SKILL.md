---
name: lambda-java-backend
description: 按统一风格开发、修改或审查 Lambda Framework Java 后端项目。适用于项目命名、POST 接口、实体内 DTO/VO、RSocket RPC、MongoDB、分页、用户体系及交付验收。
---

新建项目或微服务前确认项目名称。用户已提供时直接使用。缺少名称时询问，等待期间可以检查现有代码和准备设计。用已确认的名称派生 Maven artifactId、Java 基础包、Spring 应用名、服务发现名和 Nacos Data ID `<项目名称>.properties`。

仓库版规范位于 `../../docs/Java后端开发与架构规范.md`，本机安装版随技能提供 `references/Java后端开发与架构规范.md`。环境值从本地《第三方配置.md》或环境变量读取。提交前检查真实凭据是否混入 Git。

- 业务 HTTP 接口采用 POST 动作路径，HTTP、Service 与 RPC 共用请求 DTO 和响应 VO。
- 每个数据库表或集合对应一个 PO。DTO 和 VO 定义在所属 PO 内，按业务动作或返回用途命名，内部类型名不重复加实体名或表名。PO 外只导入外层类型，签名保留 `UserPO.FindDTO`、`UserPO.DetailVO`。数据库 PO 只用于持久化，接口参数、返回值和契约继承都不能使用 PO。
- 应用运行配置放 Nacos，本地只留项目名、导入声明和 Nacos 引导配置。网关使用 reactive，普通 JSON POST 自动转发并保留响应 JSON 类型。
- Mongo 主键与关联 ID 使用 ObjectId。HTTP 和 RPC 的 JSON 用十六进制字符串，DTO/VO 单值字段用 @JsonObjectId ObjectId，Repository 直接绑定原生 ID。枚举实现 ConverterEnum。业务金额与标量用字符串，技术字段按规范转换。
- 每个持久化实体明确表或集合名及唯一的 @Id，可复用基类标识。按 Repository 的筛选、排序、唯一约束或生命周期需求加索引，说明用途并核对实际索引和执行计划。
- 按业务读写比例、访问路径和增长设计关系。多对多必须有中间表或关系集合，一对多和多对一直接内嵌或持有引用。内嵌需有业务上限，独立分页和高频更新用引用。聚合用未加引号的占位符绑定 ObjectId，实测复杂关联与执行计划。
- @Version 按需使用。SKU 以库存足额条件加原子扣减防超卖。字符串金额用旧金额和版本做条件更新，跨文档一致性用事务与幂等键。
- 复用框架分页、异常、Reactor Context 和数据库连接。区分 SecurityLoginUser、UnifyPO、PrincipalFactory。
- RSocket Channel 保持首消息、单次订阅、背压、取消和 Payload 所有权。网关 JSON 聚合有上限，NDJSON 逐条输出。聊天 DTO/VO 描述文字、表情与媒体引用。
- 使用现有 JDK 和 IDEA Maven。先复现再修复，在相同条件复测。使用隔离测试资源，验收后按名称清理，核对数据并关闭本次启动的 JVM。
- README 记录项目运行与契约。报告区分实测、设计方案和未测范围。
