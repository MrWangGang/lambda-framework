# 登录、会话与跨服务身份

网关引入 `lambda-framework-security`，应用引入 `lambda-framework-compliance`。网关和处理登录的服务使用同一个 Redis 会话库。用 `lambda.security.permit-paths` 声明登录等公开路径，其他请求由网关从 `Authorization: Bearer <token>` 获取令牌并校验会话。

## 业务实现

用户实体可继承 `UnifyPO<ObjectId>`。登录快照使用实现 `SecurityLoginUser<ID>` 的独立类型，保留 ID 和业务需要的字段。实际应用在核验密码或其他凭据后调用 `principalFactory.setPrincipal(loginUser)`。返回的令牌通过专门的响应 record 交给客户端。Redis 按用户维护一个有效会话，重新登录替换旧令牌。

在 Controller、Service 或 Facade 注入 `PrincipalFactory`。在当前请求的 Reactor 链路内取身份。未登录时返回空 Mono，受保护的业务将空值转成自己的未认证错误。

~~~java
return principals.currentUserId()
        .switchIfEmpty(Mono.error(new IllegalStateException("登录后才能继续")))
        .flatMap(orderRepository::findByCustomerId);
~~~

| 方法 | 用途 |
|---|---|
| `currentUserId()` | 从当前会话令牌取用户 ID，直接读取 Reactor Context |
| `currentUserObjectId()` | 取得 BSON ObjectId，直接传给 MongoDB Repository |
| `currentUser(LoginUser.class)` | 取完整用户快照，按指定类型解码 |
| `currentToken()` | 获取当前会话令牌，供会话维护使用 |
| `updatePrincipal(user)` | 更新当前令牌对应的快照 |
| `revisePrincipal(user)` | 从受保护的管理入口更新指定用户当前会话的快照 |
| `deletePrincipal()` | 注销当前令牌 |

身份读取无需重新访问 Redis。只需要 ID 时使用 `currentUserId()`。MongoDB Repository 用 `ObjectId` 时，直接使用 `currentUserObjectId()`。断开的新订阅、独立定时任务和消息消费者需要自己的身份来源。业务仍负责校验租户、资源归属和操作权限。

## 透传与部署

客户端请求到网关后，网关校验 Redis 会话。框架把身份放入独立的 RSocket 元数据，下游放回 Reactor Context。网关到服务用 JSON，Java RPC 的身份元数据与业务数据都用 Fory。双向流只从首帧恢复该流的身份。

内部 TCP 使用受信任网络模型。把业务 RSocket 端口绑定到回环或专用内网地址，防火墙和网络策略只放行网关与可信服务。获准直接访问端口的进程有权提供身份元数据。公开 HTTP 与 WSS 由边缘入口统一开放。
