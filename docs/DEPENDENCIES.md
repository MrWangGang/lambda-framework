# 依赖版本清单

版本核验日期：2026-11-07；操作说明补充：2026-11-09。本轮未升级 POM。以下版本取自 Maven Central 的 `maven-metadata.xml`，过滤预览版本。Spring 生态传递依赖由 Boot / Cloud BOM 管理，不能任意混搭最高版本。

| 坐标 | 稳定版本 |
|---|---|
| `org.springframework.boot:spring-boot-starter-parent` | `4.1.2` |
| `org.springframework.cloud:spring-cloud-dependencies` | `2025.1.4` |
| `com.alibaba.cloud:spring-cloud-alibaba-dependencies` | `2025.1.0.0` |
| `com.alibaba.nacos:nacos-client` | `3.3.0` |
| `org.apache.fory:fory-core` | `1.7.7` |
| `org.projectlombok:lombok` | `1.18.48` |
| `org.modelmapper:modelmapper` | `3.2.8` |
| `commons-beanutils:commons-beanutils` | `1.11.0` |
| `org.apache.commons:commons-lang3` | `3.21.0` |
| `com.google.guava:guava` | `33.8.0-jre` |
| `io.asyncer:r2dbc-mysql` | `1.4.3` |
| `io.r2dbc:r2dbc-pool` | `1.0.2.RELEASE` |
| `com.fasterxml.jackson.datatype:jackson-datatype-jsr310` | `2.22.4` |
| `cn.hutool:hutool-core` | `5.8.48` |
| `org.redisson:redisson` | `4.8.0` |
| `org.apache.curator:curator-recipes` | `5.9.0` |
| `io.projectreactor.rabbitmq:reactor-rabbitmq` | `1.5.6` |
| `io.projectreactor.kafka:reactor-kafka` | `1.3.25` |
| `com.aventrix.jnanoid:jnanoid` | `2.0.0` |
| `org.codehaus.mojo:versions-maven-plugin` | `2.24.0` |
| `org.apache.maven.plugins:maven-deploy-plugin` | `3.2.0` |
| `com.theokanning.openai-gpt3-java:service` | `0.18.2` |
| `com.knuddels:jtokkit` | `1.1.0` |

本项目编译目标为 JDK 27。Jackson 2 是现有公共 JSON API 的兼容线；Boot 4 自身依赖可能同时引入 Jackson 3，两者包名不同。未使用的旧 javax EL / Validation 管理项已移除。Reactor RabbitMQ 1.5.6 和 JNanoID 2.0.0 已是相应 Maven 坐标的最后稳定版，并非漏升。

复核入口：[Maven Central](https://repo.maven.apache.org/maven2/)、[Spring Boot 系统要求](https://docs.spring.io/spring-boot/system-requirements.html)、[Spring Cloud 兼容矩阵](https://spring.io/projects/spring-cloud/)。

查看实际传递依赖：`./scripts/mvn.sh dependency:tree`。请结合兼容性与测试结果升级，不使用动态 `LATEST` 版本。


## 版本管理层级

1. 框架根 POM 固定 Spring Boot parent。
2. 导入 Spring Cloud 与 Spring Cloud Alibaba BOM。
3. 独立维护的组件通过根 properties / dependencyManagement 管理。
4. 子模块只声明需要的依赖，避免重复写不同版本。
5. 示例继承框架父 POM，尽量复用同一管理结果。

当前实际解析过的关键底层版本还包括 Reactor Core 3.8.8、Spring Messaging 7.0.10、RSocket Core 1.1.5、Netty Buffer 4.2.19.Final。版本由本地依赖树和构件核验，不代表任意独立升级组合都兼容。

## 查询实际依赖

在框架根目录：

~~~bash
./scripts/mvn.sh -pl lambda-framework-rsocket dependency:tree
./scripts/mvn.sh -pl lambda-framework-repository dependency:tree
./scripts/mvn.sh -pl lambda-framework-mq dependency:tree
./scripts/mvn.sh -pl lambda-framework-nacos dependency:tree
~~~

dependencyManagement 中出现坐标，不代表每个业务应用都已经使用该依赖。依赖树才表示当前模块真正引入的路径。

只看 Maven Central 的最新版本号不够，应同时核对：

- Boot / Cloud 兼容矩阵。
- JDK 27、字节码和注解处理器兼容性。
- 序列化格式是否变化。
- 服务端协议和插件是否匹配。
- 传递依赖是否发生主版本切换。

## 升级流程

1. 保存两个仓库当前提交号与完整依赖树。
2. 查询官方发布说明 / Maven 元数据，排除不需要的预览版本。
3. 一次升级一个兼容组合，明确 breaking changes。
4. 构建全部框架模块，install 到本机。
5. 构建示例并执行受影响的真实用例。
6. 对数据库格式、消息和客户端连接进行兼容验证。
7. 更新此表的核验日期与测试记录。
8. 保留前一版本构件和恢复方式，再提交并推送。

不把仓库版本字段改成 LATEST 或 RELEASE。SNAPSHOT 仅指当前项目自己的开发版本，不是“自动跟踪所有依赖最新版本”。

## 外部 SDK 与服务版本分开管理

- Qdrant 当前通过 WebClient REST 接入，没有单独 Qdrant Java SDK。
- COS / SMS / STS 的版本建议在示例云接口文档，尚未加入 POM。
- Docker 镜像版本与 Java 客户端版本不同，不能通过升级 Maven 就升级服务器。
- RabbitMQ Server 受延迟插件兼容性约束，服务器与 reactor-rabbitmq 是不同组件。

## 验证边界

整个框架构建通过不等于所有服务兼容性通过。当前实测范围与未覆盖服务见示例 TESTING；用户体系、通用分页、RSocket 连接恢复和线程配置的待修事项见各专项手册。
