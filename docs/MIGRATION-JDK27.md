# JDK 27 迁移说明

## 构建基线

JDK 从 21 升至 27；使用 `maven.compiler.release=27`，并显式配置 Lombok 注解处理器，适配新版 javac 不再隐式扫描处理器的行为。IDEA 项目 SDK、语言级别与 Maven Runner JRE 均应选择 JDK 27。

Spring Boot 升至 4.1.2，Spring Cloud 升至 2025.1.4，Spring Cloud Alibaba 升至 2025.1.0.0。独立组件版本按 Maven Central 稳定版更新；Boot BOM 管理的组件保持协同版本。JSON 公共 API 继续使用 `com.fasterxml.jackson` 的 Jackson 2 兼容线，避免改变已有序列化结果；不宣称已经迁移所有公共接口到 Jackson 3。

## 需要应用方调整的内容

1. **Security 7**：自定义 `ReactiveAuthorizationManager` 使用 `authorize` 和 `AuthorizationResult`；旧 `check` 实现需同步迁移。
2. **Boot 包拆分**：Mongo 配置类与 WebFlux 错误处理类采用新包名。Nacos 模块显式引入 Boot RSocket 支持。
3. **Nacos 配置加载**：不再使用 `bootstrap.properties`。改在 `application.properties` 中声明 `spring.config.import=optional:nacos:...`，地址、命名空间与账号由环境变量注入。
4. **Redis ACL**：新增 `lambda.repository.redis.username`；未指定用户名时保持 Redis 默认用户语义。服务器若关闭了 default 用户，必须显式配置用户名。
5. **Mongo 单机**：仅集群模式要求副本集名称。此修复不使单机具备多文档事务能力。
6. **响应类型**：已删除的 `APPLICATION_JSON_UTF8` 改为 `APPLICATION_JSON`，JSON 仍使用 UTF-8。
7. **远程构件发布**：移除历史 `distributionManagement` 及内嵌凭据；默认跳过 deploy，仅通过本机 `install` 给示例工程提供构件。

旧仓库历史曾含凭据，删除当前文件不能撤销历史泄露；凭据所有者应在对应服务轮换旧凭据。新文档、测试与配置均不保存服务器或云 API 密钥。

## 验证范围

构建覆盖所有框架模块。示例工程提供 JSON 回归及 Redis / MongoDB / RabbitMQ 实机测试；未连接的 Nacos、MySQL、Kafka、Elasticsearch、ZooKeeper 功能不能据此视为运行验证通过。详细结果以本次测试报告为准。
