# 依赖版本清单

核验日期：2026-11-07。以下版本取自 Maven Central 的 `maven-metadata.xml`，过滤预览版本。Spring 生态传递依赖由 Boot / Cloud BOM 管理，不能任意混搭最高版本。

| 坐标 | 稳定版本 |
|---|---|
| `org.springframework.boot:spring-boot-starter-parent` | `4.1.2` |
| `org.springframework.cloud:spring-cloud-dependencies` | `2025.1.4` |
| `com.alibaba.cloud:spring-cloud-alibaba-dependencies` | `2025.1.0.0` |
| `com.alibaba.nacos:nacos-client` | `3.3.0` |
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
