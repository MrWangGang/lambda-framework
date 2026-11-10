# JDK 27 迁移说明

## 构建基线

JDK 从 21 升至 27；使用 `maven.compiler.release=27`，并显式配置 Lombok 注解处理器，适配新版 javac 不再隐式扫描处理器的行为。IDEA 项目 SDK、语言级别与 Maven Runner JRE 均应选择 JDK 27。

Spring Boot 升至 4.1.2，Spring Cloud 升至 2025.1.4，Spring Cloud Alibaba 升至 2025.1.0.0。独立组件版本按 Maven Central 稳定版更新；Boot BOM 管理的组件保持协同版本。JSON 公共 API 继续使用 `com.fasterxml.jackson` 的 Jackson 2 兼容线，避免改变已有序列化结果；不宣称已经迁移所有公共接口到 Jackson 3。

## 需要应用方调整的内容

1. **Security 7**：自定义 `ReactiveAuthorizationManager` 使用 `authorize` 和 `AuthorizationResult`；旧 `check` 实现需同步迁移。
2. **Boot 包拆分**：Mongo 配置类与 WebFlux 错误处理类采用新包名。Nacos 模块显式引入 Boot RSocket 支持；需要自动注入 `WebClient.Builder` 的应用显式依赖 `spring-boot-starter-webclient`。
3. **Nacos 配置加载**：不再使用 `bootstrap.properties`。改在 `application.properties` 中声明 `spring.config.import=nacos:...`，地址、命名空间与账号由环境变量注入。Nacos Client 显式升级为 3.3.0。
4. **Redis ACL**：新增 `lambda.repository.redis.username`；未指定用户名时保持 Redis 默认用户语义。服务器若关闭了 default 用户，必须显式配置用户名。
5. **Mongo 部署模式**：单机无需 replica；集群模式设置 `lambda.repository.mongo.deploy=cluster` 和 `lambda.repository.mongo.replica=rs0`。框架客户端及 Boot ConnectionDetails 均传递副本集名称，支持真实副本集事务。
6. **响应类型**：已删除的 `APPLICATION_JSON_UTF8` 改为 `APPLICATION_JSON`，JSON 仍使用 UTF-8。
7. **远程构件发布**：移除历史 `distributionManagement` 及内嵌凭据；默认跳过 deploy，仅通过本机 `install` 给示例工程提供构件。

旧仓库历史曾含凭据，删除当前文件不能撤销历史泄露；凭据所有者应在对应服务轮换旧凭据。新文档、测试与配置均不保存服务器或云 API 密钥。

## 验证范围

构建覆盖全部框架模块。当前示例使用 MongoDB 副本集、Redis、Qdrant、RabbitMQ、Nacos；Kafka 与 ZooKeeper 可独立配置。


## 迁移前检查

- 记录原来的 Java、Boot、Cloud、Alibaba、数据库驱动版本。
- 保存现有配置名称、路由、DTO 和序列化样本。
- 确认开发与运行环境都能使用 JDK 27。
- 区分服务端版本与 Java SDK 版本。
- 备份涉及的数据，明确哪些变更可以只回滚 JAR，哪些需要数据迁移。
- 先在独立资源名称下迁移，不覆盖共享业务库。

## 分步骤迁移

### 第一步：统一构建环境

设置 JAVA_HOME、IDEA SDK、Maven Runner 和 Importer，执行 Maven -version 检查。POM 的 release=27 只设置编译目标，不能替电脑切换 JDK。

### 第二步：构建框架

从根目录 clean install，确认 15 个 reactor 项目成功。失败时保留第一个编译错误，先解决 API / 包名变化，不使用跳过整个模块的方式宣布完成。

### 第三步：迁移应用配置

- Nacos 改用 application.properties 的 spring.config.import。
- 区分业务 Mongo database 与账号 auth-database。
- 副本集名称和服务端一致。
- Redis ACL 必须显式选择正确用户。
- Nacos Java SDK 同时需要 API 与 gRPC 网络通路。
- 有 WebClient.Builder 注入需求时检查 Boot 4 starter 拆分。
- 不复制旧仓库中的密码、私钥和远程发布配置。

### 第四步：核对运行时行为

启动后不仅查看 Started 日志，还要验证配置导入、索引创建、消息拓扑、向量集合、真实读写以及应用关闭资源释放。

### 第五步：业务回归

按示例 TESTING 执行商品、游标、并发库存、幂等、事务、TTL、延迟消息和向量查询；新业务还要补自己的边界用例。

### 第六步：交付记录

保存代码提交、工具与服务版本、实际用例和清理结果。删除临时测试资源后，推送源码与不含凭据的文档。

## 暂未解决的兼容与实现问题

本次版本升级不表示历史业务缺陷全部清除：

- 通用分页存在排序、页码和 Builder 默认值问题。
- Security 专用 Redis 未接入 ACL username。
- 默认认证/授权扩展返回 true，需要业务实现。
- Web/Gateway/RSocket 线程开关的注册方式和时序需要整理。
- RSocket 多实例标识与发现恢复等仍需修复和验证。

详见数据分页、用户体系和 RSocket 手册。不要把这些问题与 JDK 27 编译成功混为一谈。

## 回滚原则

应用与框架需回到兼容的提交组合，重新构建并安装。数据和消息格式若已变化，先核验旧代码能否读取；不要仅回退 POM 就直接启动旧版本访问新格式数据。
