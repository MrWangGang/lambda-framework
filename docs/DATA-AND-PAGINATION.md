# 数据访问、CRUD 与分页手册

[返回总览](../README.md) · [数据库配置](MODULES-AND-CONFIGURATION.md) · [MongoDB 建模实例](https://github.com/MrWangGang/lambda-framework-example/blob/main/docs/DATA-MODELING.md)

## 1. 数据访问的三层

| 层 | 入口 | 适用范围 |
|---|---|---|
| 公共模型 | `UnifyPO<ID>`、`Paging`、`Paged<T>` | 实体字段、分页请求和结果约定 |
| Repository | Mongo / MySQL / ES CRUD 接口、`ReactiveRedisOperation` | 数据库读写 |
| Service | `DefaultBasicServiceImpl`、`DefaultTreeServiceImpl` | 通用业务操作组合 |

需要复杂原子条件更新、多集合事务或精确控制查询计划时，可直接使用 `ReactiveMongoTemplate`；示例的商品和订单采用这一路径。不要为了套公共 CRUD 把库存更新拆成“先读、在 Java 计算、再无条件保存”。

## 2. 公共实体

`UnifyPO<ID>` 提供 id、createTime、updateTime，并使用 Lombok `@SuperBuilder`。它没有内置账号密码、租户、乐观锁版本或软删除字段。

业务需要这些字段时显式定义，明确 ID 格式和唯一性范围、时间字段类型和时区、version 的维护方式、删除后的关系清理方式。

示例独立使用 `Product` record，字段为 id/name/category/price/stock/version/createdAt，金额为 BigDecimal + MongoDB Decimal128。

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

名称中的 `jpqlPaging` 是历史抽象命名，具体查询由回调实现；不要仅凭方法名判断一定使用阻塞 JPA。MySQL 配置走 R2DBC；Mongo 示例走 ReactiveMongoTemplate。

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

上面是结构示意，不是当前 `/api/products` 的响应。示例商品列表返回游标分页结构。

显式赋值能避开当前 Builder 默认值缺口：

~~~java
Paging paging = new Paging();
paging.setPage(1);
paging.setSize(20);
Paging another = Paging.builder().page(1).size(20).build();
~~~

`new Paging()` 有字段初始化默认值；当前 `Paging.builder().build()` 没有 `@Builder.Default` 支持，不能认为结果一定是 page=1、size=10。

## 5. 五类分页适配入口

| 入口 | 回调 | 主要结果 |
|---|---|---|
| `jpqlPaging` | `UnifyPagingJpqlOperation` | 回调返回 Spring Data Page |
| 条件 `sqlPaging` | `UnifyPagingSqlOperation` | count(condition) + query(condition, pageable) |
| 无条件 `sqlPaging` | `UnifyPagingSqlDefaultOperation` | count() + query(pageable) |
| 条件 `dslPaging` | `UnifyPagingDslOperation` | SearchPage、结果转换、排序回调 |
| 无条件 `dslPaging` | `UnifyPagingDslDefaultOperation` | 同上，省略 condition |

SQL 风格适配接口不限定只能接 MySQL。业务可以实现 Mongo 查询和 count，但需要保证：

1. 两个查询使用完全相同的过滤条件。
2. count 不携带 skip/limit。
3. 排序稳定且有索引。
4. 返回空结果时 total、pages、records 的约定一致。
5. 多次查询之间并发写入可能使 count 与 records 不处于同一快照。

`Mono.zip` 组合 count 和 records，不代表建立了快照事务，也不代表一定创建两条线程。是否并行等待由数据源和调度决定。

## 6. 当前通用分页缺陷：已复现，未修复

| 编号 | 问题 | 影响 |
|---|---|---|
| PAGE-01 | 五个实现调用 `pageRequest.withSort(sort)` 后没有接收返回值 | 传给回调的 Pageable 仍为 UNSORTED |
| PAGE-02 | JPQL 风格结果使用 `page.getNumber()` | 返回零起始页码，与请求及其他分支不一致 |
| PAGE-03 | Paging / Paged 字段缺少 Builder 默认值声明 | 无参 Builder 的 page/size 可能是 null |

外部临时回归结果：**12 项，8 失败，0 错误，0 跳过**。失败分布：5 个排序传递断言、2 个 JPQL 页码断言、1 个 Builder 默认值断言。其余 4 项覆盖部分空结果、页数计算、无排序和非法参数行为。

测试直接调用分页适配层，不依赖真实 ES 服务；不能解释为 ES 或 MySQL 实机分页验收。

这些问题尚未修改源码，也未新增示例的页码分页接口。历史“22 项业务验收通过”中的分页项只覆盖商品复合游标，不覆盖这套通用分页。

## 7. 修复方案与后续验收

以下是待实现方案：

- 接收不可变 PageRequest 的 withSort 返回值。
- JPQL 返回页码统一为 1 起始。
- 明确 Paging / Paged 的 Builder 默认值。
- 在示例新增独立的页码分页入口，保留已有游标接口。
- 业务层限制排序字段白名单、方向、页大小，并添加匹配索引。
- 测试首页、中间页、末页、超范围、空集合、同值排序、升降序、过滤条件、非法参数和 count 一致性。

目前不存在 `GET /api/products/page`。在代码实现并验收之前，调用它不会获得文档假想的 Paged 结果。

## 8. 示例已经实现的游标分页

~~~bash
curl -G 'http://127.0.0.1:8081/api/products' \
  --data-urlencode 'category=book' \
  --data-urlencode 'limit=20'
~~~

空结果结构：

~~~json
{"items":[],"nextBeforeTime":null,"nextBeforeId":null}
~~~

非末页会返回两个 next 字段。下一页必须同时传入：

~~~bash
curl -G 'http://127.0.0.1:8081/api/products' \
  --data-urlencode 'category=book' \
  --data-urlencode 'limit=20' \
  --data-urlencode 'beforeTime=上次返回的nextBeforeTime' \
  --data-urlencode 'beforeId=上次返回的nextBeforeId'
~~~

实际排序为 `createdAt DESC, _id DESC`，索引为 `category ASC, createdAt DESC, _id DESC`。查询取 limit+1 条判断是否有下一页；只返回前 limit 条。

下一页条件是“时间更早，或时间相等而 ID 更小”。ID 用于稳定排序，不用来推断创建时间。分页期间插入或修改数据仍可能改变结果集合，游标不提供跨请求一致快照。

## 9. 页码分页和游标分页如何选

| 场景 | 建议 |
|---|---|
| 后台少量数据，需要跳到第 N 页 | 页码分页，限制深度并核验 count 成本 |
| 信息流、收藏、评论、订单时间线 | 游标分页 |
| 大规模导出 | 范围批次 / 流式处理，避免整表 collectList |
| 搜索引擎深翻页 | 按引擎能力设计 search_after 等方案，单独验收 |
| 必须看到严格一致的历史快照 | 专门的快照 / 导出任务 |

大偏移 skip 随偏移增长需要扫描更多结果。范围条件配合稳定索引更适合连续翻页。[MongoDB skip 与范围查询说明](https://www.mongodb.com/docs/manual/reference/method/cursor.skip/)

## 10. 查询与更新规范

- 索引先覆盖等值过滤，再考虑排序和范围条件；用实际 explain 验证。
- 高频接口限制返回字段，避免每次返回大文档。
- 关系数据增长不设上限时拆集合，避免不断追加收藏 ID。
- 库存使用 stock >= quantity 的条件更新，再原子递减。
- 更新带 version 条件；冲突交给业务重试 / 提示，不做静默覆盖。
- count 和跨集合聚合需要独立观察成本。
- 缓存和 Qdrant 是派生数据；MongoDB 是示例业务事实来源。
- 事务范围内不进行远程副作用；可靠通知采用待实现的 Outbox 流程。

完整关系设计见[示例数据建模手册](https://github.com/MrWangGang/lambda-framework-example/blob/main/docs/DATA-MODELING.md)。
