# 🚀  lamb-framework

[![License: apache2.0](https://img.shields.io/github/license/tensorflow/tensorflow.svg)](https://www.apache.org/licenses/LICENSE-2.0)
## 📖  简介

Lambda-framework 致力于简化开发，让开发者专注于业务创新。我们重构了数据库和 TCP 底层连接，并原生支持 WebFlux 和 RSocket 两种启动协议，全面提升系统性能。

框架的 Gateway 已适配 RSocket，客户端无需修改协议即可享受其强大的背压和高吞吐能力。我们还实现了自定义的 RPC 远程调用，并通过重写底层 Socket 层，实现了用户信息的无缝传递，减少了对缓存（如 Redis）的依赖，大幅提升了微服务间的调用速度。

在未来版本我将使用 Kryo 作为rpc的序列化协议 
| 组件名称               | 说明         |
| ----------        	| ----------- |
| lambda-framework-common      	| 公共方法模块       |
| lambda-framework-compliance  	| 规范化组件        |
| lambda-framework-gateway  	| 网关组件        |
| lambda-framework-guid   		| 唯一序列号GUID生成组件        |
| lambda-framework-httpclient  	| http组件        |
| lambda-framework-loadbalance  	| 负载均衡组件        |
| lambda-framework-lock  	| 分布式锁组件        |
| lambda-framework-mq  	| mq组件        |
| lambda-framework-nacos  	| 注册中心组件        |
| lambda-framework-repository  	| 存储层组件        |
| lambda-framework-rpc  	| rpc组件        |
| lambda-framework-security  	| 权限组件        |
| lambda-framework-web   		| reactor web核心 基于reactive webflux        |
| lambda-framework-rsocket   		| reactor rsocket核心 基于reactive rsocket        |

***微服务建议:***
***每个微服务得有独特的异常命名，命名规范为 微服务名+ExceptionEnums***
***异常枚举必须实现ExceptionEnumFunction接口***
***ES_XXXXX_XXX ES开头代表这是框架底层抛出的异常***

新建一个工程的pom准备
```
    <parent>
        <groupId>org.lambda.framework</groupId>
        <artifactId>lambda-framework</artifactId>
        <version>1.0.0</version>
        <!--<relativePath/>--> <!-- lookup parent from repository -->
    </parent>
```

使用你想用的组件，不需要填写版本号，版本号与parent version同步 例如:
```
<dependency>
	<groupId>org.lambda.framework</groupId>
	<artifactId>lambda-framework-security</artifactId>
</dependency>
```
创建异常枚举类->例:
```
public enum SecurityExceptionEnum implements ExceptionEnumFunction {

    //系统异常-spring security 异常 - 200-299
    ES_SECURITY_000("ES_SECURITY_000","身份认证失败"), //AuthenticationException
    ES_SECURITY_001("ES_SECURITY_001","拒绝访问"), //AccessDeniedException
    
    @Getter
    @Setter
    private String code;
    
    @Getter
    @Setter
    private String message;
    // 构造方法
    private WebExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;

    }
    
}	
```

如果在reactor流中使用，可以使用
```
return Mono.error(new EventException(SecurityExceptionEnum.ES_SECURITY_004));
```
当然那也可以直接
```
throw new EventException(SecurityExceptionEnum.ES_SECURITY_004);
```
## lambda-framework-compliance
合规组件，有2个核心的设计理念，统一规范 和 敏捷开发
我们从mvc去理解，
先看controller层

1.DefaultBasicController
2.DefaultTreeController
```
public  class DefaultBasicController<PO extends UnifyPO,ID,Service extends IDefaultBasicService<PO,ID>> {

public class DefaultTreeController<PO extends UnifyPO & IFlattenTreePO,ID,Service extends IDefaultTreeService<PO,ID>> extends DefaultBasicController<PO,ID,Service>{
```
其中 Basic是基础的 包含了增删改查的基础接口，类似于自动生产代码的controller，直接继承就能实现。
而Tree是树的构建，比如这张表的结构是 id parentId orgId 这是一颗树的类型。 继承了这个controller后就能实现对树的增删改查的操作接口

service层
1.DefaultBasicServiceImpl
2.DefaultTreeServiceImpl
```
public class DefaultBasicServiceImpl<PO extends UnifyPO,ID,Repository extends ReactiveMySqlCrudRepositoryOperation<PO,ID>>  implements IDefaultBasicService<PO,ID> {

public class DefaultTreeServiceImpl<PO extends UnifyPO & IFlattenTreePO,ID,Repository extends ReactiveMySqlCrudRepositoryOperation<PO,ID>>  extends DefaultBasicServiceImpl<PO,ID,Repository> implements IDefaultTreeService<PO,ID> {
```
其中UnifyPO 是每个数据库表的都有的字段
```
	private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long creatorId;

    private Long updaterId;

    private String creatorName;

    private String updaterName;
```
IFlattenTreePO 是这棵树的扁平化展示 他是一个接口，代表了这颗树的核心结构
```
    public Long getId();

    public void setId(Long id);

    public Long getParentId();

    public void setParentId(Long parentId);

    public <PO extends IFlattenTreePO>List<PO> getChildrens();

    public <PO extends IFlattenTreePO>void setChildrens(List<PO> childrens);
```

登录用户模型，SecurityLoginUser和AbstractLoginUser,每个系统的用户类应当实现此接口
```
public interface SecurityLoginUser {
    public Long getId();
}
//在利用rpc调用的时候,此用户信息会自动传播到链路中
//可以随时随地的去获取用户的信息
//只要注入 SecurityPrincipalHolder 类
    @Resource
    private SecurityPrincipalHolder securityPrincipalHolder;

    public Mono<String> test() {
        return securityPrincipalHolder.fetchPrincipal2Object(EmployeePO.class)
                .flatMap(emp->{
                    return Mono.just("fxq: " + emp.getName());
                });
    }

```

在pom中注入以下的组件获得数据库 redis 和mq 的能力
```		
<dependency>
	<groupId>org.lambda.framework</groupId>
	<artifactId>lambda-framework-redis</artifactId>
</dependency>

<dependency>
	<groupId>org.lambda.framework</groupId>
	<artifactId>lambda-framework-repository</artifactId>
</dependency>

<dependency>
	<groupId>org.lambda.framework</groupId>
	<artifactId>lambda-framework-mq</artifactId>
</dependency>
```

```
//通过继承超类可以直接使用redis
@Configuration
public class RedisConfig extends DefaultReactiveRedisRepositoryConfig

//通过继承超类可以直接使用zk
@Configuration
public class RedisConfig extends DefaultZookeeperConfig


//通过继承超类可以直接使用kafkamq
@Configuration
public class RedisConfig extends DefaultReactiveKafkaMQConfig


//通过继承超类可以直接使用rabbitmq
@Configuration
public class RedisConfig extends DefaultReactiveRabbitMQConfig

//通过继承超类可以直接使用mongodb
@Configuration
public class RedisConfig extends DefaultReactiveMongoRepositoryConfig

//通过继承超类可以直接使用mysql
@Configuration
public class RedisConfig extends DefaultReactiveMysqlRepositoryConfig

```

## lambda-framework-rpc
我通过ASM实现动态代理,用loadbalance来做负载均衡实现的,利用rscoket netty和hashmap作为服务调用列表
```
//对于服务暴露端,需要将rpc接口暴露
@RSocketRpcDiscorvery("ace-microservices-fxq")
public interface IFxqTestApi {
    @RSocketRpcType
    public Mono<String> test();
}

//实现这个接口
@RSocketRpcApi
public class FxqTestApi implements IFxqTestApi {
    @Resource
    private FxqTestFunction fxqTestFunction;

    @Override
    public Mono<String> test() {
        return fxqTestFunction.test();
    }
}

//对于服务调用端,直接注入即可使用
    @RSocketRpc
    private IFxqTestApi iFxqTestApi;
```


## lambda-framework-security
在pom文件中引用下面代码块
```
<dependency>
  <groupId>org.lambda.framework</groupId>
  <artifactId>lambda-framework-security</artifactId>
</dependency>
```
###统一认证
在请求的headers中添加Auth-Token：xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
可以使用如下的例子来设置token
```
@Resource
private SecurityPrincipalUtil securityPprincipalUtil;

@GetMapping("/login")
public Mono login(){  //1
    User user =  User.builder().age(30).name("王刚").school("兰州理工大学").build();
    String userJson = JsonUtil.objToString(user);
    String authToken = principalUtil.setPrincipal(userJson);
    return returning(authToken);
}
```

并配置auth和autz的redis-database信息
```
lambda.security.redis.host=47.98.122.4
lambda.security.redis.password=XXXXXXXXXXXX
#default
lambda.security.redis.port=6379
lambda.security.redis.lettuce.pool.max_active=8
lambda.security.redis.lettuce.pool.max_wait_seconds=50
lambda.security.redis.lettuce.pool.max_idle=8
lambda.security.redis.lettuce.pool.min_idle=0
```
lambda-security已经写好了统一身份认证,无论是多服务和单机应用程序都都可以使用auth-token的形式登陆
在引用lambda-security后你需要在properties配置文件中配置你的auth认证的redis数据库和autz授权redis数据库
lambda-security默认使用redis来存储用户的auth-token和request path权限信息。
如果需要更改auth-token和request path权限信息的存储位置,可以重新配置bean
例如：
```
    CustomAuthManager extends SecurityAuthManager
    
    CustomAutzManager extends SecurityAutzManager
    
```
重写父类的方法覆盖掉逻辑并注入

```
    
    @Bean
    public SecurityAutzManager customAutzManager(CustomAuthManager customAuthManager){
        return new CustomAutzManager(customAuthManager) {
            @Override
            public boolean verify(String currentPathAutzTree, String principal) {
	    	//your code 
                return true;
            }
        };
    }

    @Bean
    public SecurityAuthManager customAuthManager(){
        return new CustomAuthManager(){
            @Override
            public boolean verify(String principal) {
	    	//your code 
                return true;
            }
        };
    }
```
如果你不希望更改身份认证校验逻辑，但是需要添加一些个性化的账号校验逻辑，你可以只重写方法来实现.
```
SecurityAuthManager.verify
```

授权校验的逻辑需要自己去写逻辑,框架提供了verify接口
```
SecurityAutzManager.verify(String currentPathAutzTree,String principal)
```
currentPathAutzTree代表当前路径的权限树
principal代表当前用户信息
你可以在verify接口中去校验currentPathAutzTree中的角色和权限是否存在于principal


lambda.security.url-autz-model=MAPPING or ALL
当你没有配置路径树 ，当检查路径树为空的时候 
当值为ALL的时候，会拒绝访问 ，方便于生产环境，所有的路径都需要在路径树中

当你没有配置路径树，当检查路径树为空的时候,当值为mapping的时候，会放过请求，方便开发环境

当路径树不为空的时候，都会经过SecurityAutzManager.verify方法进行校验(此时，url-autz-model将会失效)

## lambda-framework-web , lambda-framework-rsocket

```
//类似spring boot的controller写法,非常简单,使用@MessageMapping注解
@Controller
public class CustController {
    @Resource
    private CustFunction custFunction;

    @MessageMapping(CUST_PUT)
    public Mono<Void> put(ICustLoanApi.PutDTO dto){
        return custFunction.put(dto);
    }
}
```



