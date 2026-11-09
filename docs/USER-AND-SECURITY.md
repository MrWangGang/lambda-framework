# 用户体系、登录身份与授权操作手册

[返回总览](../README.md) · [模块配置](MODULES-AND-CONFIGURATION.md) · [线程与上下文](RSOCKET-AND-VIRTUAL-THREADS.md)

文档对应当前实现。框架提供身份与授权扩展点；**当前示例没有注册、登录、密码校验、验证码或完整 RBAC 接口**。以下接入片段是应用开发指导，不是已经存在的示例端点。

## 1. 超类与接口关系

~~~text
UnifyPO<ID>                       实体基础字段：id/createTime/updateTime
SecurityLoginUser<ID>             用户对象约定：getId/setId

PrincipalFactory                 登录会话与用户信息操作父类
├── SecurityPrincipalFactory     Security 认证环节读取 Redis 并校验 token
└── SecurityPrincipalHolder      业务环节读取 Reactor Context 的身份

SecurityAuthManager              认证扩展：verify(principal)
SecurityAutzManager              授权扩展：verify(pathRule, principal)
~~~

关键源码：

- [SecurityLoginUser](../lambda-framework-common/src/main/java/org/lambda/framework/common/po/SecurityLoginUser.java)
- [UnifyPO](../lambda-framework-common/src/main/java/org/lambda/framework/common/po/UnifyPO.java)
- [PrincipalFactory](../lambda-framework-compliance/src/main/java/org/lambda/framework/compliance/security/PrincipalFactory.java)
- [SecurityPrincipalHolder](../lambda-framework-compliance/src/main/java/org/lambda/framework/compliance/security/SecurityPrincipalHolder.java)
- [SecurityConfig](../lambda-framework-security/src/main/java/org/lambda/framework/security/config/SecurityConfig.java)

`UnifyPO` 本身不是用户表；`SecurityLoginUser` 是接口；`PrincipalFactory` 是登录身份能力的抽象父类。它们分工不同，可以组合使用。

## 2. 定义业务用户与登录快照

实体示意：

~~~java
@Data
@EqualsAndHashCode(callSuper = true)
public class User extends UnifyPO<String>
        implements SecurityLoginUser<String> {
    private String nickname;
    private boolean enabled;
}
~~~

这里 getter/setter 由 Lombok 和父类提供，应用需正确导入框架类型及 Lombok 注解。继承 `UnifyPO` 不会自动让所有保存动作都更新时间；审计字段由实际保存路径 / 审计配置负责。

登录快照建议单独建 DTO，实现 `SecurityLoginUser<String>`，只包含认证和业务请求需要的字段，例如 userId、昵称、状态、租户和必要权限版本。不要把密码哈希、短信验证码、支付信息或大数组放入登录快照。

当前 `PrincipalFactory.setPrincipal` 会序列化传入对象的全部可序列化属性。直接传数据库实体之前，应确认哪些字段会进入 Redis 和 RPC 元数据。

## 3. 登录会话如何创建

业务应先完成凭据校验，再调用：

~~~java
return verifiedLoginUser
    .flatMap(principalHolder::setPrincipal);
~~~

`verifiedLoginUser` 表示应用已经校验成功的 `Mono<LoginUser>`，不是框架自带方法。用户名密码登录、验证码登录、第三方授权各自完成认证后，才能进入这一阶段。

当前存储结构：

~~~text
Redis key = lambda.security.auth-token.<userId>
Redis value = { principal: "用户 JSON", token: "随机后缀" }
返回 token = <Redis key>.<随机后缀>
初始 TTL = 2592000 秒，即 30 天
~~~

请求头名称是 `Auth-Token`，不是自动解析 `Authorization: Bearer ...`。

~~~http
Auth-Token: <登录接口实际返回的完整令牌>
~~~

同一 userId 再次登录会覆盖同一 Redis key，旧令牌随后无法匹配。当前设计相当于“每个用户一份有效会话”，没有按设备、客户端类型维护多会话表。

## 4. 一次 HTTP 请求的身份流转

1. 请求上下文过滤器把 `ServerWebExchange` 放进 Reactor Context。
2. `SecurityAuthManager` 读取 `Auth-Token`。
3. `SecurityPrincipalFactory` 解析令牌、读取 Redis、比较随机后缀。
4. 业务 `verify(principal)` 决定账号是否允许继续访问。
5. `SecurityAutzManager` 按请求路径读取权限规则，调用业务授权判断。
6. Web / Gateway 的后置身份过滤器把 principal 与 token 写入 `SecurityStash`。
7. 业务通过 `SecurityPrincipalHolder` 从 Context 获取身份。
8. RSocket 客户端可将同一身份快照放入元数据传递给下游。

当前 Web / Gateway 过滤器不是只依赖 ThreadLocal。线程切换时应维护响应式链和 Reactor Context。RSocket 元数据携带的身份还需要明确可信来源；不能把“收到 principal JSON”当成已经通过独立认证。

## 5. 业务读取当前用户

~~~java
return principalHolder.fetchPrincipal2Object(LoginUser.class)
    .flatMap(user -> loadVisibleOrders(user.getId()));
~~~

`LoginUser / loadVisibleOrders` 是业务类型和方法占位。

| 方法 | 用途 / 注意点 |
|---|---|
| `setPrincipal(user)` | 写入登录快照，生成新令牌 |
| `getAuthToken()` | 取当前请求的令牌 |
| `fetchPrincipal()` | 从当前上下文取用户 JSON |
| `fetchPrincipal2Object(type)` | 从上下文转为指定用户类型 |
| `updatePrincipal(user)` | 校验当前令牌，再更新它关联的快照 |
| `revisePrincipal(user)` | 按 userId 修改已有会话快照；必须由可信业务调用 |
| `deletePrincipal()` | 删除当前令牌解析出的 Redis key |
| `getPrincipal(token, type)` | 已标 Deprecated；直接查 Redis 校验，不作为常规业务读取入口 |

`fetchPrincipal` 不会重新查询数据库，也不会自动检测用户表刚发生的变更。用户被禁用、角色变更、改密码时，应用需要决定撤销会话、更新快照或检查权限版本。

## 6. Redis 配置与已发现的 ACL 缺口

认证与授权使用两个具名 Bean：

| Bean | 配置前缀 | 默认数据库 |
|---|---|---:|
| `securityAuthRedisOperation` | `lambda.security.redis.auth.*` | 0 |
| `securityAutzRedisOperation` | `lambda.security.redis.autz.*` | 1 |

示意配置：

~~~properties
lambda.security.redis.auth.host=REDIS_HOST:6379
lambda.security.redis.auth.password=APP_PASSWORD
lambda.security.redis.auth.database=0
lambda.security.redis.auth.deploy=single
lambda.security.redis.autz.host=REDIS_HOST:6379
lambda.security.redis.autz.password=APP_PASSWORD
lambda.security.redis.autz.database=1
lambda.security.redis.autz.deploy=single
~~~

**当前缺口：两个 Security Redis 配置类未声明 username，也未覆盖父类的 `username()`。** 因此业务缓存的 `lambda.repository.redis.username` 不会自动应用到认证 / 授权 Redis。服务器如果关闭 default 用户、只允许 root ACL 用户，这两条认证连接不能仅靠上面的密码配置完成接入。

后续应为专用配置补齐 ACL 用户名并测试 Bean 生命周期；目前尚未修改。不要为了掩盖这个配置缺口，直接把“业务缓存读写通过”写成“用户认证也通过”。

## 7. 认证、授权扩展规则

`SecurityAuthManager.verify(String principal)` 用于账号状态等业务认证判断；`SecurityAutzManager.verify(String currentPathAutzTree, String principal)` 用于权限判断。两者当前都是同步 boolean 接口。

适合放入这些方法的是已加载快照上的快速检查。若需要额外数据库或远程调用，不应在方法中直接 `block()`；应设计响应式验证流程，或者明确隔离阻塞任务。启用虚拟线程不会自动改变这些接口的执行线程。

默认 Bean 的两个 `verify` 都返回 true：

- token 本身仍经过 Redis 校验。
- 默认不检查账号封禁 / 注销状态。
- 默认不解析并匹配角色或权限树。
- 不自动实现租户隔离和数据行级权限。

应用应注册自己的认证 / 授权实现，确认 `@ConditionalOnMissingBean` 默认实现已被替换。

## 8. URL 授权模式

属性 `lambda.security.url-autz-model` 的取值以 `SecurityContract` 常量为准，当前为 `ALL` / `MAPPING`（区分大小写）：

- `ALL`：请求路径没有权限配置时拒绝访问。
- `MAPPING`：没有权限配置时仍进入业务 verify，默认 verify 为 true 时允许已认证用户访问。
- 配置项 `lambda.security.permit-urls` 是逗号分隔的放行路径。
- 授权规则当前以 URI path 查询 Redis，没有把 HTTP method 合并进 key。

例如 `GET /orders` 与 `DELETE /orders` 需要不同权限时，现有路径 key 设计需要扩展。规则值格式由业务 verify 约定，不能凭框架存在 `SecurityRole / SecurityPermission` 就假定已经实现某个完整 RBAC 模型。

当前授权读取 Redis 失败会转换为空规则；MAPPING 模式下可能因此放行。生产接入前应明确依赖故障时的拒绝策略，加入对应回归。

## 9. 需要优先验证的问题

| 项目 | 当前观察 | 后续验证目标 |
|---|---|---|
| 用户 ID 格式 | 令牌正则的 userId 段限制为字母数字；带连字符 UUID 可生成令牌却可能校验失败 | 创建与校验采用一致的 ID 规范 |
| 初始有效期 | 创建会话为 30 天 | 更新用户快照后 TTL 仍符合预期 |
| 快照更新 | Redis 无 timeout 的 update 使用 setIfPresent | 实测并修正 TTL 保留 / 重设策略 |
| 退出登录 | 方法按令牌解析 key 删除，没有在此方法内部比较随机后缀 | 旧会话不能删除后来建立的新会话 |
| 多租户 | key 当前只有 userId | 不同租户同 ID 不能相互覆盖 |
| 权限检查 | 默认 verify 返回 true | 默认行为与业务实现的拒绝用例明确 |
| RSocket 身份 | 接收并写入上下文的 principal 来自请求元数据 | 防止未验证调用方伪造用户快照 |
| Spring Security 上下文 | 自定义身份存于 exchange / SecurityStash | 另测 `@PreAuthorize` 等方法权限，不能假定已正确集成 |

本节是源码检查和待验收事项，没有宣称已运行攻击模拟或修复这些问题。

## 10. 示例接入的推荐顺序

1. 先补齐认证 Redis ACL 配置及默认授权策略。
2. 定义 User 实体和轻量 LoginUser 快照。
3. 实现登录凭据校验、登录失败限制及会话创建。
4. 将允许匿名的路径明确列入 permit-urls。
5. 实现账号状态验证和具体角色权限判定。
6. 在业务入口从上下文取得 userId，替代信任 URL / JSON 中任意提交的用户 ID。
7. 再补多端登录、租户、改密踢出、权限更新策略。
8. 验证登录、错误令牌、过期、禁用、跨用户访问、注销、并发会话及虚拟线程切换。
9. 完成后才能将示例标记为“用户体系已接入”。

当前示例的收藏 `userId` 和购买 `userId` 均由请求输入，不能作为已认证用户身份使用。
