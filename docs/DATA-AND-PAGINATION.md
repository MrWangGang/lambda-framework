# 数据访问、CRUD 与分页手册

[返回总览](../README.md) · [数据库配置](MODULES-AND-CONFIGURATION.md) · [MongoDB 建模实例](https://github.com/MrWangGang/lambda-framework-example/blob/main/docs/DATA-MODELING.md)

## 1. 数据访问的三层

| 层 | 入口 | 适用范围 |
|---|---|---|
| 公共模型 | `UnifyPO<ID>`、`Paging`、`Paged<T>` | 实体字段、分页请求和结果约定 |
| Repository | MongoDB Repository、`ReactiveRedisOperation`、`ReactiveQdrantRepository<T>` | 数据库读写 |
| Service | `DefaultBasicServiceImpl`、`DefaultTreeServiceImpl` | 通用业务操作组合 |

需要复杂原子条件更新、多集合事务或精确控制查询计划时，可直接使用 `ReactiveMongoTemplate`；示例的商品和订单采用这一路径。不要为了套公共 CRUD 把库存更新拆成“先读、在 Java 计算、再无条件保存”。

## 2. 公共实体

所有 MongoDB 集合实体继承 `UnifyPO<ObjectId>`，统一持久化 `_id`、`createdAt`、`updatedAt` 和 `version`。Spring Data 审计负责实体写入，原子更新使用 `MongoAuditUpdates`。业务需要的租户、软删除等字段由实体单独定义。 [统一字段规范](../skills/lambda-java-backend/SKILL.md#实体注解与主键)

### 旧集合迁移

先在目标项目中确定要迁移的集合和历史时间来源，并暂停旧版本写入。下面以 MongoDB 的 `products` 集合为例。脚本保留已有值，只匹配至少一个公共字段缺失或为 null 的记录，可重跑。示例最后用 ObjectId 的生成时间补齐缺失的创建时间，已有业务审计时间时优先用业务数据。

~~~javascript
const needsAudit = {
  $or: [{createdAt: null}, {updatedAt: null}, {version: null}]
};

db.products.updateMany(needsAudit, [
  {$set: {createdAt: {$ifNull: ["$createdAt", "$createTime", {$toDate: "$_id"}]}}},
  {$set: {
    updatedAt: {$ifNull: ["$updatedAt", "$updateTime", "$createdAt"]},
    version: {$ifNull: ["$version", NumberLong("0")]}
  }}
]);

db.products.countDocuments(needsAudit);
~~~

对其他已确认的集合分别执行，计数为零后启动新版本。旧字段的移除按项目的数据保留策略单独安排。

## 3. 通用 CRUD

`IDefaultBasicService` 包含：

| 方法组 | 行为入口 |
|---|---|
| `insert / update / sync` | 单个和 Publisher 批量保存 |
| `get / find / fuzzy / count` | ID、对象条件、模糊与数量查询 |
| `delete / deleteBy` | 按 ID 或条件删除 |
| `deleteAll` | 删除全部数据，业务接口不应直接暴露 |
| `find(Paging, ...)` | 使用分页适配接口查询 |

服务需要返回匹配的 Repository，其约束包括 ReactiveCrudRepository、ReactiveSortingRepository、ReactiveQueryByExampleExecutor 和框架分页接口。

分页查询由回调实现。Mongo 示例使用 ReactiveMongoTemplate。

批量 Publisher 也需要限制输入规模和并发；`collectList()` 会把结果收集到内存，不能对无限流直接调用。

## 4. Paging 和 Paged 的字段约定

请求：

~~~json
{"page":1,"size":20}
~~~

设计约定是 page 从 1 开始，size 大于 0。框架目前没有统一的 size 上限，HTTP 边界应另设 1..100 等业务限制。

结果设计：

~~~json
{"page":1,"size":20,"total":45,"pages":3,"records":[]}
~~~

示例 POST /api/product/page 使用此响应结构，POST /api/product/cursor 使用独立的复合游标结构。

~~~java
Paging paging = Paging.builder().page(1).size(20).build();
Paging defaults = Paging.builder().build();
~~~

Builder 默认 page=1、size=10。内部 PageRequest 使用从 0 开始的页码。

## 5. 五类分页适配入口

| 入口 | 回调 | 主要结果 |
|---|---|---|
| `pageFrom` | `MappedPageQuery` | 回调返回 Spring Data Page |
| 条件 `queryPage` | `CountedPageQuery` | count(condition) + query(condition, pageable) |
| 无条件 `queryPage` | `CountedPageSource` | count() + query(pageable) |
| 条件 `dslPaging` | `UnifyPagingDslOperation` | SearchPage、结果转换、排序回调 |
| 无条件 `dslPaging` | `UnifyPagingDslDefaultOperation` | 同上，省略 condition |

业务实现 Mongo 查询和 count 时，遵守以下规则。

1. 两个查询使用完全相同的过滤条件。
2. count 不携带 skip/limit。
3. 排序稳定且有索引。
4. 返回空结果时 total、pages、records 的约定一致。
5. 多次查询之间并发写入可能使 count 与 records 不处于同一快照。

框架在事务内顺序执行 count 和 records。在事务外可并行等待两个查询。

## 6. 页码查询示例

~~~bash
curl -fsS -X POST http://127.0.0.1:8081/api/product/page \
  -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' -d '{"category":"book","page":"1","size":"20"}'
~~~

## 7. 示例已经实现的游标分页

~~~bash
curl -fsS -X POST http://127.0.0.1:8081/api/product/cursor \
  -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' -d '{"category":"book","limit":"20"}'
~~~

空结果结构：

~~~json
{"items":[],"nextBeforeTime":null,"nextBeforeId":null}
~~~

非末页会返回两个 next 字段。下一页必须同时传入：

~~~bash
curl -fsS http://127.0.0.1:8081/api/product/cursor \
  -H "Authorization: Bearer ${TOKEN}" \
  --json '{"category":"book","limit":"20","beforeTime":"上次的 nextBeforeTime","beforeId":"上次的 nextBeforeId"}'
~~~

实际排序为 `createdAt DESC, _id DESC`，索引为 `category ASC, createdAt DESC, _id DESC`。查询取 limit+1 条判断是否有下一页；只返回前 limit 条。

下一页条件是“时间更早，或时间相等而 ID 更小”。ID 用于稳定排序，不用来推断创建时间。分页期间插入或修改数据仍可能改变结果集合，游标不提供跨请求一致快照。

## 8. 页码分页和游标分页如何选

| 场景 | 建议 |
|---|---|
| 后台少量数据，需要跳到第 N 页 | 页码分页，限制深度并核验 count 成本 |
| 信息流、收藏、评论、订单时间线 | 游标分页 |
| 大规模导出 | 范围批次 / 流式处理，避免整表 collectList |
| 搜索引擎深翻页 | 按引擎能力设计 search_after 等方案，单独验收 |
| 必须看到严格一致的历史快照 | 专门的快照 / 导出任务 |

大偏移 skip 随偏移增长需要扫描更多结果。范围条件配合稳定索引更适合连续翻页。[MongoDB skip 与范围查询说明](https://www.mongodb.com/docs/manual/reference/method/cursor.skip/)

## 9. 查询与更新规范

- 索引先覆盖等值过滤，再考虑排序和范围条件；用实际 explain 验证。
- 高频接口限制返回字段，避免每次返回大文档。
- 关系数据增长不设上限时拆集合，避免不断追加收藏 ID。
- 库存使用 stock >= quantity 的条件更新，再原子递减。
- 更新带 version 条件；冲突交给业务重试 / 提示，不做静默覆盖。
- count 和跨集合聚合需要独立观察成本。
- 缓存和 Qdrant 是派生数据；MongoDB 是示例业务事实来源。
- 事务范围内不进行远程副作用；可靠通知采用待实现的 Outbox 流程。

完整关系设计见[示例数据建模手册](https://github.com/MrWangGang/lambda-framework-example/blob/main/docs/DATA-MODELING.md)。
