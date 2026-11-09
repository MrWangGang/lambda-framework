# 环境准备、构建与首次运行

[返回总览](../README.md) · [模块配置](MODULES-AND-CONFIGURATION.md) · [排错](OPERATIONS.md)

## 1. 前置条件

| 项目 | 要求 / 已验收环境 |
|---|---|
| Java | JDK 27；历史验收为 27.0.1.0.3 |
| Maven | 使用电脑已存在的 Maven；历史验收为 IDEA 自带 3.9.16 |
| IDE | IntelliJ IDEA，Project SDK 与 Maven Runner 均选 JDK 27 |
| Git | 可以拉取两个 GitHub 仓库；推送需要仓库写权限 |
| 网络 | 首次构建可访问所配置的 Maven 镜像或 Maven Central |
| 服务 | 仅运行示例时需要 MongoDB、Redis、RabbitMQ、Nacos、Qdrant |

本文不安装 Maven、不执行远程 Maven 发布，也不要求安装本地数据库。已有远程中间件可直接使用。

## 2. 确认 Java 与 Maven 实际使用的版本

macOS 可先列出已有 JDK：

~~~bash
/usr/libexec/java_home -V
export JAVA_HOME='/实际的/JDK27/Contents/Home'
export PATH="$JAVA_HOME/bin:$PATH"
java -version
./scripts/mvn.sh -version
~~~

最后一条输出中的 `Java version` 必须是 27，仅 `java -version` 正确还不够。IDEA 的 Maven Runner 可能选择了另一个 JDK。

脚本查找 Maven 的顺序：

1. 显式设置的 `MAVEN_BIN`。
2. `$HOME/Applications/IntelliJ IDEA.app/.../maven3/bin/mvn`。
3. `/Applications/IntelliJ IDEA.app/.../maven3/bin/mvn`。
4. 已存在于 PATH 的 `mvn`。
5. 都没有时退出并提示设置 `MAVEN_BIN`，不会安装软件。

非标准 IDEA 路径：

~~~bash
export MAVEN_BIN='/实际的/IntelliJ IDEA.app/Contents/plugins/maven-plugin/lib/maven3/bin/mvn'
./scripts/mvn.sh -version
~~~

`MAVEN_BIN` 指向可执行文件，不是 Maven 目录。含空格的路径必须加引号。

## 3. 克隆仓库

在准备存放项目的父目录执行：

~~~bash
mkdir -p tazhoo
cd tazhoo
git clone git@github.com:MrWangGang/lambda-framework.git
git clone git@github.com:MrWangGang/lambda-framework-example.git
~~~

仅需要阅读且尚未配置 SSH 时，也可用 HTTPS 克隆。不要在仓库 URL 中放入密码或访问令牌。已存在仓库时先 `git status`，不要重复克隆覆盖。

示例父 POM 使用 `../lambda-framework/pom.xml` 查找框架父项目，因此建议保留同级目录。即便相对父 POM 存在，框架的各个模块 JAR 仍需要先安装到本机 Maven 仓库。

## 4. 构建框架

工作目录：`lambda-framework` 根目录。

~~~bash
./scripts/mvn.sh -B clean install
~~~

预期结果：

- Reactor Summary 中父项目及 14 个模块均为 SUCCESS。
- 最后出现 BUILD SUCCESS。
- 本机 Maven 缓存中出现 `org/lambda/framework/.../1.0.0-SNAPSHOT`。
- 本操作不会启动 MongoDB 等服务，也不会部署应用。

常见 Maven 生命周期：

| 命令 | 用途 |
|---|---|
| `validate` | 检查项目模型，不能替代编译 |
| `compile` | 编译主源码 |
| `package` | 打包当前项目 |
| `install` | 打包并安装到本地 Maven 仓库，供另一个仓库引用 |
| `clean` | 删除构建产物，不删除业务数据库 |
| `deploy` | 远程构件发布；本项目默认跳过，不用于日常工作 |

单独修改一个模块后可以构建该模块及它的依赖，例如：

~~~bash
./scripts/mvn.sh -B -pl lambda-framework-repository -am install
~~~

交付前仍应完整构建。单模块成功不能证明其他模块没有编译影响。

## 5. IDEA 导入步骤

1. 打开框架根 `pom.xml`，按 Maven 工程导入。
2. Project Structure 中设置 Project SDK 为 JDK 27，Language Level 与项目一致。
3. Settings → Build Tools → Maven 中选已有 Bundled Maven。
4. Maven Runner JRE 选择 Project JDK / JDK 27。
5. Maven Importer JDK 同样使用兼容的 JDK。
6. Reload All Maven Projects，等待依赖解析结束。
7. 遇到 Lombok 生成方法不可见，先确认 Maven 编译能否通过，再检查 IDEA 的注解处理与插件支持。

POM 已显式声明 Lombok 注解处理器。不要用手工补 getter/setter 掩盖 IDE 配置问题，也不要靠降低 `release` 绕过 JDK 不一致。

## 6. 运行示例

工作目录切换到示例仓库：

~~~bash
cd ../lambda-framework-example
cp .env.example .env
chmod 600 .env
~~~

编辑 `.env`，填写自己的 JDK、服务器和凭据。`scripts/run.sh` 会在运行时读取文件；IDEA 直接运行 Java 主类不会自动读取这个 shell 文件。

服务准备顺序：

1. MongoDB 已启用副本集，成员地址可从应用电脑访问。
2. Redis ACL 用户和密码可用。
3. RabbitMQ 已加载兼容版本的延迟交换机插件。
4. Nacos 已创建应用配置，并放通 API / gRPC。
5. Qdrant 已开启 API Key，REST 可达。
6. 按[示例首次运行手册](https://github.com/MrWangGang/lambda-framework-example/blob/main/docs/QUICKSTART.md)启动应用。

不要把示例启动失败一律归为编译问题。应用启动还会创建 MongoDB 索引、RabbitMQ 拓扑、Qdrant 集合，任一依赖不可达都可能使启动失败。

## 7. Maven 仓库与镜像

脚本不会修改全局 `settings.xml`。如公司或网络环境要求国内 Maven 镜像，在 IDEA 的 Maven settings 或已有 Maven settings 文件配置；注意 Maven 下载源和 Docker 镜像源是两回事。

检查依赖：

~~~bash
./scripts/mvn.sh dependency:tree
./scripts/mvn.sh -pl lambda-framework-repository dependency:tree
~~~

排查“版本没更新”时先确认：

- 框架是否重新 `install`。
- 示例是否使用同一个 Maven 本地仓库。
- IDEA 的 User settings 与命令行设置是否一致。
- 是否存在显式旧版本覆盖 BOM。
- 两个工程是否被导入为正确的 Maven 模块。

如需输出 effective POM，放到被 Git 忽略的构建目录；其中可能包含仓库配置，分享前检查内容。

## 8. 完成一次可追溯交付

1. 记录两个仓库提交号和 JDK / Maven 版本。
2. 构建框架并安装到本机。
3. 构建示例，用独立资源名称进行业务验收。
4. 核对数据、消息、Nacos 实例的清理结果。
5. 更新测试记录，只写实际执行的项目。
6. 提交代码与文档；Git 推送与 Maven deploy 是不同操作。
7. 需要干净源码目录时运行两个仓库的 `./scripts/mvn.sh clean`。

当前仓库按交付要求不保留临时测试源码。`mvn test` 或 `package` 不能重现已经移除的历史外部验收，具体复核步骤见示例测试手册。
