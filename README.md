# Lambda Framework 操作手册

基于 **JDK 27、Spring Boot 4.1.2、Spring Cloud 2025.1.4** 的模块化响应式 Java 框架。业务继续使用 `Mono / Flux`，按需接入数据访问、消息、用户上下文、认证授权、RSocket RPC、网关与配置中心。

文档更新：**2026-11-09**。对应本仓库当前源码；已验证能力、已知问题和设计方案分别说明。这里的服务器地址、密码、云凭据均使用占位符，实际环境资料保存在不提交 Git 的本地《开发规范.md》。

## 1. 从哪里开始

| 你的任务 | 阅读顺序 |
|---|---|
| 第一次运行项目 | [环境与构建](docs/GETTING-STARTED.md) → [可运行示例](https://github.com/MrWangGang/lambda-framework-example) |
| 给业务项目接 Redis / MongoDB / RabbitMQ | [模块和配置](docs/MODULES-AND-CONFIGURATION.md) |
| 找用户超类、做登录与权限 | [用户体系手册](docs/USER-AND-SECURITY.md) |
| 使用 CRUD、分页、设计 MongoDB 关系 | [数据访问与分页](docs/DATA-AND-PAGINATION.md) |
| 排查 RSocket 连接、了解虚拟线程 | [RSocket 与线程模型](docs/RSOCKET-AND-VIRTUAL-THREADS.md) |
| 部署、更新、GitHub SSH、排错 | [维护与交付](docs/OPERATIONS.md) |
| 查看依赖与升级步骤 | [依赖清单](docs/DEPENDENCIES.md) · [JDK 27 迁移](docs/MIGRATION-JDK27.md) |
| 查看真实测试范围 | [示例验收记录](https://github.com/MrWangGang/lambda-framework-example/blob/main/docs/TESTING.md) |

## 2. 两个仓库如何配合

~~~text
tazhoo/
├── lambda-framework/          框架父 POM + 14 个模块
└── lambda-framework-example/  示例父 POM + lambda-example-app
~~~

先把框架 `install` 到本机 Maven 仓库，再构建示例。框架根项目是 `pom`，不能把它当成独立应用启动；可运行的 Boot 应用在示例仓库。

~~~bash
# 从 tazhoo 目录开始；电脑已有 Git、JDK 27 和 IDEA
git clone git@github.com:MrWangGang/lambda-framework.git
git clone git@github.com:MrWangGang/lambda-framework-example.git
cd lambda-framework
export JAVA_HOME='/实际的/JDK27/Contents/Home'
./scripts/mvn.sh -version
./scripts/mvn.sh clean install
cd ../lambda-framework-example
cp .env.example .env
# 编辑 .env，并按示例手册在 Nacos 创建启动配置
./scripts/run.sh
~~~

`scripts/mvn.sh` 只查找已有 Maven，优先使用 macOS IntelliJ IDEA 内置 Maven，**不会下载安装 Maven**。首次下载依赖需要联网。`install` 只写本地缓存；框架默认跳过远程 `deploy`。

## 3. 当前交付能力与边界

| 能力 | 状态 |
|---|---|
| 14 个框架模块 + 父项目的 JDK 27 构建 | 2026-11-07 全部通过 |
| MongoDB 副本集事务、Redis ACL、RabbitMQ 延迟消息 | 已实机验证，详见示例验收记录 |
| Nacos 配置导入、刷新、服务注册与注销 | 已实机验证 |
| Qdrant 向量读写 | 示例通过 WebClient 接入；框架没有独立 Qdrant 模块 |
| 示例商品复合游标分页 | 已实机验证 |
| 框架 `Paging/Paged` 通用分页 | 存在排序、页码和 Builder 默认值问题；外部回归 12 项中 8 项失败，尚未修复 |
| 用户认证授权抽象 | 源码已有；示例尚未接入完整登录授权，未完成端到端验收 |
| 跨服务 RSocket RPC | 源码已有；已识别连接管理风险，未完成跨服务故障恢复验收 |
| 整体虚拟线程接入 | 已分析方案；部分模块已有开关，不代表全项目已经验证启用 |
| MySQL / Elasticsearch / Kafka / ZooKeeper / 锁 | 模块参与构建；未部署对应服务做实机验收 |
| 腾讯云 COS / SMS | 已整理对接设计；没有上传云文件或发送短信 |

不能把“构建成功”解释成所有模块业务功能和高可用均验证通过。

## 4. 模块索引

完整 artifactId 为 `lambda-framework-` 加下列模块名。

| 模块 | 主要入口与用途 |
|---|---|
| `common` | `UnifyPO`、`SecurityLoginUser`、`SecurityStash`、异常、JSON、RPC 注解 |
| `repository` | Redis 操作、Mongo/R2DBC/ES 配置、`Paging/Paged` |
| `compliance` | `DefaultBasicServiceImpl`、树形业务约定、`PrincipalFactory` |
| `security` | `SecurityAuthManager` 认证、`SecurityAutzManager` 授权 |
| `web` | WebFlux 上下文、响应与异常过滤器 |
| `gateway` | 网关过滤、RSocket 转发与用户上下文 |
| `rsocket` | RSocket 编解码、请求元数据拦截、异常处理 |
| `rpc` | 接口客户端代理与服务端路由生成 |
| `loadbalance` | 服务发现、RSocketRequester 缓存与负载均衡 |
| `nacos` | Nacos 配置/发现依赖及 RSocket 端口注册监听 |
| `mq` | RabbitMQ / Kafka 响应式连接与消息操作 |
| `httpclient` | WebClient 响应过滤辅助 |
| `guid` | 雪花 ID 工厂 |
| `lock` | Redisson / ZooKeeper 锁封装 |

Redis 和 MongoDB 都在 `repository` 中，不存在 `lambda-framework-redis` 或 `lambda-framework-mongodb` 构件。不要把所有模块无条件扫描进一个应用；不同模块可能引入额外基础设施配置。

## 5. 引入框架

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
    <dependency>
        <groupId>org.lambda.framework</groupId>
        <artifactId>lambda-framework-mq</artifactId>
    </dependency>
</dependencies>
~~~

框架自身的 `1.0.0-SNAPSHOT` 是本地开发版本；外部依赖采用固定版本。父 POM 的 dependencyManagement 负责版本，不等于把所有模块自动加入应用。

Spring Boot 4 的自动配置有模块拆分；例如需要自动注入 `WebClient.Builder` 的应用，应显式引入 `spring-boot-starter-webclient`。完整配置以示例 POM 和手册为准。

## 6. 用户体系的入口

用户对象实现 `SecurityLoginUser<ID>`；公共实体字段可复用 `UnifyPO<ID>`。登录身份操作父类是 `PrincipalFactory`，业务通过 `SecurityPrincipalHolder` 读取当前 Reactor Context 中的身份。

身份验证和角色权限规则由应用实现。默认 `verify` 方法返回 `true`，不能将默认配置当成完整业务权限系统。完整生命周期、ACL 配置缺口和接入示意见[用户体系手册](docs/USER-AND-SECURITY.md)。

## 7. 文档使用约定

- 标注“当前实现”的内容来自源码；命令在指定工作目录执行。
- 标注“方案 / 待实现”的接口与类不会因为写入文档就出现在代码中。
- 示例返回值中的 ID、时间、版本号应以实际响应为准。
- 维护操作先辨别业务数据与独立验收资源；不要用删除全部数据库或数据卷的方式清理测试。
- 完整服务器账号、密码、SSH 私钥保存在本地私有资料；公开仓库只保留配置变量与操作方法。
