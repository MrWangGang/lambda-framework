# 维护、GitHub SSH 与交付操作

[返回总览](../README.md) · [环境准备](GETTING-STARTED.md) · [服务运维](https://github.com/MrWangGang/lambda-framework-example/blob/main/docs/OPERATIONS.md)

## 1. 修改前确认状态

两个仓库分别执行：

~~~bash
git status --short
git branch --show-current
git remote -v
git log -3 --oneline
~~~

先识别未提交内容来源，不通过 reset --hard 丢弃工作。框架和示例是两个独立仓库，构建、提交、推送分别进行。

## 2. 配置 GitHub SSH

复用已有密钥，以下路径为占位：

~~~bash
chmod 600 /实际路径/github_key
ssh -T -i /实际路径/github_key -o IdentitiesOnly=yes git@github.com
~~~

GitHub 返回账号认证成功提示即可；它不提供交互 shell，退出码可能不是 0。不要只凭退出码判断失败。

需要新密钥时生成专用密钥，把 .pub 公钥加入 GitHub Settings → SSH and GPG keys。私钥保留在本机，已授权的私有备份见本地《第三方配置.md》，不提交到公开仓库。首次连接核对主机指纹，不关闭校验。

## 3. 设置推送地址

框架仓库：

~~~bash
git remote set-url --push origin git@github.com:MrWangGang/lambda-framework.git
git config core.sshCommand 'ssh -i /实际路径/github_key -o IdentitiesOnly=yes'
git ls-remote origin refs/heads/main
~~~

示例仓库改为 `MrWangGang/lambda-framework-example.git`。HTTPS 拉取和 SSH 推送可以并存，以 `git remote -v` 为准。

Permission denied (publickey) 时检查密钥、公钥绑定账号和仓库写权限。浏览器已登录不代表命令行 SSH 自动授权。

## 4. 构建与推送顺序

~~~bash
# 框架仓库
./scripts/mvn.sh -B clean install
git diff --check
git diff --stat
git add README.md docs/
git commit -m "docs: expand framework operation manual"
git push origin main
~~~

上例只暂存文档。业务代码按实际文件暂存，构建成功不替代业务验收。

~~~bash
# 示例仓库
./scripts/mvn.sh -B clean package
git diff --check
git add README.md docs/
git commit -m "docs: expand example operation manual"
git push origin main
~~~

推送后核对：

~~~bash
git rev-parse HEAD
git ls-remote origin refs/heads/main
git status --short
~~~

本地 HEAD 与远程 main 相同表示同步完成。无改动时不创建空提交，日常更新不使用强制推送。Git push 与 Maven deploy 是不同操作。

## 5. 需要同步维护的文档

| 改动 | 更新内容 |
|---|---|
| POM / JDK | 依赖清单、迁移说明、构建步骤 |
| 配置 | 名称、默认值、示例变量、排错 |
| HTTP 接口 | 方法、字段、状态、curl、清理 |
| 用户体系 | 身份流转、会话、权限、测试矩阵 |
| 分页 | 页码基数、排序、count、索引、边界 |
| 中间件 | 版本兼容、备份与验收范围 |
| 测试 | 日期、提交号、环境、通过/失败、清理 |

文档日期不是“永远最新”的保证，升级时重新核验兼容性并固定版本。

## 6. 构建排错

| 现象 | 检查 | 处理方向 |
|---|---|---|
| release 27 not supported | Maven -version 的 Java | Runner 改 JDK 27 |
| 框架构件无法解析 | 同级目录、本地仓库 | 先框架 install |
| Lombok 方法缺失 | 编译与注解处理器 | 区分 IDE 和 javac 问题 |
| NoSuchMethodError | dependency:tree | 排查覆盖 BOM 的冲突 |
| 下载失败 | settings、代理、镜像 | 修下载链路，不删整个缓存 |
| deploy 没上传 | maven.deploy.skip | 当前默认行为，日常用 install |
| 类存在但 Bean 缺失 | 扫描 / Import / 条件配置 | 按模块注册 |
| web 加入后没 HTTP 端口 | web-application-type | 参照已知配置问题 |

## 7. 运行故障保留哪些证据

记录两个提交号、运行时版本、启动异常第一个 cause、非敏感有效配置、请求号与时间、对应服务端日志。

分享前移除密码、Auth-Token、云密钥、签名 URL 和用户敏感字段。不能将整份含凭据的 effective config 附在公开 Issue 中。

## 8. 数据与回滚边界

框架不部署中间件，仓库没有一键重建整台服务器的 Compose 模板。服务器配置属于部署环境，见示例运维手册。

- mvn clean 只删除构建目录。
- git revert 不回滚数据库和消息。
- 停止应用不清理业务数据。
- Docker 命名卷不等于异地备份。
- 短信、COS 对象和 Rabbit 消息不会随 MongoDB 事务自动撤销。

回滚应用前确认字段、索引、消息格式兼容；涉及数据迁移时先设计恢复流程，不删除整个数据卷重试。

## 9. 当前验证范围

2026-11-07 的框架构建和示例实机验收继续保留。后续发现的通用分页失败、用户配置缺口、RSocket 风险和虚拟线程方案单独记录。

本轮只扩充文档，不改变上述源码。文档检查与历史业务验收分别记录。
